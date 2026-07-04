package com.fund.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.research.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "dify.api-key=")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class BackendFeatureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long userId;
    private List<Long> fundIds;
    private String token;

    @BeforeEach
    void prepareData() {
        userId = ensureUser("integration_user");
        fundIds = jdbcTemplate.queryForList(
                "SELECT fund_id FROM fund_info ORDER BY fund_id ASC LIMIT 2",
                Long.class
        );
        assertThat(fundIds).hasSize(2);

        jdbcTemplate.update("DELETE FROM ai_analysis_record WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM fund_compare_record WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM fund_portfolio WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_favorite WHERE user_id = ?", userId);
        token = jwtTokenProvider.generateToken(userId, "integration_user", "USER");
    }

    @Test
    void portfolioWorkflowCreatesUpdatesListsDetailsAndDeletes() throws Exception {
        JsonNode created = postJson("/api/v1/portfolios", Map.of(
                "userId", userId,
                "portfolioName", "integration portfolio",
                "portfolioType", "RESEARCH_POOL",
                "sourceDimension", "FUND",
                "description", "created by integration test",
                "fundIds", fundIds
        ));
        Long portfolioId = created.path("data").path("portfolioId").asLong();
        assertThat(portfolioId).isPositive();
        assertThat(created.path("data").path("fundCount").asInt()).isEqualTo(2);

        JsonNode list = getJson("/api/v1/portfolios?userId=" + userId);
        assertThat(list.path("data").path("total").asLong()).isEqualTo(1);

        JsonNode updated = putJson("/api/v1/portfolios/" + portfolioId, Map.of(
                "portfolioName", "integration portfolio updated",
                "description", "updated"
        ));
        assertThat(updated.path("data").path("portfolioName").asText()).isEqualTo("integration portfolio updated");

        JsonNode detail = getJson("/api/v1/portfolios/" + portfolioId);
        assertThat(detail.path("data").path("funds")).hasSize(2);

        JsonNode removedFund = deleteJson("/api/v1/portfolios/" + portfolioId + "/funds/" + fundIds.get(1));
        assertThat(removedFund.path("data").path("fundCount").asInt()).isEqualTo(1);

        JsonNode deleted = deleteJson("/api/v1/portfolios/" + portfolioId);
        assertThat(deleted.path("data").asBoolean()).isTrue();
    }

    @Test
    void favoriteWorkflowAddsListsAndRemovesFavoriteFund() throws Exception {
        JsonNode created = postJson("/api/v1/favorites", Map.of(
                "userId", userId,
                "fundId", fundIds.get(0),
                "favoriteGroup", "DEFAULT",
                "remark", "watch"
        ));
        Long favoriteId = created.path("data").path("favoriteId").asLong();
        assertThat(favoriteId).isPositive();

        JsonNode list = getJson("/api/v1/favorites?userId=" + userId + "&favoriteGroup=DEFAULT");
        assertThat(list.path("data").path("total").asLong()).isEqualTo(1);
        assertThat(list.path("data").path("records").get(0).path("fundId").asLong()).isEqualTo(fundIds.get(0));

        JsonNode deleted = deleteJson("/api/v1/favorites/" + favoriteId + "?userId=" + userId);
        assertThat(deleted.path("data").asBoolean()).isTrue();

        JsonNode emptyList = getJson("/api/v1/favorites?userId=" + userId + "&favoriteGroup=DEFAULT");
        assertThat(emptyList.path("data").path("total").asLong()).isZero();
    }

    @Test
    void compareAndAiWorkflowStoresRecords() throws Exception {
        JsonNode createdCompare = postJson("/api/v1/compares", Map.of(
                "userId", userId,
                "fundIds", fundIds,
                "compareDimension", "SCORE,RETURN,RISK"
        ));
        Long compareId = createdCompare.path("data").path("compareId").asLong();
        assertThat(compareId).isPositive();
        assertThat(createdCompare.path("data").path("funds")).hasSize(2);
        assertThat(createdCompare.path("data").path("resultSummary").asText()).isNotBlank();

        JsonNode compareDetail = getJson("/api/v1/compares/" + compareId);
        assertThat(compareDetail.path("data").path("funds")).hasSize(2);

        JsonNode aiRecord = postJson("/api/v1/ai/analyze", Map.of(
                "userId", userId,
                "businessType", "COMPARE",
                "businessId", compareId,
                "query", "summarize this comparison",
                "inputs", Map.of("compareId", compareId)
        ));
        assertThat(aiRecord.path("data").path("analysisId").asLong()).isPositive();
        assertThat(aiRecord.path("data").path("status").asText()).isEqualTo("SKIPPED");
        assertThat(aiRecord.path("data").path("responseText").asText()).contains("Dify");

        JsonNode aiList = getJson("/api/v1/ai/records?userId=" + userId);
        assertThat(aiList.path("data").path("total").asLong()).isEqualTo(1);
    }

    private Long ensureUser(String username) {
        List<Long> existing = jdbcTemplate.queryForList(
                "SELECT user_id FROM fund_user WHERE username = ?",
                Long.class,
                username
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        jdbcTemplate.update(
                "INSERT INTO fund_user(username, password_hash, role_code, risk_preference, status) VALUES (?, ?, ?, ?, ?)",
                username,
                "integration-test",
                "USER",
                "BALANCED",
                1
        );
        return jdbcTemplate.queryForObject(
                "SELECT user_id FROM fund_user WHERE username = ?",
                Long.class,
                username
        );
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withAuth(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requestBuilder) {
        return requestBuilder.header("Authorization", "Bearer " + token);
    }

    private JsonNode getJson(String path) throws Exception {
        String content = mockMvc.perform(withAuth(get(path)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return assertApiSuccess(content);
    }

    private JsonNode postJson(String path, Object body) throws Exception {
        String content = mockMvc.perform(withAuth(post(path))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return assertApiSuccess(content);
    }

    private JsonNode putJson(String path, Object body) throws Exception {
        String content = mockMvc.perform(withAuth(put(path))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return assertApiSuccess(content);
    }

    private JsonNode deleteJson(String path) throws Exception {
        String content = mockMvc.perform(withAuth(delete(path)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return assertApiSuccess(content);
    }

    private JsonNode assertApiSuccess(String content) throws Exception {
        JsonNode node = objectMapper.readTree(content);
        assertThat(node.path("code").asInt()).isEqualTo(0);
        return node;
    }
}
