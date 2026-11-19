
package com.apiguardian.controller;

import java.util.Map;
import java.util.List;
import java.util.Map;
import com.apiguardian.service.GitHubDiffService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apiguardian.service.GitHubService;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubService gitHubService;
    private final GitHubDiffService gitHubDiffService;
    public GitHubController(
            GitHubService gitHubService,
            GitHubDiffService gitHubDiffService) {

        this.gitHubService = gitHubService;
        this.gitHubDiffService = gitHubDiffService;
    }

    @GetMapping("/repository")
    public Map<?, ?> getRepository(
            @RequestParam String owner,
            @RequestParam String repo) {

        return gitHubService.getRepositoryInfo(owner, repo);
    }

    @GetMapping("/commits")
    public Object getRepositoryCommits(
    		@RequestParam String owner,
    		@RequestParam String repo) {

    	return gitHubService.getRepositoryCommits(owner, repo);
}

@GetMapping("/compare")
public Object compareCommits(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam String base,
        @RequestParam String head) {

    return gitHubService.compareCommits(
            owner, repo, base, head);
}
@GetMapping("/changed-java-files")
public List<Map<String, Object>> getChangedJavaFiles(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam String base,
        @RequestParam String head) {

    Map<String, Object> comparison =
            (Map<String, Object>) gitHubService.compareCommits(
                    owner, repo, base, head);

    return gitHubDiffService.extractChangedFiles(comparison);
}
}
