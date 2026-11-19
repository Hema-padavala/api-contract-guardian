
package com.apiguardian.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GitHubService {
	// Testing GitHub Java file detection
    private final RestClient restClient;

    public GitHubService() {
        this.restClient = RestClient.create("https://api.github.com");
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

}
