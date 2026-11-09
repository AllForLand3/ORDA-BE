package com.allforland.orda.domain.analysis.dem.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** DB에 등록된 DEM 파일 경로를 해석하고 허용 디렉터리와 읽기 권한을 확인한다. 파일 내용과 GeoTIFF 형식 검증은 판독 단계에서 수행한다. */
@Component
public class DemFileResolver {

    private final String baseDirectory;

    public DemFileResolver(@Value("${orda.dem.base-directory:}") String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    /**
     * 상대 경로는 기준 디렉터리 아래에서 해석한다. 절대 경로도 허용 디렉터리 안에 있는 경우에만 허용한다.
     *
     * @param registeredPath DB의 dem_asset.file_path
     * @return 심볼릭 링크까지 해석한 실제 파일 경로
     */
    public Path resolve(String registeredPath) {
        Path configuredBase = configuredBaseDirectory();
        Path realBase = realBaseDirectory(configuredBase);
        Path registered = registeredPath(registeredPath);

        Path candidate =
                registered.isAbsolute()
                        ? registered.normalize()
                        : configuredBase.resolve(registered).normalize();

        // "../" 등으로 설정된 기준 디렉터리 밖을 가리키는 경로를 차단한다.
        if (!candidate.startsWith(configuredBase)) {
            throw new BusinessException(ErrorCode.DEM_FILE_OUTSIDE_DIRECTORY);
        }

        try {
            Path realFile = candidate.toRealPath();

            // 디렉터리 안의 링크가 외부 파일을 가리키는 경우도 차단한다.
            if (!realFile.startsWith(realBase)) {
                throw new BusinessException(ErrorCode.DEM_FILE_OUTSIDE_DIRECTORY);
            }

            if (!Files.isRegularFile(realFile)) {
                throw new BusinessException(ErrorCode.DEM_FILE_NOT_REGULAR);
            }

            if (!Files.isReadable(realFile)) {
                throw new BusinessException(ErrorCode.DEM_FILE_NOT_READABLE);
            }

            return realFile;
        } catch (NoSuchFileException exception) {
            throw new BusinessException(ErrorCode.DEM_FILE_NOT_FOUND);
        } catch (AccessDeniedException | SecurityException exception) {
            throw new BusinessException(ErrorCode.DEM_FILE_NOT_READABLE);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.DEM_FILE_ACCESS_FAILED);
        }
    }

    private Path configuredBaseDirectory() {
        if (baseDirectory == null || baseDirectory.isBlank()) {
            throw new BusinessException(ErrorCode.DEM_DIRECTORY_UNAVAILABLE);
        }

        try {
            Path base = Path.of(baseDirectory);

            // 실행 디렉터리에 의존하지 않도록 절대 경로를 필수로 한다.
            if (!base.isAbsolute()) {
                throw new BusinessException(ErrorCode.DEM_DIRECTORY_UNAVAILABLE);
            }

            return base.normalize();
        } catch (InvalidPathException | SecurityException exception) {
            throw new BusinessException(ErrorCode.DEM_DIRECTORY_UNAVAILABLE);
        }
    }

    private Path realBaseDirectory(Path configuredBase) {
        try {
            Path realBase = configuredBase.toRealPath();

            if (!Files.isDirectory(realBase) || !Files.isReadable(realBase)) {
                throw new BusinessException(ErrorCode.DEM_DIRECTORY_UNAVAILABLE);
            }

            return realBase;
        } catch (IOException | SecurityException exception) {
            throw new BusinessException(ErrorCode.DEM_DIRECTORY_UNAVAILABLE);
        }
    }

    private Path registeredPath(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.DEM_FILE_PATH_INVALID);
        }

        // Linux가 Windows 경로를 상대 파일명으로 해석하지 않도록 차단한다.
        if (File.separatorChar == '/'
                && (value.indexOf('\\') >= 0 || value.matches("^[A-Za-z]:.*"))) {
            throw new BusinessException(ErrorCode.DEM_FILE_PATH_INVALID);
        }

        try {
            return Path.of(value);
        } catch (InvalidPathException | SecurityException exception) {
            throw new BusinessException(ErrorCode.DEM_FILE_PATH_INVALID);
        }
    }
}
