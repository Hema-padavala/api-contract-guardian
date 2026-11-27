
package com.apiguardian.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
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

        java.nio.file.Files.writeString(tempFile.toPath(), sourceCode);

        String projectPath = System.getProperty("user.dir");

        File projectDir = new File(projectPath);
        File classpathFile = tempDir.resolve("classpath.txt").toFile();

        String wrapperPath = projectPath + File.separator + "mvnw.cmd";

        String command = "call \"" + wrapperPath
                + "\" -q -DskipTests compile dependency:build-classpath"
                + " -Dmdep.outputFile=\"" + classpathFile.getAbsolutePath() + "\"";
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.directory(projectDir);
        builder.redirectErrorStream(true);

        builder.environment().put(
                "PATH",
                "C:\\Windows\\System32\\WindowsPowerShell\\v1.0;"
                + "C:\\Windows\\System32;"
                + builder.environment().get("PATH")
        );

        Process process = builder.start();

        boolean completed = process.waitFor(180, TimeUnit.SECONDS);

        String mavenOutput = new String(
                process.getInputStream().readAllBytes()
        );

        if (!completed) {
            process.destroyForcibly();
            return "COMPILATION_FAILED: Maven timed out.";
        }

        if (process.exitValue() != 0) {
            return "COMPILATION_FAILED: Maven dependency setup failed.\n"
                    + mavenOutput;
        }

        String classpath = java.nio.file.Files.readString(
                classpathFile.toPath()
        );

        classpath += File.pathSeparator
                + projectPath + File.separator + "target"
                + File.separator + "classes";

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        if (compiler == null) {
            return "COMPILATION_ERROR: JDK compiler not available.";
        }

        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        int result = compiler.run(
                null,
                null,
                errors,
                "-classpath", classpath,
                "-d", tempDir.toString(),
                tempFile.getAbsolutePath()
        );

        if (result == 0) {
            return "COMPILATION_SUCCESS: AI-generated Java source compiled successfully.";
        }

        return "COMPILATION_FAILED:\n" + errors.toString();

    } catch (Exception e) {
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



public String runGeneratedJUnitTests(String testSource) {

    java.nio.file.Path testFile = null;
    java.nio.file.Path logFile = null;

    try {
        // 1. Validate AI response
        if (testSource == null || testSource.isBlank()) {
            return "TEST_REJECTED: Empty AI response.";
        }

        testSource = testSource.trim();

        if (testSource.contains("`")
                || testSource.contains("###")
                || testSource.toLowerCase().contains("here is the")) {
            return "TEST_REJECTED: AI returned Markdown or explanation.";
        }

        if (!testSource.contains("@Test")) {
            return "TEST_REJECTED: No JUnit test method found.";
        }

        if (testSource.contains("RestTemplate")
                || testSource.contains("WebFluxTest")
                || testSource.contains("WebTestClient")
                || testSource.contains("stubFor")
                || testSource.contains("urlEqualTo")
                || testSource.contains("aResponse")) {
            System.out.println("UNSUPPORTED AI TEST CODE:");
            System.out.println(testSource);
            return "TEST_REJECTED: AI generated unsupported testing APIs.";
            }

        // 2. Find test class
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile(
                        "\\bclass\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b"
                ).matcher(testSource);

        if (!matcher.find()) {
            return "TEST_REJECTED: Test class not found.";
        }

        String className = matcher.group(1);

        // 3. Prepare test directory
        java.nio.file.Path projectDir =
                java.nio.file.Path.of(System.getProperty("user.dir"));

        java.nio.file.Path testDirectory =
                projectDir.resolve(
                        "src/test/java/com/apiguardian/generated"
                );

        java.nio.file.Files.createDirectories(testDirectory);

        if (!testSource.contains("package ")) {
            testSource =
                    "package com.apiguardian.generated;\n" + testSource;
        }

        testFile = testDirectory.resolve(className + ".java");

        java.nio.file.Files.writeString(testFile, testSource);

        // 4. Execute JUnit test using Maven
        logFile = java.nio.file.Files.createTempFile(
                "junit-output-", ".log"
        );

        String wrapper = projectDir.resolve("mvnw.cmd").toString();

        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", "call",
                wrapper, "-Dtest=" + className, "test"
        );

        builder.directory(projectDir.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(logFile.toFile());

        builder.environment().put(
                "PATH",
                System.getenv("PATH")
                + ";C:\\Windows\\System32\\WindowsPowerShell\\v1.0"
                + ";C:\\Windows\\System32"
        );

        Process process = builder.start();

        boolean completed = process.waitFor(
                180, TimeUnit.SECONDS
        );

        if (!completed) {
            process.destroyForcibly();
            process.waitFor();
            return "TEST_TIMEOUT: Maven exceeded 180 seconds.";
        }

        String output = java.nio.file.Files.readString(logFile);

        if (process.exitValue() == 0) {
            return "TEST_SUCCESS:\n" + output;
        }

        return "TEST_FAILED:\n" + output;

    } catch (Exception e) {
        return "TEST_ERROR: " + e.getMessage();

    } finally {
        try {
            if (testFile != null) {
                java.nio.file.Files.deleteIfExists(testFile);
            }

            if (logFile != null) {
                java.nio.file.Files.deleteIfExists(logFile);
            }
        } catch (IOException ignored) {
        }
    }
}



}
