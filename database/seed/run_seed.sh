#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

usage() {
    cat <<'EOF'
사용법:
  ./database/seed/run_seed.sh <시드 이름>

예시:
  ./database/seed/run_seed.sh gwanak

필요한 DB 연결 환경변수:
  방법 1: DATABASE_URL
  방법 2: PGHOST, PGPORT(기본값 5432), PGUSER, PGDATABASE

비밀번호는 DATABASE_URL 또는 PGPASSWORD, ~/.pgpass 등을 사용하세요.
EOF
}

if [[ $# -ne 1 ]]; then
    usage >&2
    exit 2
fi

readonly SEED_NAME="$1"

if [[ ! "$SEED_NAME" =~ ^[A-Za-z0-9_-]+$ ]]; then
    echo "오류: 시드 이름에는 영문자, 숫자, 밑줄, 하이픈만 사용할 수 있습니다." >&2
    exit 2
fi

readonly SEED_DIR="$SCRIPT_DIR/$SEED_NAME"

if [[ ! -d "$SEED_DIR" ]]; then
    echo "오류: 시드 디렉터리를 찾을 수 없습니다: $SEED_DIR" >&2
    exit 1
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

mapfile -d '' SQL_FILES < <(
    find "$SEED_DIR" \
        -maxdepth 1 \
        -type f \
        -name '[0-9][0-9]_*.sql' \
        -print0 \
        | sort -z
)

if [[ ${#SQL_FILES[@]} -eq 0 ]]; then
    echo "오류: 실행할 NN_*.sql 파일이 없습니다: $SEED_DIR" >&2
    exit 1
fi

echo "시드 이름: $SEED_NAME"
echo "시드 경로: $SEED_DIR"
echo "실행 파일:"
for sql_file in "${SQL_FILES[@]}"; do
    echo "  - $(basename -- "$sql_file")"
done

echo
echo "연결 대상 확인 중..."
psql \
    "${PSQL_CONNECTION_ARGS[@]}" \
    --no-psqlrc \
    --tuples-only \
    --no-align \
    --command \
    "SELECT 'database=' || current_database() || ', user=' || current_user || ', server=' || inet_server_addr() || ':' || inet_server_port();"

if [[ "${CONFIRM_SEED_RUN:-}" != "yes" ]]; then
    if [[ ! -t 0 ]]; then
        echo "오류: 비대화형 실행에서는 CONFIRM_SEED_RUN=yes가 필요합니다." >&2
        exit 1
    fi

    echo
    read -r -p "위 DB에 '$SEED_NAME' 시드를 실행할까요? [y/N] " confirmation
    if [[ ! "$confirmation" =~ ^[Yy]$ ]]; then
        echo "실행을 취소했습니다."
        exit 0
    fi
fi

for sql_file in "${SQL_FILES[@]}"; do
    echo
    echo "실행 중: $(basename -- "$sql_file")"
    psql \
        "${PSQL_CONNECTION_ARGS[@]}" \
        --no-psqlrc \
        --set ON_ERROR_STOP=1 \
        --file "$sql_file"
done

echo
echo "'$SEED_NAME' 시드 실행을 완료했습니다."
