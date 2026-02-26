package com.example.demo.Repository;

import com.example.demo.Model.CarCategory;
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
public class CarCategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<CarCategory> rowMapper = (rs, rowNum) -> CarCategory.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public List<CarCategory> findAll() {
        return jdbcTemplate.query("SELECT * FROM car_categories", rowMapper);
    }

    public Optional<CarCategory> findById(Long id) {
        List<CarCategory> list =
                jdbcTemplate.query("SELECT * FROM car_categories WHERE id = ?", rowMapper, id);

        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public long save(CarCategory category) {
        String sql = "INSERT INTO car_categories (name, description) VALUES (?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps =
                    con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public void update(CarCategory category) {
        jdbcTemplate.update(
                "UPDATE car_categories SET name=?, description=? WHERE id=?",
                category.getName(),
                category.getDescription(),
                category.getId()
        );
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM car_categories WHERE id = ?", id);
    }
}