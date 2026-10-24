package com.aws.storage.dto;

public record FileResponse(String key, long size, String eTag) {
}
