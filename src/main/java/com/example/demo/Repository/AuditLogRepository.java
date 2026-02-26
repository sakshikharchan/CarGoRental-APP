package com.example.demo.Repository;

import com.example.demo.Model.AuditLog;
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
public class AuditLogRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AuditLog> rowMapper = (rs, rowNum) -> AuditLog.builder()
            .id(rs.getLong("id"))
            .userId(rs.getObject("user_id") != null ? rs.getLong("user_id") : null)
            .action(rs.getString("action"))
            .entityType(rs.getString("entity_type"))
            .entityId(rs.getObject("entity_id") != null ? rs.getLong("entity_id") : null)
            .oldValue(rs.getString("old_value"))
            .newValue(rs.getString("new_value"))
            .details(rs.getString("details"))
            .ipAddress(rs.getString("ip_address"))
            .userAgent(rs.getString("user_agent"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public List<AuditLog> findAll(int page, int size) {
        return jdbcTemplate.query(
                "SELECT al.*, u.full_name AS user_full_name, u.email AS user_email FROM audit_logs al " +
                "LEFT JOIN users u ON al.user_id = u.id ORDER BY al.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    AuditLog log = rowMapper.mapRow(rs, rowNum);
                    log.setUserFullName(rs.getString("user_full_name"));
                    log.setUserEmail(rs.getString("user_email"));
                    return log;
                }, size, page * size);
    }

    public void save(AuditLog log) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_value, new_value, ip_address, user_agent) VALUES (?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql,
                log.getUserId(), log.getAction(), log.getEntityType(), log.getEntityId(),
                log.getOldValue(), log.getNewValue(), log.getIpAddress(), log.getUserAgent());
    }
}