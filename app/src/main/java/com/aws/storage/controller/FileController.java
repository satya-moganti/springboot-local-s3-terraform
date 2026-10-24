package com.aws.storage.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aws.storage.dto.FileResponse;
import com.aws.storage.service.FileStorageService;
import com.aws.storage.service.FileStorageService.DownloadedFile;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileStorageService storage;

    public FileController(FileStorageService storage) {
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileResponse upload(@RequestPart("file") MultipartFile file) throws IOException {
        return storage.upload(file);
    }

    @GetMapping
    public List<FileResponse> list() {
        return storage.list();
    }

    @GetMapping("/{key:.+}")
    public ResponseEntity<byte[]> download(@PathVariable String key) {
        DownloadedFile file = storage.download(key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(key).build().toString())
                .body(file.bytes());
    }

    @DeleteMapping("/{key:.+}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        storage.delete(key);
        return ResponseEntity.noContent().build();
    }
}
