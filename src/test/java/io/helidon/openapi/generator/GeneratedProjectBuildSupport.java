package io.helidon.openapi.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.opentest4j.TestAbortedException;

import static org.assertj.core.api.Assertions.assertThat;

final class GeneratedProjectBuildSupport {

    private static final long BUILD_TIMEOUT_SECONDS = 5000;
    private static final String BUILD_TESTS_PROPERTY = "helidon.codegen.it.buildsWithMaven";

    private GeneratedProjectBuildSupport() {
    }

    static void assertMavenPackageSucceeds(Path projectDir) throws IOException, InterruptedException {
        if (System.getProperty(BUILD_TESTS_PROPERTY) == null) {
            throw new TestAbortedException("Skipping Maven build check. Set -D"
                                                   + BUILD_TESTS_PROPERTY + "=true to enable.");
        }

        String mavenExecutable = isWindows() ? "mvn.cmd" : "mvn";
        Path settingsFile = Files.createTempFile("maven-settings-", ".xml");
        Files.writeString(settingsFile, "<settings/>", StandardCharsets.UTF_8);

        ProcessBuilder processBuilder = new ProcessBuilder(
                mavenExecutable, "-B", "-q", "-s", settingsFile.toString(), "-DskipTests", "package");
        processBuilder.directory(projectDir.toFile());
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            byte[] outputBytes;
            try (var input = process.getInputStream()) {
                outputBytes = input.readAllBytes();
            }

            boolean finished = process.waitFor(BUILD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }

            String output = new String(outputBytes, StandardCharsets.UTF_8);
            assertThat(finished)
                    .as("Generated project Maven build timed out after %s seconds.%nOutput:%n%s",
                            BUILD_TIMEOUT_SECONDS, output)
                    .isTrue();
            if (process.exitValue() != 0 && isDependencyResolutionUnavailable(output)) {
                throw new TestAbortedException("Skipping Maven build check: dependency resolution unavailable "
                                                       + "in this environment.\n" + output);
            }
            assertThat(process.exitValue())
                    .as("Generated project Maven build failed.%nOutput:%n%s", output)
                    .isZero();
        } finally {
            Files.deleteIfExists(settingsFile);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private static boolean isDependencyResolutionUnavailable(String output) {
        return output.contains("Unknown host")
                || output.contains("Could not transfer artifact")
                || output.contains("Non-resolvable parent POM")
                || output.contains("Could not read artifact descriptor");
    }
}
