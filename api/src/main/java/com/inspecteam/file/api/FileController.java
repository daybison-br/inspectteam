package com.inspecteam.file.api;

import com.inspecteam.file.application.FileService;
import com.inspecteam.file.application.FileService.UploadSession;
import com.inspecteam.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/files")
public class FileController {

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    @PostMapping("/upload-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    UploadSession create(@PathVariable UUID tenantId, @Valid @RequestBody UploadRequest request,
            Authentication authentication) {
        return service.createUpload(tenantId, CurrentUser.id(authentication),
                CurrentUser.isPlatformAdmin(authentication), request.formId(), request.submissionId(),
                request.fieldId(), request.originalName(), request.contentType(), request.sizeBytes(), request.checksumSha256());
    }

    @PostMapping("/{fileId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void complete(@PathVariable UUID tenantId, @PathVariable UUID fileId,
            @Valid @RequestBody CompleteRequest request, Authentication authentication) {
        service.complete(tenantId, CurrentUser.id(authentication), CurrentUser.isPlatformAdmin(authentication),
                request.formId(), fileId);
    }

    record UploadRequest(@NotNull UUID formId, @NotNull UUID submissionId,
            @NotBlank @Size(max = 120) String fieldId,
            @NotBlank @Size(max = 255) String originalName,
            @NotBlank @Size(max = 160) String contentType,
            @Positive long sizeBytes,
            @Pattern(regexp = "[a-fA-F0-9]{64}") String checksumSha256) { }

    record CompleteRequest(@NotNull UUID formId) { }
}
