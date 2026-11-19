
package com.apiguardian.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class JavaCompilationService {

    public String compileJavaFile(String filePath) {

        try {
            Process process = new ProcessBuilder(
                    "javac",
                    filePath
            )
            .redirectErrorStream(true)
            .start();

            boolean completed = process.waitFor(
                    30, TimeUnit.SECONDS
            );

            if (!completed) {
                process.destroyForcibly();
                return "COMPILATION_FAILED: Compilation timed out.";
            }

            String output = new String(
                    process.getInputStream().readAllBytes()
            );

            if (process.exitValue() == 0) {
                return "COMPILATION_SUCCESS: Java code compiled successfully.";
            }

            return "COMPILATION_FAILED:\n" + output;

        } catch (IOException e) {
            return "COMPILATION_ERROR: " + e.getMessage();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "COMPILATION_INTERRUPTED";
        }
        
    }
    public String compileJavaCode(String sourceCode) {

        java.nio.file.Path tempDir = null;

        try {
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile(
                            "public\\s+(?:final\\s+|abstract\\s+)?class\\s+(\\w+)"
                    ).matcher(sourceCode);

            if (!matcher.find()) {
                return "COMPILATION_ERROR: Public class not found.";
            }

            String className = matcher.group(1);

            tempDir = java.nio.file.Files.createTempDirectory("guardian");

            File tempFile = tempDir.resolve(className + ".java").toFile();

            java.nio.file.Files.writeString(
                    tempFile.toPath(), sourceCode
            );

            return compileJavaFile(tempFile.getAbsolutePath());

        } catch (IOException e) {
            return "COMPILATION_ERROR: " + e.getMessage();

        } finally {
            if (tempDir != null) {
                try (var files = java.nio.file.Files.walk(tempDir)) {
                    files.sorted(java.util.Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    java.nio.file.Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                }
                            });
                } catch (IOException ignored) {
                }
            }
        }
    }
}
