package com.example.demo.Repository;

import com.example.demo.Model.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<RefreshToken> rowMapper = (rs, rowNum) -> RefreshToken.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .token(rs.getString("token"))
            .expiryDate(rs.getTimestamp("expiry_date") != null
                    ? rs.getTimestamp("expiry_date").toLocalDateTime() : null)
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public Optional<RefreshToken> findByToken(String token) {
        String sql = "SELECT * FROM refresh_tokens WHERE token = ?";
        List<RefreshToken> list = jdbcTemplate.query(sql, rowMapper, token);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public long save(RefreshToken token) {
        String sql = "INSERT INTO refresh_tokens (user_id, token, expiry_date) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, token.getUserId());
            ps.setString(2, token.getToken());
            ps.setTimestamp(3, Timestamp.valueOf(token.getExpiryDate()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void deleteByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId);
    }

    public void deleteByToken(String token) {
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE token = ?", token);
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(java.time.LocalDateTime.now());
    }
}