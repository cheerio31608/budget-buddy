package com.finance.budget_buddy.service;

import com.finance.budget_buddy.exception.BusinessException;
import com.finance.budget_buddy.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";

    private MockRestServiceServer server;
    private GeminiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiClient(builder.build(), "test-api-key", API_URL);
    }

    @Test
    void returnsGeneratedText() {
        server.expect(requestTo(API_URL))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"분석 결과"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.generateContent("prompt")).isEqualTo("분석 결과");
        server.verify();
    }

    @Test
    void mapsMissingModelToSpecificError() {
        server.expect(requestTo(API_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.generateContent("prompt"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.AI_MODEL_UNAVAILABLE));
    }

    @Test
    void mapsProviderQuotaToSpecificError() {
        server.expect(requestTo(API_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.generateContent("prompt"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.AI_PROVIDER_RATE_LIMITED));
    }
}
