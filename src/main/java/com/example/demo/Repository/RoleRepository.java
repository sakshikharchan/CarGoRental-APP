package com.example.demo.Repository;

import com.example.demo.Model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Role> roleRowMapper = (rs, rowNum) -> Role.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public Optional<Role> findByName(String name) {
        List<Role> roles = jdbcTemplate.query(
                "SELECT * FROM roles WHERE name = ?", roleRowMapper, name);
        return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
    }

    public Optional<Role> findById(Long id) {
        List<Role> roles = jdbcTemplate.query(
                "SELECT * FROM roles WHERE id = ?", roleRowMapper, id);
        return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
    }

    public List<Role> findAll() {
        return jdbcTemplate.query("SELECT * FROM roles", roleRowMapper);
    }
}