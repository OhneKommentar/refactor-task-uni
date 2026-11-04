package com.instakill.analytics.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Statistics for the posts of one author. All numbers are calculated with SQL against the
 * posts, likes and comments tables.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    /**
     * Per post: like and comment counts. Uses the posts.like_count and posts.comment_count columns,
     * which the application keeps in sync on every like and comment. Aggregating the likes and
     * comments tables for every post would scan them on each request.
     */
    private static final String POST_STATS = """
            post_stats AS (
                SELECT p.id, p.author_id, p.content, p.image_url, p.created_at,
                       p.like_count AS likes,
                       p.comment_count AS comments
                FROM posts p
            )
            """;

    private static final String TOTALS_SQL = "WITH " + POST_STATS + """
            SELECT COUNT(*) AS posts,
                   COALESCE(SUM(likes), 0) AS likes,
                   COALESCE(SUM(comments), 0) AS comments,
                   CASE WHEN COALESCE(SUM(comments), 0) = 0 THEN NULL
                        ELSE CAST(SUM(likes) AS REAL) / SUM(comments) END AS like_to_comment_ratio
            FROM post_stats
            WHERE author_id = ?
            """;

    /** Likes and comments are grouped by the week in which they were created, posts by their creation week. */
    private static final String WEEKLY_SQL = """
            SELECT strftime('%Y-W%W', ts / 1000, 'unixepoch') AS week,
                   SUM(kind = 'post') AS posts,
                   SUM(kind = 'like') AS likes,
                   SUM(kind = 'comment') AS comments
            FROM (
                SELECT 'post' AS kind, p.created_at AS ts
                FROM posts p
                WHERE p.author_id = ?
                UNION ALL
                SELECT 'like', l.created_at
                FROM likes l
                JOIN posts p ON p.id = l.post_id
                WHERE p.author_id = ?
                UNION ALL
                SELECT 'comment', c.created_at
                FROM comments c
                JOIN posts p ON p.id = c.post_id
                WHERE p.author_id = ?
            ) events
            GROUP BY week
            ORDER BY week
            """;

    private static final String TOP_POSTS_SQL = "WITH " + POST_STATS + """
            SELECT id, content, image_url, created_at, likes, comments, likes + comments AS engagement
            FROM post_stats
            WHERE author_id = ?
            ORDER BY engagement DESC, created_at DESC
            LIMIT 5
            """;

    /** Posts without image_url are text-only and are not part of the photo/video comparison. */
    private static final String MEDIA_SQL = "WITH " + POST_STATS + """
            SELECT CASE WHEN LOWER(image_url) LIKE '%.mp4'
                          OR LOWER(image_url) LIKE '%.mov'
                          OR LOWER(image_url) LIKE '%.webm' THEN 'video' ELSE 'photo' END AS media_type,
                   COUNT(*) AS posts,
                   AVG(likes) AS average_likes
            FROM post_stats
            WHERE author_id = ? AND image_url IS NOT NULL AND image_url <> ''
            GROUP BY media_type
            """;

    /**
     * Average engagement (likes + comments) per post for every author with at least one post.
     * The position is 1 + the number of authors with a strictly higher average.
     */
    private static final String RANKING_SQL = "WITH " + POST_STATS + """
            , user_avg AS (
                SELECT author_id, AVG(likes + comments) AS avg_engagement
                FROM post_stats
                GROUP BY author_id
            )
            SELECT (SELECT avg_engagement FROM user_avg WHERE author_id = ?) AS my_avg,
                   (SELECT COUNT(*) FROM user_avg
                    WHERE avg_engagement > (SELECT avg_engagement FROM user_avg WHERE author_id = ?)) AS users_ahead,
                   (SELECT COUNT(*) FROM user_avg) AS ranked_users
            """;

    private final JdbcTemplate jdbc;

    public AnalyticsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AnalyticsSummary summaryFor(UUID userId) {
        String author = userId.toString();
        return new AnalyticsSummary(
                totals(author),
                weekly(author),
                topPosts(author),
                mediaTypes(author),
                ranking(author)
        );
    }

    private AnalyticsSummary.Totals totals(String author) {
        return jdbc.queryForObject(TOTALS_SQL, (rs, row) -> new AnalyticsSummary.Totals(
                rs.getLong("posts"),
                rs.getLong("likes"),
                rs.getLong("comments"),
                nullableDouble(rs, "like_to_comment_ratio")
        ), author);
    }

    private List<AnalyticsSummary.WeekStats> weekly(String author) {
        return jdbc.query(WEEKLY_SQL, (rs, row) -> new AnalyticsSummary.WeekStats(
                rs.getString("week"),
                rs.getLong("posts"),
                rs.getLong("likes"),
                rs.getLong("comments")
        ), author, author, author);
    }

    private List<AnalyticsSummary.TopPost> topPosts(String author) {
        return jdbc.query(TOP_POSTS_SQL, (rs, row) -> new AnalyticsSummary.TopPost(
                UUID.fromString(rs.getString("id")),
                rs.getString("content"),
                rs.getString("image_url"),
                Instant.ofEpochMilli(rs.getLong("created_at")),
                rs.getLong("likes"),
                rs.getLong("comments"),
                rs.getLong("engagement")
        ), author);
    }

    private AnalyticsSummary.MediaTypeStats mediaTypes(String author) {
        Map<String, AnalyticsSummary.MediaStats> byType = new HashMap<>();
        jdbc.query(MEDIA_SQL, (ResultSet rs) -> {
            byType.put(rs.getString("media_type"), new AnalyticsSummary.MediaStats(
                    rs.getLong("posts"),
                    nullableDouble(rs, "average_likes")
            ));
        }, author);
        return new AnalyticsSummary.MediaTypeStats(
                byType.getOrDefault("photo", new AnalyticsSummary.MediaStats(0, null)),
                byType.getOrDefault("video", new AnalyticsSummary.MediaStats(0, null))
        );
    }

    private AnalyticsSummary.Ranking ranking(String author) {
        return jdbc.queryForObject(RANKING_SQL, (rs, row) -> {
            Double myAverage = nullableDouble(rs, "my_avg");
            long rankedUsers = rs.getLong("ranked_users");
            if (myAverage == null || rankedUsers == 0) {
                return new AnalyticsSummary.Ranking(null, null, rankedUsers, null);
            }
            long position = rs.getLong("users_ahead") + 1;
            double topPercent = position * 100.0 / rankedUsers;
            return new AnalyticsSummary.Ranking(myAverage, position, rankedUsers, topPercent);
        }, author, author);
    }

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : rs.getDouble(column);
    }
}
