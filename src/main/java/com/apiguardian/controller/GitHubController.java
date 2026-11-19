
package com.apiguardian.controller;

import java.util.Map;
import java.util.List;

import com.apiguardian.service.GitHubDiffService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apiguardian.service.GitHubService;
import com.apiguardian.service.GitHubDiffService;
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
@GetMapping("/java-patches")
public List<String> getJavaPatches(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam String base,
        @RequestParam String head) {

    Map<String, Object> comparison =
            (Map<String, Object>) gitHubService.compareCommits(
                    owner, repo, base, head);

    return gitHubDiffService.extractJavaPatches(comparison);
}
@GetMapping("/analyze-java-changes")
public List<String> analyzeJavaChanges(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam String base,
        @RequestParam String head) {

    Map<String, Object> comparison =
            (Map<String, Object>) gitHubService.compareCommits(
                    owner, repo, base, head);

    return gitHubDiffService.explainJavaPatches(comparison);
}
@GetMapping("/files-by-status")
public Map<String, List<Map<String, Object>>> getFilesByStatus(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam String base,
        @RequestParam String head) {

    Map<String, Object> comparison =
            (Map<String, Object>) gitHubService.compareCommits(
                    owner, repo, base, head);

    return gitHubDiffService.groupFilesByStatus(comparison);
}
@GetMapping("/change-report")
public Map<String, Object> getChangeReport(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam String base,
        @RequestParam String head) {

    Map<String, Object> comparison =
            (Map<String, Object>) gitHubService.compareCommits(
                    owner, repo, base, head);

    return gitHubDiffService.generateChangeReport(comparison);
}
@GetMapping("/latest-commit")
public List<Map<String, Object>> getLatestCommit(
        @RequestParam String owner,
        @RequestParam String repo) {

    return gitHubService.getLatestCommit(owner, repo);
}
@GetMapping("/analyze-latest-commit")
public Map<String, Object> analyzeLatestCommit(
        @RequestParam String owner,
        @RequestParam String repo) {

    List<Map<String, Object>> commits =
            gitHubService.getLatestCommit(owner, repo);

    String latestCommit =
            (String) commits.get(0).get("sha");

    Map<String, Object> latestCommitData =
            commits.get(0);

    List<Map<String, Object>> parents =
            (List<Map<String, Object>>) latestCommitData.get("parents");

    String previousCommit =
            (String) parents.get(0).get("sha");

    Map<String, Object> comparison =
            (Map<String, Object>) gitHubService.compareCommits(
                    owner, repo, previousCommit, latestCommit);

    return gitHubDiffService.generateChangeReport(comparison);
}
@GetMapping("/file-content")
public String getFileContent(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam String path,
        @RequestParam String ref) {

    return gitHubService.getFileContent(
            owner, repo, path, ref);
}
@GetMapping("/compare-java-files")
public Map<String, Object> compareGitHubJavaFiles(
        @RequestParam String owner,
        @RequestParam String repo,
        @RequestParam String path,
        @RequestParam String base,
        @RequestParam String head) {

    String oldSource = gitHubService.getFileContent(
            owner, repo, path, base);

    String newSource = gitHubService.getFileContent(
            owner, repo, path, head);

    return gitHubDiffService.compareGitHubJavaFiles(
            oldSource, newSource);
}
}
