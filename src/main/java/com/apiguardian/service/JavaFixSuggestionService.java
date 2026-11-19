
package com.apiguardian.service;

import org.springframework.stereotype.Service;

@Service
public class JavaFixSuggestionService {

    private final OllamaService ollamaService;

    public JavaFixSuggestionService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public String generateFixSuggestion(
            String changeType,
            String oldCode,
            String newCode) {

        String prompt = """
                You are a Java code generator.
        		Return ONLY valid Java code.
        		Do not include explanations or Markdown code fences.

                Analyze the old and new Java code.

                Change type: %s

                OLD CODE:
                %s

                NEW CODE:
                %s

                Tasks:
				1. Return the corrected Java method body only.
				2. Preserve the original method's purpose and signature.
				3. Fix only issues supported by the supplied code.
				4. If no fix is needed, return the NEW CODE unchanged.
				5. Never invent missing classes, variables, or methods.
                Keep the response concise.
                """.formatted(changeType, oldCode, newCode);

        try {
            return ollamaService.generateResponse(prompt);
        } catch (Exception e) {
            return "Fix suggestion unavailable: " + e.getMessage();
        }
    }
}
