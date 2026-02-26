//package com.example.demo.Repository;
//
//import com.example.demo.Model.Car;
//import lombok.RequiredArgsConstructor;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.RowMapper;
//import org.springframework.jdbc.support.GeneratedKeyHolder;
//import org.springframework.jdbc.support.KeyHolder;
//import org.springframework.stereotype.Repository;
//
//import java.sql.PreparedStatement;
//import java.sql.Statement;
//import java.sql.Types;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//@RequiredArgsConstructor
//public class CarRepository {
//
//    private final JdbcTemplate jdbcTemplate;
//
//    private final RowMapper<Car> rowMapper = (rs, rowNum) -> Car.builder()
//            .id(rs.getLong("id"))
//            .vendorId(rs.getLong("vendor_id"))
//            .categoryId(rs.getLong("category_id"))
//            .brand(rs.getString("brand"))
//            .model(rs.getString("model"))
//            .year(rs.getInt("year"))
//            .registrationNo(rs.getString("registration_no"))
//            .color(rs.getString("color"))
//            .seats(rs.getObject("seats", Integer.class)) // ✅ FIXED (null-safe)
//            .fuelType(rs.getString("fuel_type"))
//            .transmission(rs.getString("transmission"))
//            .dailyRate(rs.getBigDecimal("daily_rate"))
//            .imageUrl(rs.getString("image_url"))
//            .available(rs.getObject("is_available", Boolean.class)) // ✅ FIXED
//            .createdAt(rs.getTimestamp("created_at") != null
//                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
//            .updatedAt(rs.getTimestamp("updated_at") != null
//                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
//            .build();
//
//    public List<Car> findAll(int page, int size) {
//        return jdbcTemplate.query(
//                "SELECT * FROM cars ORDER BY created_at DESC LIMIT ? OFFSET ?",
//                rowMapper, size, page * size);
//    }
//
//    public List<Car> findAvailable(int page, int size) {
//        return jdbcTemplate.query(
//                "SELECT * FROM cars WHERE is_available = true ORDER BY created_at DESC LIMIT ? OFFSET ?",
//                rowMapper, size, page * size);
//    }
//
//    public Optional<Car> findById(Long id) {
//        List<Car> list = jdbcTemplate.query(
//                "SELECT * FROM cars WHERE id = ?", rowMapper, id);
//        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
//    }
//
//    public List<Car> findByVendorId(Long vendorId, int page, int size) {
//        return jdbcTemplate.query(
//                "SELECT * FROM cars WHERE vendor_id = ? LIMIT ? OFFSET ?",
//                rowMapper, vendorId, size, page * size);
//    }
//
//    public List<Car> search(String brand, String fuelType, String transmission,
//                            Long categoryId, Boolean isAvailable, int page, int size) {
//
//        StringBuilder sql = new StringBuilder("SELECT * FROM cars WHERE 1=1");
//        List<Object> params = new ArrayList<>();
//
//        if (brand != null && !brand.isBlank()) {
//            sql.append(" AND brand LIKE ?");
//            params.add("%" + brand + "%");
//        }
//
//        if (fuelType != null && !fuelType.isBlank()) {
//            sql.append(" AND fuel_type = ?");
//            params.add(fuelType.toUpperCase());
//        }
//
//        if (transmission != null && !transmission.isBlank()) {
//            sql.append(" AND transmission = ?");
//            params.add(transmission.toUpperCase());
//        }
//
//        if (categoryId != null) {
//            sql.append(" AND category_id = ?");
//            params.add(categoryId);
//        }
//
//        if (isAvailable != null) {
//            sql.append(" AND is_available = ?");
//            params.add(isAvailable);
//        }
//
//        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
//        params.add(size);
//        params.add(page * size);
//
//        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
//    }
//
//    public long save(Car car) {
//
//        String sql = "INSERT INTO cars (vendor_id, category_id, brand, model, year, " +
//                "registration_no, color, seats, fuel_type, transmission, daily_rate, image_url, is_available) " +
//                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//
//        KeyHolder keyHolder = new GeneratedKeyHolder();
//
//        jdbcTemplate.update(con -> {
//            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
//
//            ps.setLong(1, car.getVendorId());
//            ps.setLong(2, car.getCategoryId());
//            ps.setString(3, car.getBrand());
//            ps.setString(4, car.getModel());
//            if (car.getYear() != null) ps.setInt(5, car.getYear()); else ps.setNull(5, java.sql.Types.INTEGER);
//            ps.setString(6, car.getRegistrationNo());
//            ps.setString(7, car.getColor());
//
//            // ✅ NULL SAFE seats
//            if (car.getSeats() != null) {
//                ps.setInt(8, car.getSeats());
//            } else {
//                ps.setNull(8, Types.INTEGER);
//            }
//
//            ps.setString(9, car.getFuelType());
//            ps.setString(10, car.getTransmission());
//            ps.setBigDecimal(11, car.getDailyRate());
//            ps.setString(12, car.getImageUrl());
//
//            // ✅ NULL SAFE available
//            if (car.getAvailable() != null) {
//                ps.setBoolean(13, car.getAvailable());
//            } else {
//                ps.setBoolean(13, true); // default true
//            }
//
//            return ps;
//        }, keyHolder);
//
//        return keyHolder.getKey().longValue();
//    }
//
//    public void update(Car car) {
//
//        String sql = "UPDATE cars SET vendor_id=?, category_id=?, brand=?, model=?, year=?, " +
//                "registration_no=?, color=?, seats=?, fuel_type=?, transmission=?, " +
//                "daily_rate=?, image_url=?, is_available=? WHERE id=?";
//
//        jdbcTemplate.update(sql,
//                car.getVendorId(),
//                car.getCategoryId(),
//                car.getBrand(),
//                car.getModel(),
//                car.getYear(),
//                car.getRegistrationNo(),
//                car.getColor(),
//                car.getSeats(),
//                car.getFuelType(),
//                car.getTransmission(),
//                car.getDailyRate(),
//                car.getImageUrl(),
//                car.getAvailable(),
//                car.getId());
//    }
//
//    public void delete(Long id) {
//        jdbcTemplate.update("DELETE FROM cars WHERE id = ?", id);
//    }
//
//    public void updateAvailability(Long id, Boolean available) {
//        jdbcTemplate.update("UPDATE cars SET is_available = ? WHERE id = ?", available, id);
//    }
//
//    public boolean existsByRegistrationNo(String registrationNo) {
//        Integer count = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM cars WHERE registration_no = ?",
//                Integer.class, registrationNo);
//
//        return count != null && count > 0;
//    }
//
//    public long countAll() {
//        Long count = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM cars", Long.class);
//
//        return count != null ? count : 0;
//    }
//
//    public long countAvailable() {
//        Long count = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM cars WHERE is_available = true",
//                Long.class);
//
//        return count != null ? count : 0;
//    }
//}
package com.example.demo.Repository;

import com.example.demo.Model.Car;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CarRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Car> rowMapper = (rs, rowNum) -> Car.builder()
            .id(rs.getLong("id"))
            .vendorId(rs.getLong("vendor_id"))
            .categoryId(rs.getLong("category_id"))
            .brand(rs.getString("brand"))
            .model(rs.getString("model"))
            .year(rs.getInt("year"))
            .registrationNo(rs.getString("registration_no"))
            .color(rs.getString("color"))
            .seats(rs.getObject("seats", Integer.class))
            .fuelType(rs.getString("fuel_type"))
            .transmission(rs.getString("transmission"))
            .dailyRate(rs.getBigDecimal("daily_rate"))
            .imageUrl(rs.getString("image_url"))
            .isAvailable(rs.getObject("is_available", Boolean.class)) // ✅ FIXED: was .available()
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public List<Car> findAll(int page, int size) {
        return jdbcTemplate.query(
                "SELECT * FROM cars ORDER BY created_at DESC LIMIT ? OFFSET ?",
                rowMapper, size, page * size);
    }

    public List<Car> findAvailable(int page, int size) {
        return jdbcTemplate.query(
                "SELECT * FROM cars WHERE is_available = true ORDER BY created_at DESC LIMIT ? OFFSET ?",
                rowMapper, size, page * size);
    }

    public Optional<Car> findById(Long id) {
        List<Car> list = jdbcTemplate.query(
                "SELECT * FROM cars WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Car> findByVendorId(Long vendorId, int page, int size) {
        return jdbcTemplate.query(
                "SELECT * FROM cars WHERE vendor_id = ? LIMIT ? OFFSET ?",
                rowMapper, vendorId, size, page * size);
    }

    public List<Car> search(String brand, String fuelType, String transmission,
                            Long categoryId, Boolean isAvailable, int page, int size) {

        StringBuilder sql = new StringBuilder("SELECT * FROM cars WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (brand != null && !brand.isBlank()) {
            sql.append(" AND brand LIKE ?");
            params.add("%" + brand + "%");
        }

        if (fuelType != null && !fuelType.isBlank()) {
            sql.append(" AND fuel_type = ?");
            params.add(fuelType.toUpperCase());
        }

        if (transmission != null && !transmission.isBlank()) {
            sql.append(" AND transmission = ?");
            params.add(transmission.toUpperCase());
        }

        if (categoryId != null) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }

        if (isAvailable != null) {
            sql.append(" AND is_available = ?");
            params.add(isAvailable);
        }

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public long save(Car car) {

        String sql = "INSERT INTO cars (vendor_id, category_id, brand, model, year, " +
                "registration_no, color, seats, fuel_type, transmission, daily_rate, image_url, is_available) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setLong(1, car.getVendorId());
            ps.setLong(2, car.getCategoryId());
            ps.setString(3, car.getBrand());
            ps.setString(4, car.getModel());
            if (car.getYear() != null) ps.setInt(5, car.getYear()); else ps.setNull(5, Types.INTEGER);
            ps.setString(6, car.getRegistrationNo());
            ps.setString(7, car.getColor());

            if (car.getSeats() != null) {
                ps.setInt(8, car.getSeats());
            } else {
                ps.setNull(8, Types.INTEGER);
            }

            ps.setString(9, car.getFuelType());
            ps.setString(10, car.getTransmission());
            ps.setBigDecimal(11, car.getDailyRate());
            ps.setString(12, car.getImageUrl());

            // ✅ FIXED: was car.getAvailable() → now car.getIsAvailable()
            if (car.getIsAvailable() != null) {
                ps.setBoolean(13, car.getIsAvailable());
            } else {
                ps.setBoolean(13, true); // default true
            }

            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public void update(Car car) {

        String sql = "UPDATE cars SET vendor_id=?, category_id=?, brand=?, model=?, year=?, " +
                "registration_no=?, color=?, seats=?, fuel_type=?, transmission=?, " +
                "daily_rate=?, image_url=?, is_available=? WHERE id=?";

        jdbcTemplate.update(sql,
                car.getVendorId(),
                car.getCategoryId(),
                car.getBrand(),
                car.getModel(),
                car.getYear(),
                car.getRegistrationNo(),
                car.getColor(),
                car.getSeats(),
                car.getFuelType(),
                car.getTransmission(),
                car.getDailyRate(),
                car.getImageUrl(),
                car.getIsAvailable(), // ✅ FIXED: was car.getAvailable()
                car.getId());
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM cars WHERE id = ?", id);
    }

    public void updateAvailability(Long id, Boolean available) {
        jdbcTemplate.update("UPDATE cars SET is_available = ? WHERE id = ?", available, id);
    }

    public boolean existsByRegistrationNo(String registrationNo) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cars WHERE registration_no = ?",
                Integer.class, registrationNo);

        return count != null && count > 0;
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cars", Long.class);

        return count != null ? count : 0;
    }

    public long countAvailable() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cars WHERE is_available = true",
                Long.class);

        return count != null ? count : 0;
    }
}