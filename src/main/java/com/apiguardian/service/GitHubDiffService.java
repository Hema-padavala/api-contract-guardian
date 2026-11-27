
package com.apiguardian.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import org.springframework.stereotype.Service;
import com.apiguardian.service.OllamaService;
import com.apiguardian.service.JavaSourceAnalysisService;
import com.apiguardian.service.JavaFixSuggestionService;
import com.apiguardian.service.JavaCompilationService;
@Service
public class GitHubDiffService {
	private final OllamaService ollamaService;
	private final JavaSourceAnalysisService analysisService;
	private final JavaFixSuggestionService fixSuggestionService;
	private final JavaCompilationService compilationService;
	public GitHubDiffService(
	        OllamaService ollamaService,
	        JavaSourceAnalysisService analysisService,
	        JavaFixSuggestionService fixSuggestionService,
	        JavaCompilationService compilationService) {

	    this.ollamaService = ollamaService;
	    this.analysisService = analysisService;
	    this.fixSuggestionService = fixSuggestionService;
	    this.compilationService = compilationService;
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

        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        result.put("added", new ArrayList<>());
        result.put("modified", new ArrayList<>());
        result.put("removed", new ArrayList<>());

        for (Map<String, Object> file : files) {
            String status = (String) file.get("status");

            if (result.containsKey(status)) {
                result.get(status).add(file);
            }
        }

        return result;
    }
    public Map<String, Object> generateChangeReport(
            Map<String, Object> comparison) {

        Map<String, Object> report = new HashMap<>();

        report.put("filesByStatus", groupFilesByStatus(comparison));
        report.put("javaPatches", extractJavaPatches(comparison));
        report.put("aiExplanations", explainJavaPatches(comparison));

        return report;
    }
    public Map<String, Object> compareGitHubJavaFiles(
            String oldSource, String newSource) {

        Map<String, Object> result = new HashMap<>();

        result.put("oldClassName",
                analysisService.extractClassName(oldSource));

        result.put("newClassName",
                analysisService.extractClassName(newSource));

        result.put("methodComparison",
                analysisService.compareMethods(oldSource, newSource));

        result.put("returnTypeComparison",
                analysisService.compareReturnTypes(oldSource, newSource));

        result.put("endpointComparison",
                analysisService.compareEndpoints(oldSource, newSource));

        result.put("modifiedMethods",
                analysisService.detectModifiedMethods(oldSource, newSource));

        result.put("endpointImplementationChanges",
                analysisService.compareEndpointImplementations(
                        oldSource, newSource));

        return result;
    } 
    public Map<String, String> generateGitHubFixSuggestions(
            String oldSource, String newSource) {

        Map<String, String> oldMethods =
                analysisService.extractMethodBodies(oldSource);

        Map<String, String> newMethods =
                analysisService.extractMethodBodies(newSource);

        Map<String, String> suggestions = new HashMap<>();

        for (String methodName : oldMethods.keySet()) {

            if (newMethods.containsKey(methodName)
                    && !oldMethods.get(methodName).trim()
                            .equals(newMethods.get(methodName).trim())) {

                String suggestion =
                        fixSuggestionService.generateFixSuggestion(
                                "MODIFIED_METHOD",
                                oldMethods.get(methodName),
                                newMethods.get(methodName));

                suggestions.put(methodName, suggestion);
            }
        }

        return suggestions;
    }


    public Map<String, String> validateGitHubFixSuggestions(
            String oldSource,
            String newSource,
            Map<String, String> suggestions) {

        Map<String, String> results = new HashMap<>();

        // Check safety before compiling suggestions
        Map<String, String> safetyResults =
                validateFixSafety(suggestions, oldSource);

        Map<String, String> newMethods =
                analysisService.extractMethodBodies(newSource);

        for (Map.Entry<String, String> entry : suggestions.entrySet()) {

            String methodName = entry.getKey();
            String suggestedBody = entry.getValue();

            String safetyResult = safetyResults.get(methodName);

            // Do not compile unsafe suggestions
            if (safetyResult == null ||
                    !safetyResult.startsWith("SAFETY_CHECK_PASSED")) {

                results.put(methodName,
                        "VALIDATION_SKIPPED: " + safetyResult);
                continue;
            }

            String originalBody = newMethods.get(methodName);

            if (originalBody == null) {
                results.put(methodName,
                        "VALIDATION_FAILED: Original method not found.");
                continue;
            }

            String updatedSource = newSource.replace(
                    originalBody, suggestedBody
            );

            String validation =
                    compilationService.compileJavaCode(updatedSource);

            results.put(methodName, validation);
        }

        return results;
    }

    public Map<String, String> verifyFixQuality(
            String oldSource,
            String newSource,
            Map<String, String> suggestions) {

        Map<String, String> results = new HashMap<>();

        Map<String, String> oldMethods =
                analysisService.extractMethodBodies(oldSource);

        Map<String, String> newMethods =
                analysisService.extractMethodBodies(newSource);

        for (Map.Entry<String, String> entry : suggestions.entrySet()) {

            String methodName = entry.getKey();

            String prompt = """
                    You are a Java code reviewer.

                    Compare these three method bodies:

                    ORIGINAL METHOD:
                    %s

                    MODIFIED METHOD:
                    %s

                    AI SUGGESTION:
                    %s

                    Check:
                    1. Does the suggestion preserve the intended behavior?
                    2. Does it introduce possible bugs?
                    3. Does it preserve necessary functionality?

                    Return:
                    VERDICT: PASS or FAIL

                    REASON: Short explanation

                    Do not invent missing information.
                    """.formatted(
                        oldMethods.get(methodName),
                        newMethods.get(methodName),
                        entry.getValue()
                    );

            try {
                results.put(methodName,
                        ollamaService.generateResponse(prompt));

            } catch (Exception e) {
                results.put(methodName,
                        "VERIFICATION_ERROR: " + e.getMessage());
            }
        }

        return results;
    }

public Map<String, String> generateGitHubTestCases(
        String oldSource, String newSource) {

    Map<String, String> oldMethods =
            analysisService.extractMethodBodies(oldSource);

    Map<String, String> newMethods =
            analysisService.extractMethodBodies(newSource);

    Map<String, String> results = new HashMap<>();

    for (String methodName : oldMethods.keySet()) {

        if (newMethods.containsKey(methodName)
                && !oldMethods.get(methodName).trim()
                        .equals(newMethods.get(methodName).trim())) {

            String testCases = fixSuggestionService.generateTestCases(
                    newSource, methodName
            );

            results.put(methodName, testCases);
        }
    }

    return results;
}

public Map<String, String> generateAndRunGitHubTestCases(
        String oldSource, String newSource) {

    Map<String, String> generatedTests =
            generateGitHubTestCases(oldSource, newSource);

    Map<String, String> executionResults = new HashMap<>();

    for (String methodName : generatedTests.keySet()) {

        String testSource = generatedTests.get(methodName);
        System.out.println("GENERATED TEST SOURCE:");
        System.out.println(testSource);

        String result = compilationService.runGeneratedJUnitTests(
                testSource
        );

        executionResults.put(methodName, result);
    }

    return executionResults;
}

public Map<String, Object> generateAutomaticTestReport(
        String oldSource, String newSource) {

    Map<String, Object> report = new HashMap<>();

    report.put("codeComparison",
            compareGitHubJavaFiles(oldSource, newSource));

    // Generate suggestions only once
    Map<String, String> suggestions =
            generateGitHubFixSuggestions(oldSource, newSource);

    report.put("fixSuggestions", suggestions);

    report.put("fixSafetyValidation",
            validateFixSafety(suggestions, oldSource));

    report.put("fixValidation",
            validateGitHubFixSuggestions(
                    oldSource, newSource, suggestions));

    report.put("fixQualityVerification",
            verifyFixQuality(oldSource, newSource, suggestions));

    report.put("generatedTestResults",
            generateAndRunGitHubTestCases(oldSource, newSource));
    Map<String, String> safetyResults =(Map<String, String>) report.get("fixSafetyValidation");
    Map<String, String> qualityResults =
            (Map<String, String>) report.get("fixQualityVerification");

    for (Map.Entry<String, String> entry : safetyResults.entrySet()) {

        String methodName = entry.getKey();
        String safetyResult = entry.getValue();

        if (!safetyResult.startsWith("SAFETY_CHECK_PASSED")) {

            qualityResults.put(methodName,
                    "VERDICT: FAIL\n" +
                    "REASON: Fix rejected by safety validation. " +
                    safetyResult);
        }
    }

    boolean allFixesSafe = safetyResults.values().stream()
            .allMatch(result -> result.startsWith("SAFETY_CHECK_PASSED"));

    report.put("overallStatus",
            allFixesSafe ? "READY_FOR_APPROVAL" : "FIXES_REJECTED");
    Map<String, String> decisionReport = new HashMap<>();

    for (Map.Entry<String, String> entry : safetyResults.entrySet()) {

        String methodName = entry.getKey();
        String safetyResult = entry.getValue();

        if (safetyResult.startsWith("SAFETY_CHECK_PASSED")) {

            decisionReport.put(methodName, "APPROVED");

        } else {

            decisionReport.put(methodName, "REJECTED");
        }
    }

    report.put("fixDecisionReport", decisionReport);
    boolean anyRejected = decisionReport.values().stream()
            .anyMatch(status -> status.equals("REJECTED"));
    boolean anyFailed = report.get("generatedTestResults")
            .toString().contains("TEST_FAILED");

    Map<String, String> fixValidationResults =
            (Map<String, String>) report.get("fixValidation");

    boolean anyCompilationFailed = fixValidationResults.values().stream()
            .anyMatch(result ->
                    result.startsWith("COMPILATION_FAILED")
                    || result.startsWith("COMPILATION_ERROR")
                    || result.startsWith("VALIDATION_FAILED"));

    String finalWorkflowStatus;

    if (anyRejected) {
        finalWorkflowStatus = "WORKFLOW_BLOCKED_UNSAFE_FIX";
    } else if (anyCompilationFailed) {
        finalWorkflowStatus = "WORKFLOW_FAILED_COMPILATION";
    } else if (anyFailed) {
        finalWorkflowStatus = "WORKFLOW_FAILED_TESTS";
    } else {
        finalWorkflowStatus = "WORKFLOW_READY";
    }

    report.put("finalWorkflowStatus", finalWorkflowStatus);

    return report;
}



public Map<String, String> validateFixSafety(
        Map<String, String> suggestions, String newSource) {

    Map<String, String> results = new HashMap<>();

    for (Map.Entry<String, String> entry : suggestions.entrySet()) {

        String methodName = entry.getKey();
        String suggestion = entry.getValue();

        String originalMethod = analysisService
                .extractMethodBodies(newSource)
                .get(methodName);
        System.out.println("ORIGINAL METHOD: " + originalMethod);
        System.out.println("AI SUGGESTION: " + suggestion);

        if (originalMethod == null) {
            results.put(methodName,
                    "REJECTED: Original method not found.");
            continue;
        }

        if (suggestion.contains("?test=true")
                && !originalMethod.contains("?test=true")) {

            results.put(methodName,
                    "REJECTED: Introduces unsupported URL parameter.");
            continue;
        }

        if (suggestion.contains("http://")
                || suggestion.contains("https://")) {

            results.put(methodName,
                    "REJECTED: Introduces a hardcoded URL.");
            continue;
        }

        results.put(methodName,
                "SAFETY_CHECK_PASSED: No detected suspicious changes.");
    }

    return results;
}



}
