package com.fund.research.module.admin.service.impl;

import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.module.admin.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    private static final List<String> BUSINESS_TABLES = List.of(
            "fund_user",
            "fund_company",
            "fund_manager",
            "fund_info",
            "fund_manager_relation",
            "fund_nav",
            "fund_performance_metric",
            "fund_tag",
            "fund_tag_relation",
            "company_tag_relation",
            "manager_tag_relation",
            "fund_score",
            "fund_holding_report",
            "fund_holding_detail",
            "fund_attribution",
            "fund_announcement",
            "fund_portfolio",
            "portfolio_fund_relation",
            "user_favorite",
            "fund_compare_record",
            "fund_compare_item",
            "ai_analysis_record",
            "user_saved_filter",
            "user_recent_fund_view",
            "community_author_profile",
            "community_post",
            "community_post_view",
            "community_comment",
            "community_like",
            "community_author_follow",
            "ai_recommend_record",
            "ai_recommend_item",
            "data_import_batch",
            "data_import_error"
    );

    @Override
    public List<String> businessTables() {
        return BUSINESS_TABLES;
    }

    @Override
    public Map<String, Object> dbCheck() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String schema = conn.getSchema();

            // Build IN-clause placeholders for business tables (case-insensitive match via lower()).
            String placeholders = String.join(",", BUSINESS_TABLES.stream().map(t -> "?").toList());
            String sql = "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = ? AND lower(table_name) IN (" + placeholders + ")";

            Object[] args = new Object[BUSINESS_TABLES.size() + 1];
            args[0] = schema == null ? "public" : schema;
            for (int i = 0; i < BUSINESS_TABLES.size(); i++) {
                args[i + 1] = BUSINESS_TABLES.get(i).toLowerCase();
            }

            List<String> existing = jdbcTemplate.queryForList(sql, args, String.class);

            Map<String, Object> dbInfo = new LinkedHashMap<>();
            dbInfo.put("databaseProductName", meta.getDatabaseProductName());
            dbInfo.put("databaseProductVersion", meta.getDatabaseProductVersion());
            dbInfo.put("driverName", meta.getDriverName());
            dbInfo.put("driverVersion", meta.getDriverVersion());
            dbInfo.put("url", meta.getURL());
            dbInfo.put("schema", schema);

            result.put("database", dbInfo);
            result.put("expectedTableCount", BUSINESS_TABLES.size());
            result.put("existingTableCount", existing.size());
            result.put("existingTables", existing);
            result.put("missingTables", BUSINESS_TABLES.stream()
                    .filter(t -> existing.stream().noneMatch(e -> e.equalsIgnoreCase(t)))
                    .toList());
            return result;
        } catch (Exception ex) {
            log.error("dbCheck failed", ex);
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "数据库检查失败: " + ex.getMessage());
        }
    }
}
