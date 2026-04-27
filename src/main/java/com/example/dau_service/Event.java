package com.example.dau_service;

import java.time.Instant;

public record Event(int userId, int authorId, Instant timestamp) {
    public Event(int userId, int authorId) {
        this(userId, authorId, Instant.now());
    }
}