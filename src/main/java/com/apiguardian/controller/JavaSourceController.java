package com.apiguardian.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.apiguardian.service.JavaSourceAnalysisService;
import com.apiguardian.service.OllamaService;
import com.apiguardian.service.ChangeExplanationService;
import com.apiguardian.service.JavaFixSuggestionService;
import com.apiguardian.service.JavaCompilationService;
@RestController
@RequestMapping("/api/java")
public class JavaSourceController {

    private final JavaSourceAnalysisService analysisService;
    private final OllamaService ollamaService;
    private final ChangeExplanationService changeExplanationService;
    private final JavaFixSuggestionService fixSuggestionService;
    private final JavaCompilationService compilationService;
    private String explainJavaChange(String changeType, String details) {
    
        String prompt = """
                You are a Java code reviewer.

                Analyze ONLY the actual information provided below.

                Change type: %s

                CODE CHANGES:
                %s

                Follow these rules strictly:
                1. Identify the exact differences between old and new code.
                2. Never invent methods, variables, classes or code.
                3. Never use hypothetical examples.
                4. If the code does not provide enough information,
                   explicitly say so.
                5. Do not assume a bug exists without evidence.
                6. Do not generate replacement code unless requested.
                7. Clearly distinguish confirmed facts from possible risks.
                8. Never claim that callers, databases, or users are affected
                   unless evidence is provided.
                9. If caller code is not provided, say:
                   "Caller impact cannot be determined from the supplied code."
                Return these sections:
                1. Exact changes
                2. Why the changes matter
                3. Potential impact
                4. Recommended action

                Keep the explanation concise and factual.
                """.formatted(changeType, details);

        try {
            return ollamaService.generateResponse(prompt);
        } catch (Exception e) {
            return "AI explanation unavailable: " + e.getMessage();
        }
    }
    public JavaSourceController(
            JavaSourceAnalysisService analysisService,
            OllamaService ollamaService,
            ChangeExplanationService changeExplanationService,
            JavaFixSuggestionService fixSuggestionService,
            JavaCompilationService compilationService) {

        this.analysisService = analysisService;
        this.ollamaService = ollamaService;
        this.changeExplanationService = changeExplanationService;
        this.fixSuggestionService = fixSuggestionService;
        this.compilationService = compilationService;
    }
    @PostMapping("/compare")
    public ResponseEntity<?> compareJavaFiles(
            @RequestParam("oldFile") MultipartFile oldFile,
            @RequestParam("newFile") MultipartFile newFile) {

        try {
            String oldSource = new String(
                    oldFile.getBytes(), StandardCharsets.UTF_8);

            String newSource = new String(
                    newFile.getBytes(), StandardCharsets.UTF_8);

            Map<String, Object> report = new java.util.HashMap<>();

            report.put("oldClassName", analysisService.extractClassName(oldSource));
            report.put("newClassName", analysisService.extractClassName(newSource));
            report.put("methodComparison", analysisService.compareMethods(oldSource, newSource));
            report.put("modifiedMethodComparison",
                    analysisService.detectModifiedMethods(oldSource, newSource));

            report.put("endpointComparison",
                    analysisService.compareEndpoints(oldSource, newSource));
            Map<String, String> endpointImplementationChanges =
                    analysisService.compareEndpointImplementations(oldSource, newSource);

            report.put("endpointImplementationChanges", endpointImplementationChanges);
            Map<String, String> aiExplanations = new java.util.HashMap<>();
            Map<String, String> factualExplanations = new java.util.HashMap<>();
            for (Map.Entry<String, String> entry :
                endpointImplementationChanges.entrySet()) {

            String explanation = explainJavaChange(
                    "ENDPOINT_IMPLEMENTATION_CHANGED",
                    "Endpoint method: " + entry.getKey()
                            + "\nActual code differences:\n" + entry.getValue()
            );

            aiExplanations.put(
                    "endpoint_" + entry.getKey(),
                    explanation
            );
            factualExplanations.put(
                    "endpoint_" + entry.getKey(),
                    changeExplanationService.explainChange(
                            "ENDPOINT_IMPLEMENTATION_CHANGED",
                            entry.getKey() + ":\n" + entry.getValue()
                    )
            );
        }
            
            @SuppressWarnings("unchecked")
            Map<String, java.util.List<String>> modifiedComparison =
                    (Map<String, java.util.List<String>>) report.get("modifiedMethodComparison");

            for (String methodName : modifiedComparison.get("modifiedMethods")) {

                String explanation = explainJavaChange(
                        "MODIFIED_METHOD",
                        "The method " + methodName
                                + " exists in both versions, but its implementation has changed."
                );

                aiExplanations.put(methodName, explanation);
            }
            Map<String, java.util.List<String>> modifiedEndpointComparison =
                    analysisService.detectModifiedEndpoints(oldSource, newSource);

            report.put("modifiedEndpointComparison", modifiedEndpointComparison);

            Map<String, String> oldMethodBodies =
                    analysisService.extractMethodBodies(oldSource);

            Map<String, String> newMethodBodies =
                    analysisService.extractMethodBodies(newSource);

            for (String methodName : modifiedComparison.get("modifiedMethods")) {

                String oldBody = oldMethodBodies.get(methodName);
                String newBody = newMethodBodies.get(methodName);

                String actualDiff = analysisService.compareMethodBodyLines(
                        oldBody, newBody
                );

                String details = """
                        Method: %s

                        ACTUAL CODE DIFFERENCES:
                        %s
                        """.formatted(methodName, actualDiff);

                String explanation = explainJavaChange("MODIFIED_METHOD", details);

                aiExplanations.put(methodName, explanation);
                factualExplanations.put(
                        methodName,
                        changeExplanationService.explainChange(
                                "MODIFIED_METHOD",
                                methodName + ":\n" + actualDiff
                        )
                );
            }
            
                        
            Map<String, String> returnTypeChanges =
                    analysisService.compareReturnTypes(oldSource, newSource);
            Map<String, String> fixSuggestions = new java.util.HashMap<>();

            for (String methodName : modifiedComparison.get("modifiedMethods")) {

                String oldCode = oldMethodBodies.get(methodName);
                String newCode = newMethodBodies.get(methodName);

                String suggestion = fixSuggestionService.generateFixSuggestion(
                        "MODIFIED_METHOD",
                        oldCode == null ? "" : oldCode,
                        newCode == null ? "" : newCode
                );

                fixSuggestions.put(methodName, suggestion);
            }

            report.put("fixSuggestions", fixSuggestions);
            Map<String, String> compilationResults = new java.util.HashMap<>();

            for (String methodName : fixSuggestions.keySet()) {

            	String originalCode = fixSuggestions.get(methodName);

                String compilationResult = compilationService.compileJavaCode(
                        "public class CompilationTest {\n" +
                        "    public Object test() {\n" +
                        "        " + originalCode + "\n" +
                        "    }\n" +
                        "}"
                );
                compilationResults.put(methodName, compilationResult);
            }

            report.put("compilationResults", compilationResults);
            report.put("returnTypeComparison", returnTypeChanges);

            for (Map.Entry<String, String> entry : returnTypeChanges.entrySet()) {

                String methodName = entry.getKey();

                String oldBody = oldMethodBodies.get(methodName);
                String newBody = newMethodBodies.get(methodName);

                String actualDiff = analysisService.compareMethodBodyLines(
                        oldBody == null ? "" : oldBody,
                        newBody == null ? "" : newBody
                );

                String details = """
                        Method: %s
                        Return type change: %s

                        ACTUAL CODE DIFFERENCES:
                        %s
                        """.formatted(methodName, entry.getValue(), actualDiff);

                String explanation = explainJavaChange(
                        "RETURN_TYPE_CHANGED", details);

                aiExplanations.put(methodName + "_returnType", explanation);
                factualExplanations.put(
                        methodName + "_returnType",
                        changeExplanationService.explainChange(
                                "RETURN_TYPE_CHANGED",
                                methodName + ": " + entry.getValue() + "\n" + actualDiff
                        )
                );
            }
            report.put("aiExplanations", aiExplanations);
            report.put("factualExplanations", factualExplanations);
            return ResponseEntity.ok(report);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not read uploaded Java files."));
        }
    }
}