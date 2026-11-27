
package com.apiguardian.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;


import java.util.Map;

class GitHubServiceTest {

    @Test
    void testGetRepositoryInfo() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(builder).build();

        GitHubService service = new GitHubService(builder);

        mockServer.expect(requestTo(
        		"https://api.github.com/repos/owner/repo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        "Accept", "application/vnd.github+json"))
                .andRespond(withSuccess(
                        "{\"name\":\"api-contract-guardian\"}",
                        MediaType.APPLICATION_JSON));

        Map<?, ?> result =
                service.getRepositoryInfo("owner", "repo");

        assertEquals("api-contract-guardian", result.get("name"));

        mockServer.verify();
    }

@Test
void testGetRepositoryCommits() {

    RestClient.Builder builder = RestClient.builder();

    MockRestServiceServer mockServer =
            MockRestServiceServer.bindTo(builder).build();

    GitHubService service = new GitHubService(builder);

    mockServer.expect(requestTo(
            "https://api.github.com/repos/owner/repo/commits"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(
                    "[{\"sha\":\"abc123\"}]",
                    MediaType.APPLICATION_JSON));

    Object result = service.getRepositoryCommits("owner", "repo");

    assertEquals(1, ((java.util.List<?>) result).size());

    mockServer.verify();
}

@Test
void testGetFileContent() {

    RestClient.Builder builder = RestClient.builder();

    MockRestServiceServer mockServer =
            MockRestServiceServer.bindTo(builder).build();

    GitHubService service = new GitHubService(builder);

    String encodedContent = java.util.Base64.getEncoder()
            .encodeToString("Hello GitHub".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8));

    mockServer.expect(requestTo(
            "https://api.github.com/repos/owner/repo/contents/README.md?ref=main"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(
                    "{\"content\":\"" + encodedContent + "\"}",
                    MediaType.APPLICATION_JSON));

    String result = service.getFileContent(
            "owner", "repo", "README.md", "main");

    assertEquals("Hello GitHub", result);

    mockServer.verify();
}

}
