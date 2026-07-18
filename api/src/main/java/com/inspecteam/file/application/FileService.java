package com.inspecteam.file.application;

import com.inspecteam.file.infrastructure.FileJdbcRepository;
import com.inspecteam.file.infrastructure.StorageProperties;
import com.inspecteam.permission.application.TenantAuthorizationService;
import com.inspecteam.shared.exception.ApiException;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository.Membership;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private final FileJdbcRepository files;
    private final MinioClient storage;
    private final StorageProperties properties;
    private final TenantAuthorizationService authorization;

    public FileService(FileJdbcRepository files, MinioClient storage, StorageProperties properties,
            TenantAuthorizationService authorization) {
        this.files = files;
        this.storage = storage;
        this.properties = properties;
        this.authorization = authorization;
    }

    @Transactional
    public UploadSession createUpload(UUID tenantId, UUID userId, boolean admin, UUID formId,
            UUID submissionId, String fieldId, String originalName, String contentType,
            long size, String checksum) {
        Membership membership = authorization.requireForm(tenantId, userId, admin, formId, "FORM_USE");
        if (membership == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Administrador global precisa de membership para anexar arquivos");
        }
        if (size <= 0 || size > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Arquivo deve possuir no máximo 20 MB");
        }
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = tenantId + "/submissions/" + submissionId + "/" + UUID.randomUUID() + "/" + safeName;
        try {
            ensureBucket();
            String uploadUrl = storage.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT).bucket(properties.bucket()).object(objectKey)
                    .expiry(1, TimeUnit.HOURS).build());
            UUID fileId = files.create(tenantId, submissionId, fieldId, objectKey, originalName,
                    contentType, size, checksum, membership.id());
            return new UploadSession(fileId, objectKey, uploadUrl, 3600);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Storage de arquivos indisponível");
        }
    }

    @Transactional
    public void complete(UUID tenantId, UUID userId, boolean admin, UUID formId, UUID fileId) {
        authorization.requireForm(tenantId, userId, admin, formId, "FORM_USE");
        var file = files.find(tenantId, fileId);
        try {
            var stat = storage.statObject(StatObjectArgs.builder()
                    .bucket(properties.bucket()).object(file.objectKey()).build());
            if (stat.size() != file.expectedSize()) {
                throw new ApiException(HttpStatus.CONFLICT, "Tamanho enviado difere do tamanho declarado");
            }
            files.complete(tenantId, fileId);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Upload ainda não está disponível no storage");
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = storage.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build());
        if (!exists) {
            storage.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
        }
    }

    public record UploadSession(UUID fileId, String objectKey, String uploadUrl, int expiresInSeconds) { }
}
