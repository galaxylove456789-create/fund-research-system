package com.fund.research.module.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class AiAnalysisServiceImplTest {

    private final AiAnalysisServiceImpl service = new AiAnalysisServiceImpl(
            null,
            null,
            null,
            null,
            new ObjectMapper(),
            new RestTemplateBuilder()
    );

    @Test
    void extractResponseTextUsesRecommendBranchOutput() throws Exception {
        String response = """
                {
                  "data": {
                    "outputs": {
                      "result1": "recommend explanation",
                      "result2": "compare summary"
                    }
                  }
                }
                """;

        String text = service.extractResponseText(response, "FUND_RECOMMEND");

        assertThat(text).isEqualTo("recommend explanation");
    }

    @Test
    void extractResponseTextUsesCompareBranchOutput() throws Exception {
        String response = """
                {
                  "data": {
                    "outputs": {
                      "result1": "recommend explanation",
                      "result2": "compare summary"
                    }
                  }
                }
                """;

        String text = service.extractResponseText(response, "FUND_COMPARE");

        assertThat(text).isEqualTo("compare summary");
    }

    @Test
    void extractResponseTextKeepsTextFallbackForExistingWorkflows() throws Exception {
        String response = """
                {
                  "data": {
                    "outputs": {
                      "text": "legacy text output"
                    }
                  }
                }
                """;

        String text = service.extractResponseText(response, "FUND_RECOMMEND");

        assertThat(text).isEqualTo("legacy text output");
    }

    @Test
    void extractResponseTextRemovesThinkTagAndUnwrapsJsonText() throws Exception {
        String response = """
                {
                  "data": {
                    "outputs": {
                      "result1": "<think>internal reasoning</think>{\\"explanation\\":\\"clean explanation\\"}"
                    }
                  }
                }
                """;

        String text = service.extractResponseText(response, "FUND_RECOMMEND");

        assertThat(text).isEqualTo("clean explanation");
    }

    @Test
    void extractResponseTextFormatsFullCompareJsonWithoutTruncation() throws Exception {
        String response = """
                {
                  "data": {
                    "outputs": {
                      "result2": "<think>internal reasoning</think>{\\"summary\\":\\"收益和回撤差异明显\\",\\"riskWarning\\":\\"高收益基金回撤更大\\",\\"conclusion\\":\\"应结合风险偏好选择\\"}"
                    }
                  }
                }
                """;

        String text = service.extractResponseText(response, "FUND_COMPARE");

        assertThat(text).contains("总结：收益和回撤差异明显");
        assertThat(text).contains("风险提示：高收益基金回撤更大");
        assertThat(text).contains("结论：应结合风险偏好选择");
        assertThat(text).doesNotContain("…");
    }
}
