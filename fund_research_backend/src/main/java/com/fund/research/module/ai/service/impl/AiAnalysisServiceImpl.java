package com.fund.research.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.common.PageResult;
import com.fund.research.config.DifyProperties;
import com.fund.research.module.ai.dto.AiAnalyzeRequestDTO;
import com.fund.research.module.ai.entity.AiAnalysisRecord;
import com.fund.research.module.ai.mapper.AiAnalysisMapper;
import com.fund.research.module.ai.mapper.RecommendMapper;
import com.fund.research.module.ai.service.AiAnalysisService;
import com.fund.research.module.ai.vo.AiAnalysisRecordVO;
import com.fund.research.module.ai.vo.RecommendFundVO;
import com.fund.research.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final AiAnalysisMapper aiAnalysisMapper;
    private final RecommendMapper recommendMapper;
    private final UserMapper userMapper;
    private final DifyProperties difyProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplateBuilder restTemplateBuilder;
    private static final List<String> RECOMMEND_OUTPUT_KEYS = List.of(
            "result1",
            "recommendResult",
            "recommendation",
            "fundRecommend",
            "text",
            "answer",
            "result"
    );
    private static final List<String> COMPARE_OUTPUT_KEYS = List.of(
            "result2",
            "compareResult",
            "comparison",
            "fundCompare",
            "text",
            "answer",
            "result"
    );
    private static final List<String> DEFAULT_OUTPUT_KEYS = List.of(
            "text",
            "answer",
            "result",
            "output",
            "result1",
            "result2"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAnalysisRecordVO analyze(AiAnalyzeRequestDTO request) {
        if (request.getUserId() != null && userMapper.selectById(request.getUserId()) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "User does not exist");
        }

        AiAnalysisRecord record = new AiAnalysisRecord();
        record.setUserId(request.getUserId());
        record.setBusinessType(defaultText(request.getBusinessType(), "FUND"));
        record.setBusinessId(request.getBusinessId());
        record.setDifyWorkflowId(trimToNull(request.getDifyWorkflowId()));
        record.setRequestPayload(toJson(buildRequestPayload(request)));

        AiAnalysisRecordVO cached = findCachedAnalysis(request);
        if (cached != null && !containsEllipsis(cached.getResponseText())) {
            record.setStatus("SUCCESS");
            record.setResponseText(normalizeDifyText(cached.getResponseText()));
            record.setDifyWorkflowId(defaultText(record.getDifyWorkflowId(), "cache:" + cached.getAnalysisId()));
            aiAnalysisMapper.insert(record);
            return getRecord(record.getAnalysisId());
        }

        if (!StringUtils.hasText(difyProperties.getApiKey())) {
            record.setStatus("SKIPPED");
            record.setResponseText("Dify API key is not configured. Local fallback recorded the analysis request.");
            aiAnalysisMapper.insert(record);
            return getRecord(record.getAnalysisId());
        }

        try {
            String response = callDify(request);
            record.setStatus("SUCCESS");
            record.setResponseText(extractResponseText(response, request.getBusinessType()));
        } catch (RestClientException | JsonProcessingException ex) {
            record.setStatus("FAILED");
            record.setErrorMessage(ex.getMessage());
            record.setResponseText("Dify workflow call failed. Check api-key, workflow configuration, and network.");
        }

        aiAnalysisMapper.insert(record);
        return getRecord(record.getAnalysisId());
    }

    @Override
    public PageResult<AiAnalysisRecordVO> pageRecords(Long userId, String businessType, Integer pageNo, Integer pageSize) {
        Page<AiAnalysisRecordVO> page = Page.of(normalizePage(pageNo), normalizeSize(pageSize));
        aiAnalysisMapper.selectAnalysisPage(page, userId, trimToNull(businessType));
        return PageResult.of(page);
    }

    @Override
    public AiAnalysisRecordVO getRecord(Long analysisId) {
        if (analysisId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "analysisId must not be null");
        }
        AiAnalysisRecordVO record = aiAnalysisMapper.selectAnalysisById(analysisId);
        if (record == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "AI analysis record does not exist");
        }
        return record;
    }

    @Override
    public PageResult<RecommendFundVO> pageRecommendFunds(Long userId, String scene, Integer pageNo, Integer pageSize) {
        Page<RecommendFundVO> page = Page.of(normalizePage(pageNo), normalizeSize(pageSize));
        recommendMapper.selectRecommendFundPage(page, userId, trimToNull(scene));
        if (userId != null && page.getRecords().isEmpty()) {
            recommendMapper.selectRecommendFundPage(page, null, trimToNull(scene));
        }
        enrichRecommendTags(page.getRecords());
        return PageResult.of(page);
    }

    @Override
    public List<RecommendFundVO> listRecommendFunds(Long userId, String scene, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 6 : Math.min(limit, 50);
        List<RecommendFundVO> records = recommendMapper.selectRecommendFunds(userId, trimToNull(scene), safeLimit);
        if (userId != null && (records == null || records.isEmpty())) {
            records = recommendMapper.selectRecommendFunds(null, trimToNull(scene), safeLimit);
        }
        enrichRecommendTags(records);
        return records == null ? Collections.emptyList() : records;
    }

    @Override
    public RecommendFundVO getLatestRecommendFund(Long userId, Long fundId) {
        if (fundId == null) {
            return null;
        }
        RecommendFundVO vo = recommendMapper.selectLatestRecommendFund(userId, fundId);
        enrichRecommendTags(vo == null ? null : Collections.singletonList(vo));
        return vo;
    }

    private void enrichRecommendTags(List<RecommendFundVO> records) {
        if (records == null) {
            return;
        }
        for (RecommendFundVO vo : records) {
            if (vo == null) {
                continue;
            }
            String raw = vo.getTags();
            if (StringUtils.hasText(raw)) {
                List<String> tags = new ArrayList<>();
                for (String token : raw.split(",")) {
                    if (token == null) {
                        continue;
                    }
                    String trimmed = token.trim();
                    if (!trimmed.isEmpty()) {
                        tags.add(trimmed);
                    }
                }
                vo.setTagList(tags);
            } else {
                vo.setTagList(Collections.emptyList());
            }
        }
    }

    private AiAnalysisRecordVO findCachedAnalysis(AiAnalyzeRequestDTO request) {
        String businessType = defaultText(request.getBusinessType(), "FUND");
        if (request.getBusinessId() == null || !isCacheableBusinessType(businessType)) {
            return null;
        }
        AiAnalysisRecordVO userCached = aiAnalysisMapper.selectLatestSuccessfulAnalysis(
                request.getUserId(),
                businessType,
                request.getBusinessId()
        );
        if (userCached != null && isUsableCachedResponse(userCached.getResponseText())) {
            return userCached;
        }
        AiAnalysisRecordVO sharedCached = aiAnalysisMapper.selectLatestSuccessfulAnalysis(null, businessType, request.getBusinessId());
        return sharedCached != null && isUsableCachedResponse(sharedCached.getResponseText()) ? sharedCached : null;
    }

    private boolean isCacheableBusinessType(String businessType) {
        String normalized = defaultText(businessType, "").toUpperCase();
        return "FUND_RECOMMEND".equals(normalized) || "FUND_COMPARE".equals(normalized);
    }

    private boolean isUsableCachedResponse(String responseText) {
        return StringUtils.hasText(responseText) && !responseText.contains("…");
    }

    private boolean containsEllipsis(String text) {
        return StringUtils.hasText(text) && (text.contains("…") || text.contains("..."));
    }

    private String callDify(AiAnalyzeRequestDTO request) {
        Map<String, Object> inputs = new HashMap<>();
        if (request.getInputs() != null) {
            inputs.putAll(request.getInputs());
        }
        inputs.putIfAbsent("query", request.getQuery());
        inputs.putIfAbsent("businessType", defaultText(request.getBusinessType(), "FUND"));
        if (request.getBusinessId() != null) {
            inputs.putIfAbsent("businessId", request.getBusinessId());
        }
        inputs.putIfAbsent("outputRequirement", outputRequirementFor(defaultText(request.getBusinessType(), "FUND")));

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", request.getUserId() == null ? "fund-research-anonymous" : "fund-research-" + request.getUserId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(difyProperties.getApiKey());

        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMillis()))
                .setReadTimeout(Duration.ofMillis(timeoutMillis()))
                .build();
        return restTemplate.postForObject(difyWorkflowUrl(), new HttpEntity<>(body, headers), String.class);
    }

    private Map<String, Object> buildRequestPayload(AiAnalyzeRequestDTO request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", request.getUserId());
        payload.put("businessType", defaultText(request.getBusinessType(), "FUND"));
        payload.put("businessId", request.getBusinessId());
        payload.put("difyWorkflowId", request.getDifyWorkflowId());
        payload.put("query", request.getQuery());
        payload.put("inputs", request.getInputs());
        return payload;
    }

    private String outputRequirementFor(String businessType) {
        String normalized = defaultText(businessType, "").toUpperCase();
        if ("FUND_COMPARE".equals(normalized) || "COMPARE".equals(normalized)) {
            return "请输出完整基金对比结论，不要输出思考过程或 Markdown。不要使用省略号，内容可包含收益、回撤、波动率、风险等级和适用人群。";
        }
        return "请只输出严格 JSON，不要输出思考过程、Markdown 或额外文字。JSON 字段只包含 reason。reason 输出完整推荐理由，不要使用省略号。";
    }

    String extractResponseText(String response, String businessType) throws JsonProcessingException {
        if (!StringUtils.hasText(response)) {
            return "Empty Dify response.";
        }
        JsonNode root = objectMapper.readTree(response);
        JsonNode outputs = root.path("data").path("outputs");

        String selectedOutput = findOutputText(outputs, outputKeysForBusinessType(businessType));
        if (StringUtils.hasText(selectedOutput)) {
            return normalizeDifyText(selectedOutput);
        }

        String firstTextOutput = findFirstTextOutput(outputs);
        if (StringUtils.hasText(firstTextOutput)) {
            return normalizeDifyText(firstTextOutput);
        }

        if (!outputs.isMissingNode() && !outputs.isNull() && outputs.size() > 0) {
            return normalizeDifyText(outputs.toString());
        }
        if (root.hasNonNull("answer")) {
            return normalizeDifyText(root.path("answer").asText());
        }
        return normalizeDifyText(response);
    }

    private List<String> outputKeysForBusinessType(String businessType) {
        String normalized = defaultText(businessType, "").toUpperCase();
        if ("FUND_RECOMMEND".equals(normalized) || "RECOMMEND".equals(normalized)) {
            return RECOMMEND_OUTPUT_KEYS;
        }
        if ("FUND_COMPARE".equals(normalized) || "COMPARE".equals(normalized)) {
            return COMPARE_OUTPUT_KEYS;
        }
        return DEFAULT_OUTPUT_KEYS;
    }

    private String findOutputText(JsonNode outputs, List<String> keys) {
        if (outputs == null || outputs.isMissingNode() || outputs.isNull()) {
            return null;
        }
        for (String key : keys) {
            if (outputs.hasNonNull(key)) {
                String text = outputNodeToText(outputs.path(key));
                if (StringUtils.hasText(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private String findFirstTextOutput(JsonNode outputs) {
        if (outputs == null || !outputs.isObject()) {
            return null;
        }
        var fields = outputs.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String text = outputNodeToText(field.getValue());
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private String outputNodeToText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isValueNode()) {
            return node.asText();
        }
        return node.toString();
    }

    private String normalizeDifyText(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String cleaned = text.replaceAll("(?is)<think>.*?</think>", "").trim();
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            if (node.isObject()) {
                String readable = objectToReadableText(node);
                if (StringUtils.hasText(readable)) {
                    return readable;
                }
                String cardText = compactCardExplanation(node);
                if (StringUtils.hasText(cardText)) {
                    return cardText;
                }
                String extracted = findOutputText(node, List.of(
                        "explanation",
                        "reason",
                        "summary",
                        "text",
                        "answer",
                        "result",
                        "result1",
                        "result2"
                ));
                if (StringUtils.hasText(extracted)) {
                    return extracted;
                }
            }
        } catch (JsonProcessingException ignored) {
            // Dify may return plain text; keep the cleaned text in that case.
        }
        return cleaned;
    }

    private String compactCardExplanation(JsonNode node) {
        String reason = textField(node, "reason");
        String riskWarning = textField(node, "riskWarning");
        String suitableUser = textField(node, "suitableUser");
        if (!StringUtils.hasText(reason) && !StringUtils.hasText(riskWarning) && !StringUtils.hasText(suitableUser)) {
            return null;
        }
        if (StringUtils.hasText(reason)) {
            return reason;
        }
        if (StringUtils.hasText(riskWarning)) {
            return riskWarning;
        }
        return suitableUser;
    }

    private String objectToReadableText(JsonNode node) {
        List<String> parts = new ArrayList<>();
        addReadableField(parts, node, "summary", "总结");
        addReadableField(parts, node, "bestForStableUser", "稳健型用户");
        addReadableField(parts, node, "bestForAggressiveUser", "进取型用户");
        addReadableField(parts, node, "riskWarning", "风险提示");
        addReadableField(parts, node, "suitableUser", "适合人群");
        addReadableField(parts, node, "conclusion", "结论");
        addReadableField(parts, node, "disclaimer", "说明");
        return String.join("\n\n", parts);
    }

    private void addReadableField(List<String> parts, JsonNode node, String fieldName, String label) {
        String text = textField(node, fieldName);
        if (StringUtils.hasText(text)) {
            parts.add(label + "：" + text.trim());
        }
    }

    private String textField(JsonNode node, String fieldName) {
        if (node.hasNonNull(fieldName)) {
            return outputNodeToText(node.path(fieldName));
        }
        return null;
    }

    private String limitText(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "…";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "Unable to serialize AI request");
        }
    }

    private String difyWorkflowUrl() {
        String baseUrl = defaultText(difyProperties.getBaseUrl(), "https://api.dify.ai/v1");
        return baseUrl.replaceAll("/+$", "") + "/workflows/run";
    }

    private int timeoutMillis() {
        return difyProperties.getTimeoutMillis() == null ? 30000 : difyProperties.getTimeoutMillis();
    }

    private long normalizePage(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1L : pageNo.longValue();
    }

    private long normalizeSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
