package com.example.backend.global.infra.file;

import com.example.backend.global.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    @Value("${file.upload.dir}")
    private String uploadDir;
    // 파일을 저장하고 "저장된 파일명"을 리턴
    public String saveFile(MultipartFile file) {
        // 1. 빈 파일 체크
        if (file.isEmpty()) {
            throw new FileStorageException("빈 파일은 저장할 수 없습니다.");
        }

        // 2. 파일명 클렌징 (경로 조작 방지)
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        if (originalFilename.contains("..")) {
            throw new FileStorageException("유효하지 않은 파일 경로가 포함되어 있습니다: " + originalFilename);
        }

        try {
            // 3. 저장 경로 생성 (없으면 생성)
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath); // mkdirs() 보다 안전한 NIO 방식
            }

            // 4. 고유 파일명 생성 (UUID)
            String savedFileName = UUID.randomUUID() + "_" + originalFilename;

            // 5. 파일 저장 (Copy)
            // resolve: 경로 합치기 (/app/uploads + /uuid_file.wav)
            Path targetLocation = uploadPath.resolve(savedFileName);

            // 기존에 같은 이름 있으면 덮어쓰기 (REPLACE_EXISTING) 등 옵션 설정 가능
            Files.copy(file.getInputStream(), targetLocation);

            log.info("파일 저장 성공: {}", targetLocation.toString());
            return savedFileName;

        } catch (IOException e) {
            throw new FileStorageException("파일을 저장할 수 없습니다. 다시 시도해주세요.", e);
        }
    }
}
