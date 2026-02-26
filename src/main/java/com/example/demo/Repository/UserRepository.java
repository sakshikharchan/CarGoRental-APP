package com.example.demo.Repository;

import com.example.demo.Model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> User.builder()
            .id(rs.getLong("id"))
            .fullName(rs.getString("full_name"))
            .email(rs.getString("email"))
            .password(rs.getString("password"))
            .phone(rs.getString("phone"))
            .address(rs.getString("address"))
            .profileImage(rs.getString("profile_image"))
            .isActive(rs.getBoolean("is_active"))
            .roleId(rs.getLong("role_id"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public Optional<User> findByEmail(String email) {
        List<User> users = jdbcTemplate.query(
                "SELECT * FROM users WHERE email = ?", userRowMapper, email);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query(
                "SELECT * FROM users WHERE id = ?", userRowMapper, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public long save(User user) {
        String sql = "INSERT INTO users (full_name, email, password, phone, address, " +
                "profile_image, is_active, role_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getAddress());
            ps.setString(6, user.getProfileImage());
            ps.setBoolean(7, user.isActive());
            ps.setLong(8, user.getRoleId());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void update(User user) {
        jdbcTemplate.update(
                "UPDATE users SET full_name=?, phone=?, address=?, profile_image=?, is_active=? WHERE id=?",
                user.getFullName(), user.getPhone(), user.getAddress(),
                user.getProfileImage(), user.isActive(), user.getId());
    }

    public List<User> findAll(int page, int size) {
        return jdbcTemplate.query(
                "SELECT * FROM users LIMIT ? OFFSET ?", userRowMapper, size, page * size);
    }

    public long countByRoleName(String roleName) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users u JOIN roles r ON u.role_id = r.id WHERE r.name = ?",
                Long.class, roleName);
        return count != null ? count : 0;
    }
}