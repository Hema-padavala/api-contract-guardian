
package com.apiguardian.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.apiguardian.service.OllamaService;
@Service
public class GitHubDiffService {
	private final OllamaService ollamaService;

	public GitHubDiffService(OllamaService ollamaService) {
	    this.ollamaService = ollamaService;
	}
    public List<Map<String, Object>> extractChangedFiles(
            Map<String, Object> comparison) {

        List<Map<String, Object>> files =
                (List<Map<String, Object>>) comparison.get("files");

        if (files == null) {
            return List.of();
        }

        return files.stream()
                .filter(file -> {
                    String filename = (String) file.get("filename");
                    return filename != null && filename.endsWith(".java");
                })
                .collect(Collectors.toList());
    }
    public List<String> extractJavaPatches(
            Map<String, Object> comparison) {

        List<Map<String, Object>> files =
                (List<Map<String, Object>>) comparison.get("files");

        if (files == null) {
            return List.of();
        }

        return files.stream()
                .filter(file -> {
                    String filename = (String) file.get("filename");
                    return filename != null && filename.endsWith(".java");
                })
                .map(file -> (String) file.get("patch"))
                .filter(patch -> patch != null)
                .collect(Collectors.toList());
    }
    public List<String> explainJavaPatches(Map<String, Object> comparison) {

        List<String> patches = extractJavaPatches(comparison);

        return patches.stream()
                .map(patch -> {
                	String prompt = """
                	        You are a strict Java code reviewer.

                	        Analyze ONLY the supplied GitHub diff.

                	        Rules:
                	        1. Lines starting with + are additions.
                	        2. Lines starting with - are removals.
                	        3. Lines starting with @@ are diff metadata, not code.
                	        4. Do not describe unchanged code.
                	        5. Do not invent imports, methods, classes or variables.
                	        6. Do not repeat the same change.
                	        7. If the diff shows a new file, clearly say it is a new file.
                	        8. If information is unavailable, say so.

                	        Return only:
                	        1. File change summary
                	        2. Added code
                	        3. Removed code
                	        4. Potential impact

                	        Keep the explanation concise.

                	        GITHUB DIFF:
                	        %s
                	        """.formatted(patch);

                    return ollamaService.generateResponse(prompt);
                })
                .collect(Collectors.toList());
    }
    public Map<String, List<Map<String, Object>>> groupFilesByStatus(
            Map<String, Object> comparison) {

        List<Map<String, Object>> files = extractChangedFiles(comparison);

        return files.stream()
                .collect(Collectors.groupingBy(
                        file -> (String) file.get("status")));
    }
}
