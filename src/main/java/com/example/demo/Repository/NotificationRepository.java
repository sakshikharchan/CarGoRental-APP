package com.example.demo.Repository;

import com.example.demo.Model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Notification> rowMapper = (rs, rowNum) -> Notification.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .title(rs.getString("title"))
            .message(rs.getString("message"))
            .type(rs.getString("type"))
            .referenceId(rs.getObject("reference_id") != null ? rs.getLong("reference_id") : null)
            .referenceType(rs.getString("reference_type"))
            .isRead(rs.getBoolean("is_read"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public List<Notification> findByUserId(Long userId, int page, int size) {
        return jdbcTemplate.query(
                "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
                rowMapper, userId, size, page * size);
    }

 // NotificationRepository.java

    public long countUnread(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0",
                Long.class, userId);
        return count != null ? count : 0;
    }

    public long save(Notification notification) {
        String sql = "INSERT INTO notifications (user_id, title, message, type, reference_id, reference_type) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, notification.getUserId());
            ps.setString(2, notification.getTitle());
            ps.setString(3, notification.getMessage());
            ps.setString(4, notification.getType());
            if (notification.getReferenceId() != null) ps.setLong(5, notification.getReferenceId());
            else ps.setNull(5, java.sql.Types.BIGINT);
            ps.setString(6, notification.getReferenceType());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void markAllRead(Long userId) {
        jdbcTemplate.update("UPDATE notifications SET is_read = 1 WHERE user_id = ?", userId);
    }

    public void markRead(Long id, Long userId) {
        jdbcTemplate.update("UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?", id, userId);
    }
}