package com.apiguardian.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OllamaService {

    private final RestClient restClient;

    public OllamaService() {
        this.restClient = RestClient.create("http://localhost:11434");
    }

    public String generateResponse(String prompt) {

        Map<String, Object> request = Map.of(
                "model", "qwen2.5:3b",
                "prompt", prompt,
                "stream", false
        );

        Map<?, ?> response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .body(Map.class);

        return String.valueOf(response.get("response"));
    }

    public String testOllama() {
        return generateResponse(
                "Explain what a breaking API change is in simple words."
        );
    }
}