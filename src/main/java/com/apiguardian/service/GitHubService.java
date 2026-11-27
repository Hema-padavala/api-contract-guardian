
package com.apiguardian.service;

import java.util.Map;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GitHubService {

    private final RestClient restClient;

    public GitHubService(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://api.github.com")
                .build();
    }

    public Map<?, ?> getRepositoryInfo(String owner, String repo) {

        return restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(Map.class);
    }

    public Object getRepositoryCommits(String owner, String repo) {

        return restClient.get()
                .uri("/repos/{owner}/{repo}/commits", owner, repo)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(Object.class);
    }

    public Object compareCommits(
            String owner, String repo,
            String base, String head) {

        return restClient.get()
                .uri("/repos/{owner}/{repo}/compare/{base}...{head}",
                        owner, repo, base, head)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(Object.class);
    }

    public List<Map<String, Object>> getLatestCommit(
            String owner, String repo) {

        return (List<Map<String, Object>>) restClient.get()
                .uri("/repos/{owner}/{repo}/commits?per_page=1",
                        owner, repo)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(List.class);
    }

    public String getFileContent(
            String owner, String repo, String path, String ref) {

        Map<?, ?> response = restClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}?ref={ref}",
                        owner, repo, path, ref)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(Map.class);

        String content = (String) response.get("content");

        byte[] decoded = Base64.getMimeDecoder().decode(content);

        return new String(decoded, StandardCharsets.UTF_8);
    }
}
