package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.PresignedUrlResponse;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.EntityType;
import com.nexuslearn.api.models.User;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "txt",                // Documents
            "png", "jpg", "jpeg", "gif", "webp",        // Images
            "mp4", "webm",                              // Videos
            "zip", "rar"                                // Archives (for code submissions)
    );
    private final MinioClient minioClient;
    private final CourseSecurityValidator securityValidator;
    @Value("${minio.bucket-name:nexuslearn-files}")
    private String bucketName;

    @Value("${minio.url:http://localhost:9000}")
    private String minioBaseUrl;

    @PostConstruct
    public void initBucket() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MinIO bucket", e);
        }
    }

    public PresignedUrlResponse generatePreSignedUploadUrl(String originalFilename, UUID entityId, EntityType entityType, User user) {
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AppException("File type '." + extension + "' is not allowed for upload.", HttpStatus.BAD_REQUEST);
        }

        securityValidator.validateAttachmentUpload(entityId, entityType, user);

        try {
            String objectName = UUID.randomUUID() + "-" + originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");

            String presignedUrl = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.PUT).bucket(bucketName).object(objectName).expiry(15, TimeUnit.MINUTES).build());

            String finalFileUrl = minioBaseUrl + "/" + bucketName + "/" + objectName;

            return new PresignedUrlResponse(presignedUrl, finalFileUrl, objectName);

        } catch (Exception e) {
            throw new AppException("Failed to generate secure upload ticket", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String generatePreSignedDownloadUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET).bucket(bucketName).object(objectName).expiry(1, TimeUnit.HOURS).build());
        } catch (Exception e) {
            throw new AppException("Failed to generate secure download ticket", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String objectName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            throw new AppException("Failed to delete file from cloud storage", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}