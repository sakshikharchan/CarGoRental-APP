package com.example.demo.Repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CostBreakdownRepository {

    private final JdbcTemplate jdbcTemplate;

    // ── State Tax ──────────────────────────────────────────────────────────────

    public Map<String, Object> findStateTax(String stateCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT state_code, state_name, tax_rate FROM state_tax_config WHERE state_code = ? AND is_active = TRUE",
                stateCode.toUpperCase());
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> findAllStateTaxes() {
        return jdbcTemplate.queryForList(
                "SELECT * FROM state_tax_config WHERE is_active = TRUE ORDER BY state_name");
    }

    // ── Insurance Plans ────────────────────────────────────────────────────────

    public List<Map<String, Object>> findInsurancePlans(Long categoryId) {
        return jdbcTemplate.queryForList("""
            SELECT * FROM insurance_plans
            WHERE is_active = TRUE
              AND (category_id = ? OR category_id IS NULL)
            ORDER BY daily_premium ASC
            """, categoryId);
    }

    public Map<String, Object> findInsurancePlanById(Long planId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM insurance_plans WHERE id = ? AND is_active = TRUE", planId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> findMandatoryInsurancePlan(Long categoryId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT * FROM insurance_plans
            WHERE is_mandatory = TRUE AND is_active = TRUE
              AND (category_id = ? OR category_id IS NULL)
            ORDER BY daily_premium ASC LIMIT 1
            """, categoryId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // ── Toll Estimates ─────────────────────────────────────────────────────────

    public BigDecimal findTollEstimate(String fromCity, String toCity) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT estimated_toll FROM toll_estimates
            WHERE LOWER(from_city) = LOWER(?) AND LOWER(to_city) = LOWER(?)
            """, fromCity, toCity);
        if (rows.isEmpty()) return BigDecimal.ZERO;
        Object toll = rows.get(0).get("estimated_toll");
        return toll != null ? new BigDecimal(toll.toString()) : BigDecimal.ZERO;
    }

    // ── Late Return Penalty ────────────────────────────────────────────────────

    /**
     * Returns vendor-specific penalty if set, else platform default (vendor_id IS NULL).
     */
    public Map<String, Object> findLatePenalty(Long vendorId) {
        // Try vendor-specific first
        if (vendorId != null) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM late_return_penalties
                WHERE vendor_id = ? AND is_active = TRUE LIMIT 1
                """, vendorId);
            if (!rows.isEmpty()) return rows.get(0);
        }
        // Fall back to platform default
        List<Map<String, Object>> defaults = jdbcTemplate.queryForList("""
            SELECT * FROM late_return_penalties
            WHERE vendor_id IS NULL AND is_active = TRUE LIMIT 1
            """);
        return defaults.isEmpty() ? null : defaults.get(0);
    }

    // ── Admin: manage state tax ────────────────────────────────────────────────

    public void upsertStateTax(String stateCode, String stateName, BigDecimal taxRate) {
        jdbcTemplate.update("""
            INSERT INTO state_tax_config (state_code, state_name, tax_rate)
            VALUES (?,?,?)
            ON DUPLICATE KEY UPDATE state_name=VALUES(state_name), tax_rate=VALUES(tax_rate)
            """, stateCode.toUpperCase(), stateName, taxRate);
    }

    // ── Admin: manage insurance plans ─────────────────────────────────────────

    public void saveInsurancePlan(String planName, String description,
                                  Long categoryId, BigDecimal dailyPremium,
                                  BigDecimal coverageAmount, boolean isMandatory) {
        jdbcTemplate.update("""
            INSERT INTO insurance_plans
                (plan_name, description, category_id, daily_premium, coverage_amount, is_mandatory)
            VALUES (?,?,?,?,?,?)
            """, planName, description, categoryId, dailyPremium, coverageAmount, isMandatory);
    }

    public void toggleInsurancePlan(Long planId, boolean active) {
        jdbcTemplate.update("UPDATE insurance_plans SET is_active = ? WHERE id = ?", active, planId);
    }

    // ── Admin: manage toll routes ─────────────────────────────────────────────

    public void upsertTollRoute(String fromCity, String toCity,
                                BigDecimal estimatedToll, BigDecimal distanceKm) {
        jdbcTemplate.update("""
            INSERT INTO toll_estimates (from_city, to_city, estimated_toll, distance_km)
            VALUES (?,?,?,?)
            ON DUPLICATE KEY UPDATE estimated_toll=VALUES(estimated_toll), distance_km=VALUES(distance_km)
            """, fromCity, toCity, estimatedToll, distanceKm);
    }
}