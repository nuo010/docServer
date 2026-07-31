package com.example.docserver.service;

import org.springframework.http.MediaType;

public record DocumentResult(byte[] content, String filename, MediaType mediaType) {
}
