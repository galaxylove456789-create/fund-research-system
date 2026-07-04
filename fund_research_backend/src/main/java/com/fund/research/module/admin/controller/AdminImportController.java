package com.fund.research.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.research.common.PageResult;
import com.fund.research.common.Result;
import com.fund.research.module.admin.mapper.AdminImportMapper;
import com.fund.research.module.admin.vo.ImportBatchVO;
import com.fund.research.module.admin.vo.ImportErrorVO;
import com.fund.research.security.PasswordPolicy;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Tag(name = "管理员后台", description = "数据导入、标签维护、评分重算、基金展示和用户管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminImportController {

    private final AdminImportMapper adminImportMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/import/batches")
    public Result<PageResult<ImportBatchVO>> getImportBatches(
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<ImportBatchVO> page = Page.of(safePageNo, safePageSize);
        adminImportMapper.selectImportBatches(page);
        return Result.success(PageResult.of(page));
    }

    @PostMapping("/import/preview")
    public Result<Map<String, Object>> previewImport(@RequestPart(value = "file", required = false) MultipartFile file)
            throws IOException {
        ParsedCsv csv = parseCsv(file);
        List<RowError> errors = validateRows(csv).stream().limit(20).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fileName", file == null ? null : file.getOriginalFilename());
        data.put("size", file == null ? 0 : file.getSize());
        data.put("totalCount", csv.rows().size());
        data.put("validCount", Math.max(0, csv.rows().size() - validateRows(csv).size()));
        data.put("errorCount", validateRows(csv).size());
        data.put("fields", buildFieldPreview(csv.headers()));
        data.put("sampleRows", csv.rows().stream().limit(5).toList());
        data.put("errors", errors);
        return Result.success(data);
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importData(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "importType", required = false) String importType) throws IOException {
        ParsedCsv csv = parseCsv(file);
        String safeImportType = StringUtils.hasText(importType) ? importType : "fund_basic";
        List<RowError> errors = validateRows(csv);
        long batchId = nextId("data_import_batch", "batch_id");
        String batchNo = "IMPORT_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());

        int successCount = 0;
        if ("fund_basic".equalsIgnoreCase(safeImportType)) {
            for (int i = 0; i < csv.rows().size(); i++) {
                int rowNo = i + 2;
                if (errors.stream().anyMatch(error -> error.rowNo() == rowNo)) {
                    continue;
                }
                upsertFundBasic(csv.rows().get(i));
                successCount++;
            }
        }

        int errorCount = errors.size();
        jdbcTemplate.update("""
                INSERT INTO data_import_batch (
                    batch_id, batch_no, import_type, file_name, file_hash,
                    total_count, success_count, error_count, field_mapping,
                    validate_summary, status, operator_id, created_time, finished_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                batchId,
                batchNo,
                safeImportType,
                file == null ? null : file.getOriginalFilename(),
                null,
                csv.rows().size(),
                successCount,
                errorCount,
                toJsonLike(buildFieldPreview(csv.headers())),
                "校验完成：成功 " + successCount + " 条，异常 " + errorCount + " 条",
                errorCount == 0 ? "SUCCESS" : (successCount > 0 ? "PARTIAL_SUCCESS" : "FAILED"),
                1L);
        for (RowError error : errors) {
            insertImportError(batchId, error);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("batchId", String.valueOf(batchId));
        data.put("batchNo", batchNo);
        data.put("importType", safeImportType);
        data.put("totalCount", csv.rows().size());
        data.put("successCount", successCount);
        data.put("errorCount", errorCount);
        data.put("status", errorCount == 0 ? "SUCCESS" : (successCount > 0 ? "PARTIAL_SUCCESS" : "FAILED"));
        return Result.success(data);
    }

    @GetMapping("/import/errors")
    public Result<PageResult<ImportErrorVO>> getImportErrors(
            @RequestParam(value = "batchId", required = false) Long batchId,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<ImportErrorVO> page = Page.of(safePageNo, safePageSize);
        adminImportMapper.selectImportErrors(page, batchId);
        return Result.success(PageResult.of(page));
    }

    @PostMapping("/score/recalculate")
    public Result<Map<String, Object>> recalculateScores() {
        List<Map<String, Object>> metrics = jdbcTemplate.queryForList("""
                SELECT f.fund_id, f.fund_name, f.fund_scale,
                       pm.return_rate, pm.max_drawdown, pm.volatility, pm.sharpe_ratio
                FROM fund_info f
                LEFT JOIN fund_performance_metric pm ON pm.fund_id = f.fund_id
                    AND pm.period_code = '1Y'
                    AND pm.stat_date = (
                        SELECT MAX(stat_date)
                        FROM fund_performance_metric
                        WHERE fund_id = f.fund_id AND period_code = '1Y'
                    )
                WHERE f.status = 1
                """);
        jdbcTemplate.update("DELETE FROM fund_score WHERE score_date = CURRENT_DATE");
        int count = 0;
        for (Map<String, Object> row : metrics) {
            BigDecimal returnRate = decimal(row.get("return_rate"));
            BigDecimal maxDrawdown = decimal(row.get("max_drawdown"));
            BigDecimal volatility = decimal(row.get("volatility"));
            BigDecimal sharpe = decimal(row.get("sharpe_ratio"));
            BigDecimal scale = decimal(row.get("fund_scale"));

            double yieldScore = clamp(55 + returnRate.doubleValue() * 0.7, 0, 100);
            double riskScore = clamp(100 - maxDrawdown.doubleValue() * 2.0, 0, 100);
            double stabilityScore = clamp(100 - volatility.doubleValue() * 1.5, 0, 100);
            double managerScore = clamp(70 + sharpe.doubleValue() * 8, 0, 100);
            double scaleScore = clamp(60 + Math.min(scale.doubleValue(), 100) * 0.25, 0, 100);
            double totalScore = yieldScore * 0.30 + riskScore * 0.25 + stabilityScore * 0.20
                    + managerScore * 0.15 + scaleScore * 0.10;
            String level = totalScore >= 85 ? "RECOMMEND" : totalScore >= 70 ? "WATCH" : "NEUTRAL";
            jdbcTemplate.update("""
                    INSERT INTO fund_score (
                        score_id, fund_id, score_date, yield_score, risk_score,
                        stability_score, manager_score, scale_score, total_score,
                        recommend_level, explain_text, created_time
                    ) VALUES ((SELECT COALESCE(MAX(score_id), 0) + 1 FROM fund_score),
                              ?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    row.get("fund_id"),
                    round(yieldScore),
                    round(riskScore),
                    round(stabilityScore),
                    round(managerScore),
                    round(scaleScore),
                    round(totalScore),
                    level,
                    "后台按近一年收益、最大回撤、波动率、夏普比率和基金规模重新计算综合评分。");
            count++;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "SUCCESS");
        data.put("recalculatedCount", count);
        data.put("scoreDate", LocalDate.now().toString());
        return Result.success(data);
    }

    @PostMapping("/tags/rebuild-score")
    public Result<Map<String, Object>> rebuildTagScore() {
        return recalculateScores();
    }

    @GetMapping("/tags/rules")
    public Result<List<Map<String, Object>>> getTagRules() {
        List<Map<String, Object>> rules = jdbcTemplate.queryForList("""
                SELECT tag_id AS "tagId",
                       tag_code AS "tagCode",
                       tag_name AS "name",
                       tag_category AS "type",
                       COALESCE(rule_expression, description) AS "condition",
                       description AS "description",
                       enabled AS "enabled"
                FROM fund_tag
                ORDER BY tag_category, tag_id
                """);
        for (Map<String, Object> rule : rules) {
            Object enabledValue = rule.get("enabled");
            boolean enabled = enabledValue instanceof Number number && number.intValue() == 1;
            rule.put("source", "规则引擎");
            rule.put("status", enabled ? "启用" : "停用");
        }
        return Result.success(rules);
    }

    @PostMapping("/tags/rules")
    public Result<Map<String, Object>> saveTagRule(@RequestBody Map<String, Object> request) {
        String name = Objects.toString(request.get("name"), "").trim();
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("标签名称不能为空");
        }
        long tagId = nextId("fund_tag", "tag_id");
        String tagCode = Objects.toString(request.get("tagCode"), "").trim();
        if (!StringUtils.hasText(tagCode)) {
            tagCode = "CUSTOM_" + tagId;
        }
        jdbcTemplate.update("""
                INSERT INTO fund_tag (
                    tag_id, tag_code, tag_name, tag_category,
                    description, rule_expression, enabled
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                tagId,
                tagCode,
                name,
                blankToNull(Objects.toString(request.get("type"), "")),
                blankToNull(Objects.toString(request.get("description"), "")),
                blankToNull(Objects.toString(request.get("condition"), "")),
                boolValue(request.getOrDefault("enabled", true)) ? 1 : 0);
        Map<String, Object> data = new LinkedHashMap<>(request);
        data.put("tagId", String.valueOf(tagId));
        data.put("tagCode", tagCode);
        data.put("enabled", boolValue(request.getOrDefault("enabled", true)) ? 1 : 0);
        data.put("status", boolValue(request.getOrDefault("enabled", true)) ? "启用" : "停用");
        return Result.success(data);
    }

    @PutMapping("/tags/rules/{tagId}")
    public Result<Map<String, Object>> updateTagRule(
            @PathVariable("tagId") Long tagId,
            @RequestBody Map<String, Object> request) {
        String name = Objects.toString(request.get("name"), "").trim();
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("标签名称不能为空");
        }
        String tagCode = Objects.toString(request.get("tagCode"), "").trim();
        if (!StringUtils.hasText(tagCode)) {
            tagCode = jdbcTemplate.queryForObject("SELECT tag_code FROM fund_tag WHERE tag_id = ?", String.class, tagId);
        }
        jdbcTemplate.update("""
                UPDATE fund_tag
                SET tag_code = ?,
                    tag_name = ?,
                    tag_category = ?,
                    description = ?,
                    rule_expression = ?,
                    enabled = ?
                WHERE tag_id = ?
                """,
                tagCode,
                name,
                blankToNull(Objects.toString(request.get("type"), "")),
                blankToNull(Objects.toString(request.get("description"), "")),
                blankToNull(Objects.toString(request.get("condition"), "")),
                boolValue(request.getOrDefault("enabled", true)) ? 1 : 0,
                tagId);
        Map<String, Object> data = new LinkedHashMap<>(request);
        data.put("tagId", tagId);
        data.put("enabled", boolValue(request.getOrDefault("enabled", true)) ? 1 : 0);
        data.put("status", boolValue(request.getOrDefault("enabled", true)) ? "启用" : "停用");
        return Result.success(data);
    }

    @DeleteMapping("/tags/rules/{tagId}")
    public Result<Map<String, Object>> deleteTagRule(@PathVariable("tagId") Long tagId) {
        jdbcTemplate.update("DELETE FROM fund_tag_relation WHERE tag_id = ?", tagId);
        jdbcTemplate.update("DELETE FROM company_tag_relation WHERE tag_id = ?", tagId);
        jdbcTemplate.update("DELETE FROM manager_tag_relation WHERE tag_id = ?", tagId);
        jdbcTemplate.update("DELETE FROM fund_tag WHERE tag_id = ?", tagId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tagId", String.valueOf(tagId));
        data.put("deleted", true);
        return Result.success(data);
    }

    @GetMapping("/funds")
    public Result<List<Map<String, Object>>> getAdminFunds() {
        return Result.success(jdbcTemplate.queryForList("""
                SELECT f.fund_id AS "fundId",
                       f.fund_code AS "fundCode",
                       f.fund_name AS "fundName",
                       f.fund_type AS "fundType",
                       f.risk_level AS "riskLevel",
                       COALESCE(c.company_name, '未维护') AS "companyName",
                       f.fund_scale AS "fundScale",
                       f.status AS "status",
                       f.source AS "source",
                       f.updated_time AS "updatedTime"
                FROM fund_info f
                LEFT JOIN fund_company c ON c.company_id = f.company_id
                ORDER BY f.status DESC, f.fund_id DESC
                """));
    }

    @PutMapping("/funds/{fundId}/visibility")
    public Result<Map<String, Object>> updateFundVisibility(
            @PathVariable("fundId") Long fundId,
            @RequestBody Map<String, Object> request) {
        int status = boolValue(request.get("visible")) ? 1 : 0;
        jdbcTemplate.update("UPDATE fund_info SET status = ?, updated_time = CURRENT_TIMESTAMP WHERE fund_id = ?",
                status, fundId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fundId", String.valueOf(fundId));
        data.put("visible", status == 1);
        return Result.success(data);
    }

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getUsers() {
        return Result.success(jdbcTemplate.queryForList("""
                SELECT user_id AS "userId",
                       username AS "username",
                       role_code AS "roleCode",
                       risk_preference AS "riskPreference",
                       status AS "status",
                       created_time AS "createdTime",
                       updated_time AS "updatedTime"
                FROM fund_user
                ORDER BY role_code, user_id
                """));
    }

    @PutMapping("/users/{userId}/status")
    public Result<Map<String, Object>> updateUserStatus(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, Object> request) {
        int status = boolValue(request.get("enabled")) ? 1 : 0;
        jdbcTemplate.update("UPDATE fund_user SET status = ?, updated_time = CURRENT_TIMESTAMP WHERE user_id = ?",
                status, userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", String.valueOf(userId));
        data.put("enabled", status == 1);
        return Result.success(data);
    }

    @PutMapping("/users/{userId}/role")
    public Result<Map<String, Object>> updateUserRole(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, Object> request) {
        String role = Objects.toString(request.getOrDefault("roleCode", "USER"), "USER").toUpperCase(Locale.ROOT);
        if (!"ADMIN".equals(role)) {
            role = "USER";
        }
        jdbcTemplate.update("UPDATE fund_user SET role_code = ?, updated_time = CURRENT_TIMESTAMP WHERE user_id = ?",
                role, userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", String.valueOf(userId));
        data.put("roleCode", role);
        return Result.success(data);
    }

    @PostMapping("/users")
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> request) {
        String username = Objects.toString(request.get("username"), "").trim();
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM fund_user WHERE username = ?",
                Integer.class, username);
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String password = Objects.toString(request.getOrDefault("password", "123456"), "123456");
        PasswordPolicy.validate(username, password);
        String role = Objects.toString(request.getOrDefault("roleCode", "USER"), "USER").toUpperCase(Locale.ROOT);
        if (!"ADMIN".equals(role)) {
            role = "USER";
        }
        long userId = nextId("fund_user", "user_id");
        jdbcTemplate.update("""
                INSERT INTO fund_user (
                    user_id, username, password_hash, role_code, risk_preference,
                    status, created_time, updated_time, avatar, signature
                ) VALUES (?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
                """,
                userId,
                username,
                passwordEncoder.encode(password),
                role,
                Objects.toString(request.getOrDefault("riskPreference", "BALANCED"), "BALANCED"),
                username.substring(0, 1).toUpperCase(Locale.ROOT),
                "由管理员在后台创建");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", String.valueOf(userId));
        data.put("username", username);
        data.put("roleCode", role);
        return Result.success(data);
    }

    private ParsedCsv parseCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请先上传 CSV 文件");
        }
        String content = decode(file.getBytes());
        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        if (lines.length == 0 || !StringUtils.hasText(lines[0])) {
            throw new IllegalArgumentException("CSV 文件为空");
        }
        List<String> headers = parseCsvLine(stripBom(lines[0])).stream().map(this::normalizeHeader).toList();
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (!StringUtils.hasText(lines[i])) {
                continue;
            }
            List<String> values = parseCsvLine(lines[i]);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                row.put(headers.get(j), j < values.size() ? values.get(j).trim() : "");
            }
            rows.add(row);
        }
        return new ParsedCsv(headers, rows);
    }

    private String decode(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.contains("\uFFFD")) {
            return new String(bytes, Charset.forName("GBK"));
        }
        return text;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private List<Map<String, Object>> buildFieldPreview(List<String> headers) {
        return headers.stream().map(header -> {
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("rawField", header);
            field.put("systemField", systemFieldLabel(header));
            field.put("fieldType", fieldType(header));
            field.put("required", "fund_code".equals(header) || "fund_name".equals(header));
            field.put("status", systemFieldLabel(header).equals("未识别") ? "待确认" : "已映射");
            return field;
        }).toList();
    }

    private List<RowError> validateRows(ParsedCsv csv) {
        List<RowError> errors = new ArrayList<>();
        for (int i = 0; i < csv.rows().size(); i++) {
            Map<String, String> row = csv.rows().get(i);
            int rowNo = i + 2;
            String fundCode = value(row, "fund_code");
            if (!StringUtils.hasText(fundCode)) {
                errors.add(new RowError(rowNo, "", "fund_code", "基金代码不能为空", "填写 6 位基金代码", row.toString()));
            }
            if (!StringUtils.hasText(value(row, "fund_name"))) {
                errors.add(new RowError(rowNo, fundCode, "fund_name", "基金名称不能为空", "填写基金简称", row.toString()));
            }
            if (StringUtils.hasText(fundCode) && Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) > 1 FROM fund_info WHERE fund_code = ?", Boolean.class, fundCode))) {
                errors.add(new RowError(rowNo, fundCode, "fund_code", "基金代码在库中存在多条记录", "先清理重复基金代码", row.toString()));
            }
            validateDecimal(row, rowNo, fundCode, "fund_scale", errors);
            validateDecimal(row, rowNo, fundCode, "return_1y", errors);
            validateDecimal(row, rowNo, fundCode, "max_drawdown", errors);
            validateDecimal(row, rowNo, fundCode, "volatility", errors);
            validateDecimal(row, rowNo, fundCode, "sharpe_ratio", errors);
            validateDecimal(row, rowNo, fundCode, "total_score", errors);
            if (StringUtils.hasText(value(row, "establish_date"))) {
                try {
                    LocalDate.parse(value(row, "establish_date"));
                } catch (Exception ex) {
                    errors.add(new RowError(rowNo, fundCode, "establish_date", "日期格式无法识别", "使用 yyyy-MM-dd", row.toString()));
                }
            }
        }
        return errors;
    }

    private void validateDecimal(Map<String, String> row, int rowNo, String fundCode, String key, List<RowError> errors) {
        if (!StringUtils.hasText(value(row, key))) {
            return;
        }
        try {
            new BigDecimal(value(row, key));
        } catch (Exception ex) {
            errors.add(new RowError(rowNo, fundCode, key, "数字格式无法识别", "填写数字，例如 12.35", row.toString()));
        }
    }

    private void upsertFundBasic(Map<String, String> row) {
        Long companyId = ensureCompany(value(row, "company_name"));
        Long fundId = queryLong("SELECT fund_id FROM fund_info WHERE fund_code = ?", value(row, "fund_code"));
        if (fundId == null) {
            fundId = nextId("fund_info", "fund_id");
            jdbcTemplate.update("""
                    INSERT INTO fund_info (
                        fund_id, fund_code, fund_name, full_name, fund_type, risk_level,
                        company_id, fund_scale, establish_date, benchmark, custodian,
                        status, source, updated_time
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    fundId,
                    value(row, "fund_code"),
                    value(row, "fund_name"),
                    blankToNull(value(row, "full_name")),
                    blankToNull(value(row, "fund_type")),
                    normalizeRisk(value(row, "risk_level")),
                    companyId,
                    decimalOrNull(value(row, "fund_scale")),
                    dateOrNull(value(row, "establish_date")),
                    blankToNull(value(row, "benchmark")),
                    blankToNull(value(row, "custodian")),
                    intOrDefault(value(row, "status"), 1),
                    StringUtils.hasText(value(row, "source")) ? value(row, "source") : "ADMIN_IMPORT");
        } else {
            jdbcTemplate.update("""
                    UPDATE fund_info
                    SET fund_name = ?, full_name = ?, fund_type = ?, risk_level = ?,
                        company_id = ?, fund_scale = ?, establish_date = ?, benchmark = ?,
                        custodian = ?, status = ?, source = ?, updated_time = CURRENT_TIMESTAMP
                    WHERE fund_id = ?
                    """,
                    value(row, "fund_name"),
                    blankToNull(value(row, "full_name")),
                    blankToNull(value(row, "fund_type")),
                    normalizeRisk(value(row, "risk_level")),
                    companyId,
                    decimalOrNull(value(row, "fund_scale")),
                    dateOrNull(value(row, "establish_date")),
                    blankToNull(value(row, "benchmark")),
                    blankToNull(value(row, "custodian")),
                    intOrDefault(value(row, "status"), 1),
                    StringUtils.hasText(value(row, "source")) ? value(row, "source") : "ADMIN_IMPORT",
                    fundId);
        }
        ensureManagerRelation(fundId, value(row, "manager_name"), companyId);
        upsertMetric(fundId, row);
        upsertScore(fundId, row);
    }

    private Long ensureCompany(String companyName) {
        if (!StringUtils.hasText(companyName)) {
            return null;
        }
        Long existing = queryLong("SELECT company_id FROM fund_company WHERE company_name = ?", companyName);
        if (existing != null) {
            return existing;
        }
        long companyId = nextId("fund_company", "company_id");
        jdbcTemplate.update("""
                INSERT INTO fund_company (
                    company_id, company_name, short_name, fund_count, manager_count,
                    profile, source, updated_time
                ) VALUES (?, ?, ?, 0, 0, ?, 'ADMIN_IMPORT', CURRENT_TIMESTAMP)
                """,
                companyId,
                companyName,
                companyName.replace("基金管理有限公司", "").replace("基金有限公司", ""),
                companyName + "由管理员导入维护，后续可补充公司画像、管理规模和产品结构。");
        return companyId;
    }

    private void ensureManagerRelation(Long fundId, String managerNames, Long companyId) {
        if (!StringUtils.hasText(managerNames)) {
            return;
        }
        for (String rawName : managerNames.split("[,，、/]")) {
            String name = rawName.trim();
            if (!StringUtils.hasText(name)) {
                continue;
            }
            Long managerId = queryLong("SELECT manager_id FROM fund_manager WHERE manager_name = ?", name);
            if (managerId == null) {
                managerId = nextId("fund_manager", "manager_id");
                jdbcTemplate.update("""
                        INSERT INTO fund_manager (
                            manager_id, manager_name, company_id, experience_years,
                            manage_scale, current_fund_count, profile, updated_time
                        ) VALUES (?, ?, ?, 5, 0, 1, ?, CURRENT_TIMESTAMP)
                        """,
                        managerId,
                        name,
                        companyId,
                        name + "由管理员导入维护，后续可补充基金经理画像和历史业绩。");
            }
            Integer relationExists = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM fund_manager_relation WHERE fund_id = ? AND manager_id = ?
                    """, Integer.class, fundId, managerId);
            if (relationExists == null || relationExists == 0) {
                jdbcTemplate.update("""
                        INSERT INTO fund_manager_relation (
                            relation_id, fund_id, manager_id, start_date, is_current, role_name
                        ) VALUES ((SELECT COALESCE(MAX(relation_id), 0) + 1 FROM fund_manager_relation),
                                  ?, ?, CURRENT_DATE, 1, '基金经理')
                        """, fundId, managerId);
            }
        }
    }

    private void upsertMetric(Long fundId, Map<String, String> row) {
        if (!StringUtils.hasText(value(row, "return_1y"))
                && !StringUtils.hasText(value(row, "max_drawdown"))
                && !StringUtils.hasText(value(row, "volatility"))
                && !StringUtils.hasText(value(row, "sharpe_ratio"))) {
            return;
        }
        jdbcTemplate.update("DELETE FROM fund_performance_metric WHERE fund_id = ? AND period_code = '1Y' AND stat_date = CURRENT_DATE", fundId);
        jdbcTemplate.update("""
                INSERT INTO fund_performance_metric (
                    metric_id, fund_id, stat_date, period_code, return_rate,
                    annual_return, volatility, max_drawdown, sharpe_ratio
                ) VALUES ((SELECT COALESCE(MAX(metric_id), 0) + 1 FROM fund_performance_metric),
                          ?, CURRENT_DATE, '1Y', ?, ?, ?, ?, ?)
                """,
                fundId,
                decimalOrNull(value(row, "return_1y")),
                decimalOrNull(value(row, "return_1y")),
                decimalOrNull(value(row, "volatility")),
                decimalOrNull(value(row, "max_drawdown")),
                decimalOrNull(value(row, "sharpe_ratio")));
    }

    private void upsertScore(Long fundId, Map<String, String> row) {
        if (!StringUtils.hasText(value(row, "total_score"))) {
            return;
        }
        BigDecimal totalScore = decimalOrNull(value(row, "total_score"));
        String level = StringUtils.hasText(value(row, "recommend_level")) ? value(row, "recommend_level")
                : (totalScore.doubleValue() >= 85 ? "RECOMMEND" : totalScore.doubleValue() >= 70 ? "WATCH" : "NEUTRAL");
        jdbcTemplate.update("DELETE FROM fund_score WHERE fund_id = ? AND score_date = CURRENT_DATE", fundId);
        jdbcTemplate.update("""
                INSERT INTO fund_score (
                    score_id, fund_id, score_date, total_score, recommend_level,
                    explain_text, created_time
                ) VALUES ((SELECT COALESCE(MAX(score_id), 0) + 1 FROM fund_score),
                          ?, CURRENT_DATE, ?, ?, '管理员导入评分', CURRENT_TIMESTAMP)
                """, fundId, totalScore, level);
    }

    private void insertImportError(long batchId, RowError error) {
        jdbcTemplate.update("""
                INSERT INTO data_import_error (
                    error_id, batch_id, row_no, fund_code, error_field,
                    error_reason, suggestion, raw_data, status, created_time
                ) VALUES ((SELECT COALESCE(MAX(error_id), 0) + 1 FROM data_import_error),
                          ?, ?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)
                """,
                batchId,
                error.rowNo(),
                error.fundCode(),
                error.errorField(),
                error.errorReason(),
                error.suggestion(),
                error.rawData());
    }

    private long nextId(String tableName, String idColumn) {
        Long value = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(" + idColumn + "), 0) + 1 FROM " + tableName, Long.class);
        return value == null ? 1L : value;
    }

    private Long queryLong(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, args);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String normalizeHeader(String header) {
        String key = stripBom(header).trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "基金代码", "fundcode", "code" -> "fund_code";
            case "基金名称", "基金简称", "fundname", "name" -> "fund_name";
            case "基金全称", "fullname" -> "full_name";
            case "基金类型", "type" -> "fund_type";
            case "风险等级", "risk" -> "risk_level";
            case "基金公司", "公司", "company" -> "company_name";
            case "基金经理", "manager" -> "manager_name";
            case "基金规模", "规模", "scale" -> "fund_scale";
            case "成立日期", "成立日", "date" -> "establish_date";
            case "近一年收益", "近1年收益", "return1y" -> "return_1y";
            case "最大回撤", "drawdown" -> "max_drawdown";
            case "波动率" -> "volatility";
            case "夏普", "夏普比率" -> "sharpe_ratio";
            case "综合评分", "评分", "score" -> "total_score";
            case "推荐等级", "recommend" -> "recommend_level";
            case "业绩基准" -> "benchmark";
            case "托管人" -> "custodian";
            case "数据来源" -> "source";
            case "状态", "是否显示" -> "status";
            default -> key;
        };
    }

    private String systemFieldLabel(String key) {
        return switch (key) {
            case "fund_code" -> "基金代码";
            case "fund_name" -> "基金名称";
            case "full_name" -> "基金全称";
            case "fund_type" -> "基金类型";
            case "risk_level" -> "风险等级";
            case "company_name" -> "基金公司";
            case "manager_name" -> "基金经理";
            case "fund_scale" -> "基金规模";
            case "establish_date" -> "成立日期";
            case "return_1y" -> "近一年收益";
            case "max_drawdown" -> "最大回撤";
            case "volatility" -> "波动率";
            case "sharpe_ratio" -> "夏普比率";
            case "total_score" -> "综合评分";
            case "recommend_level" -> "推荐等级";
            case "benchmark" -> "业绩基准";
            case "custodian" -> "托管人";
            case "source" -> "数据来源";
            case "status" -> "是否显示";
            default -> "未识别";
        };
    }

    private String fieldType(String key) {
        if (List.of("fund_scale", "return_1y", "max_drawdown", "volatility", "sharpe_ratio", "total_score").contains(key)) {
            return "Number";
        }
        if ("establish_date".equals(key)) {
            return "Date";
        }
        if ("status".equals(key)) {
            return "Boolean";
        }
        return "String";
    }

    private String normalizeRisk(String value) {
        if (!StringUtils.hasText(value)) {
            return "MEDIUM";
        }
        String risk = value.trim().toUpperCase(Locale.ROOT);
        if (risk.contains("低") || "LOW".equals(risk)) {
            return "LOW";
        }
        if (risk.contains("中低") || "MEDIUM_LOW".equals(risk)) {
            return "MEDIUM_LOW";
        }
        if (risk.contains("中高") || "MEDIUM_HIGH".equals(risk)) {
            return "MEDIUM_HIGH";
        }
        if (risk.contains("高") || "HIGH".equals(risk)) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private String value(Map<String, String> row, String key) {
        return row.getOrDefault(key, "").trim();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private LocalDate dateOrNull(String value) {
        return StringUtils.hasText(value) ? LocalDate.parse(value) : null;
    }

    private BigDecimal decimalOrNull(String value) {
        return StringUtils.hasText(value) ? new BigDecimal(value) : null;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString());
    }

    private int intOrDefault(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        if ("显示".equals(value) || "启用".equals(value) || "true".equalsIgnoreCase(value)) {
            return 1;
        }
        if ("隐藏".equals(value) || "停用".equals(value) || "false".equalsIgnoreCase(value)) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    private boolean boolValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = Objects.toString(value, "false");
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "启用".equals(text) || "显示".equals(text);
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(Math.round(value * 100.0) / 100.0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String stripBom(String value) {
        return value == null ? "" : value.replace("\uFEFF", "");
    }

    private String toJsonLike(Object value) {
        return Objects.toString(value, "");
    }

    private record ParsedCsv(List<String> headers, List<Map<String, String>> rows) {
    }

    private record RowError(int rowNo, String fundCode, String errorField, String errorReason, String suggestion,
                            String rawData) {
    }
}
