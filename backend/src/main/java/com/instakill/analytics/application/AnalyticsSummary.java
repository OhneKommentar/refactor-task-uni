package com.instakill.analytics.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalyticsSummary(
        Totals totals,
        List<WeekStats> weekly,
        List<TopPost> topPosts,
        MediaTypeStats mediaTypes,
        Ranking ranking
) {

    public record Totals(long posts, long likes, long comments, Double likeToCommentRatio) {
    }

    public record WeekStats(String week, long posts, long likes, long comments) {
    }

    public record TopPost(UUID id, String content, String imageUrl, Instant createdAt,
                          long likes, long comments, long engagement) {
    }

    public record MediaTypeStats(MediaStats photos, MediaStats videos) {
    }

    public record MediaStats(long posts, Double averageLikes) {
    }

    public record Ranking(Double averageEngagementPerPost, Long position, long rankedUsers, Double topPercent) {
    }
}
