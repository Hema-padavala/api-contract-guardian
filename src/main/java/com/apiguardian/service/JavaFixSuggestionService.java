
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




public String generateTestCases(String javaSource, String methodName) {

    if ("getRepositoryInfo".equals(methodName)) {
        return """
                package com.apiguardian.generated;

                import static org.junit.jupiter.api.Assertions.assertEquals;
                import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
                import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

                import org.junit.jupiter.api.Test;
                import org.springframework.http.MediaType;
                import org.springframework.test.web.client.MockRestServiceServer;
                import org.springframework.web.client.RestClient;

                import java.util.Map;
                import com.apiguardian.service.GitHubService;

                class GeneratedJUnitTest {

                    @Test
                    void testGetRepositoryInfo() {

                        RestClient.Builder builder = RestClient.builder();

                        MockRestServiceServer mockServer =
                                MockRestServiceServer.bindTo(builder).build();

                        GitHubService service = new GitHubService(builder);

                        mockServer.expect(requestTo(
                                "https://api.github.com/repos/owner/repo"))
                                .andRespond(withSuccess(
                                        "{\\"name\\":\\"api-contract-guardian\\"}",
                                        MediaType.APPLICATION_JSON));

                        Map<?, ?> result =
                                service.getRepositoryInfo("owner", "repo");

                        mockServer.verify();

                        assertEquals("api-contract-guardian", result.get("name"));
                    }
                }
                """;
    }

    return "No deterministic test template available for method: " + methodName;
}



}
