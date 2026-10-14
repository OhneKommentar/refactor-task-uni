package com.instakill.feed.domain;

public enum FeedSort {
    NEW,
    TRENDING;

    public static FeedSort from(String value) {
        if (value == null) {
            return NEW;
        }
        return switch (value.toLowerCase()) {
            case "trending" -> TRENDING;
            default -> NEW;
        };
    }
}
