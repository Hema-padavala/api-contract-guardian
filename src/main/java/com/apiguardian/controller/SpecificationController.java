
package com.apiguardian.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import com.apiguardian.service.SpecificationParserService;
import com.apiguardian.service.SpecificationComparisonService;
import com.apiguardian.service.ChangeExplanationService;
import com.apiguardian.service.OllamaService;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/specifications")
public class SpecificationController {

    private final SpecificationParserService parserService;
    private final SpecificationComparisonService comparisonService;
    private final ChangeExplanationService explanationService;
    private final OllamaService ollamaService;
    public SpecificationController(
            SpecificationParserService parserService,
            SpecificationComparisonService comparisonService,
            ChangeExplanationService explanationService,
            OllamaService ollamaService) {

        this.parserService = parserService;
        this.comparisonService = comparisonService;
        this.explanationService = explanationService;
        this.ollamaService = ollamaService;
    }


    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadSpecifications(
            @RequestParam("oldFile") MultipartFile oldFile,
            @RequestParam("newFile") MultipartFile newFile)
            throws IOException {

        if (oldFile.isEmpty() || newFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Please upload both specification files."));
        }

        String oldContent = new String(
                oldFile.getBytes(), StandardCharsets.UTF_8);
        String newContent = new String(
                newFile.getBytes(), StandardCharsets.UTF_8);

        try {
            Set<String> oldEndpoints =
                    parserService.extractEndpoints(oldContent);
            Set<String> newEndpoints =
                    parserService.extractEndpoints(newContent);

            Set<String> oldOperations =
                    parserService.extractOperations(oldContent);
            Set<String> newOperations =
                    parserService.extractOperations(newContent);

            Set<String> oldParameters =
                    parserService.extractParameters(oldContent);
            Set<String> newParameters =
                    parserService.extractParameters(newContent);

            Map<String, Object> report =
                    comparisonService.generateStructuredCompatibilityReport(
                            oldEndpoints,
                            newEndpoints,
                            oldOperations,
                            newOperations,
                            oldParameters,
                            newParameters);
            @SuppressWarnings("unchecked")
            List<Map<String, String>> originalChanges =
                    (List<Map<String, String>>) report.get("changes");

            List<Map<String, String>> changes = new java.util.ArrayList<>();

            for (Map<String, String> originalChange : originalChanges) {
                changes.add(new java.util.HashMap<>(originalChange));
            }

            report.put("changes", changes);
            for (Map<String, String> change : changes) {

                String aiPrompt = """
                        You are an experienced Java Spring Boot API developer.

                        Explain the following API change in simple language.

                        Change type: %s
                        Change details: %s

                        Provide these four sections:
                        1. What changed
                        2. Why it matters
                        3. Possible application impact
                        4. Suggested Java Spring Boot fix

                        Keep the answer practical and beginner-friendly.
                        """.formatted(
                                change.get("type"),
                                change.get("description")
                        );

                try {
                    String aiExplanation =
                            ollamaService.generateResponse(aiPrompt);

                    change.put("aiExplanation", aiExplanation);

                } catch (Exception e) {
                    String fallback = explanationService.explainChange(
                            change.get("type"),
                            change.get("description")
                    );

                    change.put("aiExplanation", fallback);
                }
            }

            return ResponseEntity.ok(report);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Comparison failed: " + e.getClass().getName()
                                    + " - " + String.valueOf(e.getMessage())
                    ));
        }
    }
    @GetMapping("/ai-test")
    public ResponseEntity<String> testAI() {

        try {
            String response = ollamaService.testOllama();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ollama Error: " + e.getMessage());
        }
    }
}
