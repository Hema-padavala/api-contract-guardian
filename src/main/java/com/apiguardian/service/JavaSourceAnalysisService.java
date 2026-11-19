package com.apiguardian.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
@Service
public class JavaSourceAnalysisService {

    // Extract class name
    public String extractClassName(String sourceCode) {
    	

        if (sourceCode == null || sourceCode.isBlank()) {
            return "No Java source code provided";
        }

        String[] lines = sourceCode.split("\\R");

        for (String line : lines) {

            line = line.trim();

            if (line.contains(" class ")) {

                String[] parts = line.split("\\s+");

                for (int i = 0; i < parts.length; i++) {

                    if (parts[i].equals("class") && i + 1 < parts.length) {
                        return parts[i + 1];
                    }
                }
            }
        }

        return "Class name not found";
    }

    // Extract method names
    public List<String> extractMethodNames(String sourceCode) {

        List<String> methods = new ArrayList<>();

        if (sourceCode == null || sourceCode.isBlank()) {
            return methods;
        }

        Pattern pattern = Pattern.compile(
                "(public|private|protected)\\s+" +
                "[\\w<>\\[\\], ?]+?\\s+" +
                "(\\w+)\\s*\\([^{};]*\\)\\s*\\{"
        );

        Matcher matcher = pattern.matcher(sourceCode);

        while (matcher.find()) {
            methods.add(matcher.group(2));
        }

        return methods;
    }
    public Map<String, String> extractMethodReturnTypes(String sourceCode) {

        Map<String, String> returnTypes = new HashMap<>();

        Pattern pattern = Pattern.compile(
                "(public|private|protected)\\s+" +
                "([\\w<>\\[\\], ?]+?)\\s+" +
                "(\\w+)\\s*\\([^{};]*\\)\\s*\\{"
        );

        Matcher matcher = pattern.matcher(sourceCode);

        while (matcher.find()) {

            String returnType = matcher.group(2).trim();
            String methodName = matcher.group(3);

            returnTypes.put(methodName, returnType);
        }

        return returnTypes;
    }
    public Map<String, String> compareReturnTypes(
            String oldSource,
            String newSource) {

        Map<String, String> oldTypes =
                extractMethodReturnTypes(oldSource);

        Map<String, String> newTypes =
                extractMethodReturnTypes(newSource);

        Map<String, String> modified = new HashMap<>();

        for (String methodName : oldTypes.keySet()) {

            if (newTypes.containsKey(methodName)) {

                String oldType = oldTypes.get(methodName);
                String newType = newTypes.get(methodName);

                if (!oldType.equals(newType)) {

                    modified.put(
                            methodName,
                            oldType + " -> " + newType
                    );
                }
            }
        }

        return modified;
    }
    public Map<String, List<String>> compareMethods(
            String oldSource,
            String newSource) {

        List<String> oldMethods = extractMethodNames(oldSource);
        List<String> newMethods = extractMethodNames(newSource);

        List<String> added = new ArrayList<>(newMethods);
        added.removeAll(oldMethods);

        List<String> removed = new ArrayList<>(oldMethods);
        removed.removeAll(newMethods);

        List<String> unchanged = new ArrayList<>(oldMethods);
        unchanged.retainAll(newMethods);
        List<String> modifiedMethods =
                detectModifiedMethods(oldSource, newSource)
                        .get("modifiedMethods");

        unchanged.removeAll(modifiedMethods);
        Map<String, List<String>> result = new HashMap<>();

        result.put("addedMethods", added);
        result.put("removedMethods", removed);
        result.put("unchangedMethods", unchanged);

        return result;
    }
    public Map<String, List<String>> detectModifiedMethods(
            String oldSource,
            String newSource) {

        Map<String, List<String>> result = new HashMap<>();

        List<String> modified = new ArrayList<>();

        Pattern pattern = Pattern.compile(
                "(public|private|protected)\\s+" +
                "[\\w<>\\[\\], ?]+?\\s+" +
                "(\\w+)\\s*\\([^{};]*\\)\\s*\\{([^{}]*)\\}"
        );

        Map<String, String> oldMethods = new HashMap<>();
        Map<String, String> newMethods = new HashMap<>();

        Matcher oldMatcher = pattern.matcher(oldSource);
        while (oldMatcher.find()) {
            oldMethods.put(oldMatcher.group(2), oldMatcher.group(3).trim());
        }

        Matcher newMatcher = pattern.matcher(newSource);
        while (newMatcher.find()) {
            newMethods.put(newMatcher.group(2), newMatcher.group(3).trim());
        }

        for (String methodName : oldMethods.keySet()) {

            if (newMethods.containsKey(methodName)
                    && !oldMethods.get(methodName).equals(newMethods.get(methodName))) {

                modified.add(methodName);
            }
        }

        result.put("modifiedMethods", modified);

        return result;
    }
    public List<String> extractEndpoints(String sourceCode) {

        List<String> endpoints = new ArrayList<>();

        if (sourceCode == null || sourceCode.isBlank()) {
            return endpoints;
        }

        Pattern pattern = Pattern.compile(
                "@(Get|Post|Put|Delete|Patch)Mapping\\s*\\(\\s*\"([^\"]+)\""
        );

        Matcher matcher = pattern.matcher(sourceCode);

        while (matcher.find()) {

            String httpMethod = matcher.group(1).toUpperCase();
            String endpoint = matcher.group(2);

            endpoints.add(httpMethod + " " + endpoint);
        }

        return endpoints;
    }
    public Map<String, List<String>> compareEndpoints(
            String oldSource,
            String newSource) {

        List<String> oldEndpoints = extractEndpoints(oldSource);
        List<String> newEndpoints = extractEndpoints(newSource);

        List<String> added = new ArrayList<>(newEndpoints);
        added.removeAll(oldEndpoints);

        List<String> removed = new ArrayList<>(oldEndpoints);
        removed.removeAll(newEndpoints);

        List<String> unchanged = new ArrayList<>(oldEndpoints);
        unchanged.retainAll(newEndpoints);

        Map<String, List<String>> result = new HashMap<>();

        result.put("addedEndpoints", added);
        result.put("removedEndpoints", removed);
        result.put("unchangedEndpoints", unchanged);

        return result;
    }

public Map<String, List<String>> detectModifiedEndpoints(
        String oldSource,
        String newSource) {

    List<String> oldEndpoints = extractEndpoints(oldSource);
    List<String> newEndpoints = extractEndpoints(newSource);

    List<String> modified = new ArrayList<>();

    for (String oldEndpoint : oldEndpoints) {

        String[] oldParts = oldEndpoint.split(" ", 2);

        if (oldParts.length < 2) {
            continue;
        }

        for (String newEndpoint : newEndpoints) {

            String[] newParts = newEndpoint.split(" ", 2);

            if (newParts.length < 2) {
                continue;
            }

            boolean sameHttpMethod =
                    oldParts[0].equals(newParts[0]);

            boolean differentPath =
                    !oldParts[1].equals(newParts[1]);

            if (sameHttpMethod && differentPath) {

                modified.add(oldEndpoint + " -> " + newEndpoint);
                break;
            }
        }
    }

    Map<String, List<String>> result = new HashMap<>();

    result.put("modifiedEndpoints", modified);

    return result;
}
public Map<String, String> extractMethodBodies(String sourceCode) {

    Map<String, String> methods = new HashMap<>();

    Pattern pattern = Pattern.compile(
            "(public|private|protected)\\s+" +
            "[\\w<>\\[\\], ?]+?\\s+" +
            "(\\w+)\\s*\\([^)]*\\)\\s*\\{"
    );

    Matcher matcher = pattern.matcher(sourceCode);

    while (matcher.find()) {

        String methodName = matcher.group(2);
        int bodyStart = matcher.end();

        int braceCount = 1;
        int i = bodyStart;

        while (i < sourceCode.length() && braceCount > 0) {

            char ch = sourceCode.charAt(i);

            if (ch == '{') {
                braceCount++;
            } else if (ch == '}') {
                braceCount--;
            }

            i++;
        }

        if (braceCount == 0) {
            String methodBody = sourceCode.substring(bodyStart, i - 1).trim();
            methods.put(methodName, methodBody);
        }
    }

    return methods;
}

public String compareMethodBodyLines(String oldBody, String newBody) {

    String[] oldLines = oldBody.split("\\R");
    String[] newLines = newBody.split("\\R");

    int m = oldLines.length;
    int n = newLines.length;

    int[][] dp = new int[m + 1][n + 1];

    for (int i = m - 1; i >= 0; i--) {
        for (int j = n - 1; j >= 0; j--) {

            if (oldLines[i].trim().equals(newLines[j].trim())) {
                dp[i][j] = 1 + dp[i + 1][j + 1];
            } else {
                dp[i][j] = Math.max(
                    dp[i + 1][j],
                    dp[i][j + 1]
                );
            }
        }
    }

    StringBuilder diff = new StringBuilder();

    int i = 0;
    int j = 0;

    while (i < m && j < n) {

        if (oldLines[i].trim().equals(newLines[j].trim())) {
            i++;
            j++;
        } else if (dp[i + 1][j] >= dp[i][j + 1]) {
            diff.append("- ").append(oldLines[i]).append("\n");
            i++;
        } else {
            diff.append("+ ").append(newLines[j]).append("\n");
            j++;
        }
    }

    while (i < m) {
        diff.append("- ").append(oldLines[i]).append("\n");
        i++;
    }

    while (j < n) {
        diff.append("+ ").append(newLines[j]).append("\n");
        j++;
    }

    return diff.length() == 0
            ? "No line-level differences found."
            : diff.toString();
}


public Map<String, String> compareEndpointImplementations(
        String oldSource, String newSource) {

    Map<String, String> oldMethods = extractMethodBodies(oldSource);
    Map<String, String> newMethods = extractMethodBodies(newSource);

    Map<String, String> changes = new HashMap<>();

    Pattern endpointPattern = Pattern.compile(
            "@(Get|Post|Put|Delete|Patch)Mapping\\s*(?:\\([^)]*\\))?\\s*" +
            "(?:public|protected|private)\\s+" +
            "[\\w<>\\[\\], ?]+?\\s+(\\w+)\\s*\\("
    );

    Matcher oldMatcher = endpointPattern.matcher(oldSource);

    while (oldMatcher.find()) {

        String methodName = oldMatcher.group(2);

        if (newMethods.containsKey(methodName)
                && oldMethods.containsKey(methodName)) {

            String diff = compareMethodBodyLines(
                    oldMethods.get(methodName),
                    newMethods.get(methodName)
            );

            if (!diff.equals("No line-level differences found.")) {
                changes.put(methodName, diff);
            }
        }
    }

    return changes;
}


}