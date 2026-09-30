package com.finance.budget_buddy.service;

import com.finance.budget_buddy.dto.gemini.GeminiRequest;
import com.finance.budget_buddy.dto.gemini.GeminiResponse;
import com.finance.budget_buddy.exception.BusinessException;
import com.finance.budget_buddy.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String apiUrl;

    @Autowired
    public GeminiClient(@Value("${gemini.api.key}") String apiKey,
                        @Value("${gemini.api.url}") String apiUrl) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        factory.setReadTimeout(java.time.Duration.ofSeconds(40));
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    GeminiClient(RestClient restClient, String apiKey, String apiUrl) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    /**
     * Gemini API에 텍스트 프롬프트를 전송하고 응답을 받습니다.
     */
    public String generateContent(String prompt) {
        ensureConfigured();
        GeminiRequest request = GeminiRequest.fromText(prompt);
        try {
            GeminiResponse response = restClient.post()
                    .uri(apiUrl)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null || response.getText().isBlank()) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return response.getText();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            log.warn("Gemini request rejected with provider status={}", status);
            throw new BusinessException(mapProviderError(status));
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private ErrorCode mapProviderError(int status) {
        return switch (status) {
            case 401, 403 -> ErrorCode.AI_CREDENTIALS_REJECTED;
            case 404 -> ErrorCode.AI_MODEL_UNAVAILABLE;
            case 429 -> ErrorCode.AI_PROVIDER_RATE_LIMITED;
            default -> ErrorCode.AI_SERVICE_UNAVAILABLE;
        };
    }

    /** Checks local configuration before an attempt is charged against the AI quota. */
    public void ensureConfigured() {
        if (apiKey == null || apiKey.isBlank() || "dummy-api-key".equals(apiKey)) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }
    }
}
