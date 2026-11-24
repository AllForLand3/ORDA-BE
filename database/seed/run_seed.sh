#!/usr/bin/env bash

set -Eeuo pipefail

# 시드 SQL 파일은 UTF-8이므로 Windows 로캘과 무관하게 psql 입력 인코딩을 고정한다.
export PGCLIENTENCODING=UTF8

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

usage() {
    cat <<'EOF'
사용법:
  bash ./database/seed/run_seed.sh all
  bash ./database/seed/run_seed.sh <산 이름>
  bash ./database/seed/run_seed.sh <SQL 파일명>

예시:
  bash ./database/seed/run_seed.sh all
  bash ./database/seed/run_seed.sh gwanak
  bash ./database/seed/run_seed.sh inwang
  bash ./database/seed/run_seed.sh inwang_seed.sql

실행 모드:
  all       데이터가 전혀 없는 초기 DB에 관악산 시연 데이터와 모든 산 시드를 실행합니다.
  산 이름   해당 산의 폴더 시드 또는 <산 이름>_seed.sql만 실행합니다.
  파일명    database/seed 바로 아래의 지정한 SQL 파일만 실행합니다.

필요한 DB 연결 환경변수:
  방법 1: DATABASE_URL
  방법 2: PGHOST, PGPORT(기본값 5432), PGUSER, PGDATABASE

비밀번호는 DATABASE_URL 또는 PGPASSWORD, ~/.pgpass 등을 사용하세요.
자세한 내용은 database/seed/README.md를 확인하세요.
EOF
}

if [[ $# -ne 1 ]]; then
    usage >&2
    exit 2
fi

readonly SEED_TARGET="$1"

if [[ ! "$SEED_TARGET" =~ ^[A-Za-z0-9_-]+([.]sql)?$ ]]; then
    echo "오류: 산 이름 또는 database/seed 바로 아래의 SQL 파일명만 지정할 수 있습니다." >&2
    exit 2
fi

if ! command -v psql >/dev/null 2>&1; then
    echo "오류: psql 명령을 찾을 수 없습니다." >&2
    exit 1
fi

declare -a PSQL_CONNECTION_ARGS=()

if [[ -n "${DATABASE_URL:-}" ]]; then
    PSQL_CONNECTION_ARGS+=("$DATABASE_URL")
else
    : "${PGHOST:?오류: DATABASE_URL 또는 PGHOST를 설정해야 합니다.}"
    : "${PGUSER:?오류: DATABASE_URL 또는 PGUSER를 설정해야 합니다.}"
    : "${PGDATABASE:?오류: DATABASE_URL 또는 PGDATABASE를 설정해야 합니다.}"

    readonly PGPORT="${PGPORT:-5432}"
    PSQL_CONNECTION_ARGS+=(
        --host "$PGHOST"
        --port "$PGPORT"
        --username "$PGUSER"
        --dbname "$PGDATABASE"
    )
fi

declare -a SQL_FILES=()

append_seed_directory() {
    local seed_dir="$1"
    local -a directory_files=()

    mapfile -d '' directory_files < <(
        find "$seed_dir" \
            -maxdepth 1 \
            -type f \
            -name '[0-9][0-9]_*.sql' \
            -print0 \
            | sort -z
    )

    if [[ ${#directory_files[@]} -eq 0 ]]; then
        echo "오류: 실행할 NN_*.sql 파일이 없습니다: $seed_dir" >&2
        exit 1
    fi

    SQL_FILES+=("${directory_files[@]}")
}

readonly INITIAL_LOAD_MODE=$([[ "$SEED_TARGET" == "all" ]] && echo "yes" || echo "no")

if [[ "$INITIAL_LOAD_MODE" == "yes" ]]; then
    # 관악산 초기 시드는 명시적 PK를 사용하고 마지막에 sequence를 보정하므로 가장 먼저 실행한다.
    append_seed_directory "$SCRIPT_DIR/gwanak"

    declare -a root_seed_files=()
    mapfile -d '' root_seed_files < <(
        find "$SCRIPT_DIR" \
            -maxdepth 1 \
            -type f \
            -name '*_seed.sql' \
            -print0 \
            | sort -z
    )
    SQL_FILES+=("${root_seed_files[@]}")
elif [[ -d "$SCRIPT_DIR/$SEED_TARGET" ]]; then
    append_seed_directory "$SCRIPT_DIR/$SEED_TARGET"
elif [[ -f "$SCRIPT_DIR/$SEED_TARGET" && "$SEED_TARGET" == *.sql ]]; then
    SQL_FILES+=("$SCRIPT_DIR/$SEED_TARGET")
elif [[ -f "$SCRIPT_DIR/${SEED_TARGET}_seed.sql" ]]; then
    SQL_FILES+=("$SCRIPT_DIR/${SEED_TARGET}_seed.sql")
else
    echo "오류: 시드 폴더 또는 SQL 파일을 찾을 수 없습니다: $SEED_TARGET" >&2
    usage >&2
    exit 1
fi

if [[ ${#SQL_FILES[@]} -eq 0 ]]; then
    echo "오류: 실행할 시드 SQL 파일이 없습니다." >&2
    exit 1
fi

if [[ "$INITIAL_LOAD_MODE" == "yes" ]]; then
    echo "주의: all 모드는 관악산 시드에 포함된 개발·시연 계정과 코스도 함께 적재합니다."
    echo "      산 데이터만 추가하려면 산 이름 또는 SQL 파일명을 지정하세요."
    echo
fi

echo "시드 대상: $SEED_TARGET"
echo "실행 파일:"
for sql_file in "${SQL_FILES[@]}"; do
    echo "  - ${sql_file#"$SCRIPT_DIR"/}"
done

echo
echo "연결 대상 확인 중..."
psql \
    "${PSQL_CONNECTION_ARGS[@]}" \
    --no-psqlrc \
    --tuples-only \
    --no-align \
    --set ON_ERROR_STOP=1 \
    --command \
    "SELECT 'database=' || current_database()
         || ', user=' || current_user
         || ', server=' || COALESCE(inet_server_addr()::text, 'unix-domain socket')
         || CASE
                WHEN inet_server_addr() IS NULL THEN ''
                ELSE ':' || COALESCE(inet_server_port()::text, 'unknown')
            END;"

if [[ "$INITIAL_LOAD_MODE" == "yes" ]]; then
    echo
    echo "초기 DB 여부 확인 중..."
    psql \
        "${PSQL_CONNECTION_ARGS[@]}" \
        --no-psqlrc \
        --set ON_ERROR_STOP=1 \
        --command \
        "DO \$seed_empty_check\$
         DECLARE
             target_table record;
             table_has_rows boolean;
         BEGIN
             FOR target_table IN
                 SELECT tablename
                 FROM pg_catalog.pg_tables
                 WHERE schemaname = 'public'
                   AND tablename NOT IN ('flyway_schema_history', 'spatial_ref_sys')
                 ORDER BY tablename
             LOOP
                 EXECUTE format('SELECT EXISTS (SELECT 1 FROM public.%I)', target_table.tablename)
                 INTO table_has_rows;

                 IF table_has_rows THEN
                     RAISE EXCEPTION
                         '전체 초기 적재는 빈 DB에서만 실행할 수 있습니다. 데이터가 있는 테이블: public.%',
                         target_table.tablename;
                 END IF;
             END LOOP;
         END
         \$seed_empty_check\$;"
fi

if [[ "${CONFIRM_SEED_RUN:-}" != "yes" ]]; then
    if [[ ! -t 0 ]]; then
        echo "오류: 비대화형 실행에서는 CONFIRM_SEED_RUN=yes가 필요합니다." >&2
        exit 1
    fi

    echo
    read -r -p "위 DB에 '$SEED_TARGET' 시드를 실행할까요? [y/N] " confirmation
    if [[ ! "$confirmation" =~ ^[Yy]$ ]]; then
        echo "실행을 취소했습니다."
        exit 0
    fi
fi

for sql_file in "${SQL_FILES[@]}"; do
    echo
    echo "실행 중: ${sql_file#"$SCRIPT_DIR"/}"
    psql \
        "${PSQL_CONNECTION_ARGS[@]}" \
        --no-psqlrc \
        --set ON_ERROR_STOP=1 \
        --file "$sql_file"
done

echo
echo "'$SEED_TARGET' 시드 실행을 완료했습니다."
