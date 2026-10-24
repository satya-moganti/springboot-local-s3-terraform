package com.aws.storage.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aws.storage.dto.FileResponse;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class FileStorageService {
    private final S3Client s3;
    private final String bucket;

    public FileStorageService(S3Client s3, @Value("${app.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public FileResponse upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a non-empty file.");
        }

        String key = safeKey(file.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType() == null
                        ? "application/octet-stream" : file.getContentType())
                .build();

        var result = s3.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return new FileResponse(key, file.getSize(), result.eTag());
    }

    public List<FileResponse> list() {
        return s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build())
                .contents().stream()
                .map(obj -> new FileResponse(obj.key(), obj.size(), obj.eTag()))
                .toList();
    }

    public DownloadedFile download(String key) {
        ResponseInputStream<GetObjectResponse> stream = s3.getObject(
                GetObjectRequest.builder().bucket(bucket).key(safeKey(key)).build());
        try (stream) {
            byte[] bytes = stream.readAllBytes();
            String contentType = stream.response().contentType();
            return new DownloadedFile(bytes, contentType == null
                    ? "application/octet-stream" : contentType);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read S3 object.", e);
        }
    }

    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(safeKey(key)).build());
    }

    private String safeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("File key/name must not be blank.");
        }
        String key = raw.replace("\\", "/");
        key = key.substring(key.lastIndexOf('/') + 1);
        if (key.isBlank() || key.equals(".") || key.equals("..")) {
            throw new IllegalArgumentException("Invalid file key.");
        }
        return key;
    }

    public record DownloadedFile(byte[] bytes, String contentType) {}
}
