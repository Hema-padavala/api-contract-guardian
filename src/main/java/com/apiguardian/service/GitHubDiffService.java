
package com.apiguardian.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class GitHubDiffService {

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
}
