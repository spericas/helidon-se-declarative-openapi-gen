package io.helidon.openapi.generator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test — verifies gap features: deprecated, Javadoc, enums,
 * default values, nullable, and parameter-level validation.
 */
class FeaturesGenerationIT {

    @TempDir
    static Path outputDir;

    @BeforeAll
    static void generate() throws Exception {
        URL resource = FeaturesGenerationIT.class
                .getClassLoader()
                .getResource("features.yaml");
        String specPath = Paths.get(resource.toURI()).toAbsolutePath().toString();

        CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("helidon-se-declarative")
                .setInputSpec(specPath)
                .setOutputDir(outputDir.toString())
                .addAdditionalProperty("helidonVersion", "4.4.0")
                .addAdditionalProperty("apiPackage", "io.helidon.example.api")
                .addAdditionalProperty("modelPackage", "io.helidon.example.model")
                .addAdditionalProperty("invokerPackage", "io.helidon.example");

        new DefaultGenerator().opts(configurator.toClientOptInput()).generate();
    }

    // -------------------------------------------------------------------------
    // deprecated: true → @Deprecated on operation method
    // -------------------------------------------------------------------------

    @Test
    void apiInterfaceDeprecatedOperationHasDeprecatedAnnotation() throws IOException {
        assertThat(read(apiFile("ThingsApi.java")))
                .contains("@Deprecated");
    }

    @Test
    void endpointImplementsApiContract() throws IOException {
        assertThat(read(apiFile("ThingsEndpoint.java")))
                .contains("implements ThingsApi");
    }

    // -------------------------------------------------------------------------
    // summary / description → Javadoc
    // -------------------------------------------------------------------------

    @Test
    void endpointOperationHasJavadocFromSummary() throws IOException {
        assertThat(read(apiFile("ThingsEndpoint.java")))
                .contains("List all things");
    }

    @Test
    void endpointOperationHasJavadocFromDescription() throws IOException {
        assertThat(read(apiFile("ThingsEndpoint.java")))
                .contains("Returns a paginated list of things");
    }

    @Test
    void apiInterfaceOperationHasJavadocFromSummary() throws IOException {
        assertThat(read(apiFile("ThingsApi.java")))
                .contains("List all things");
    }

    // -------------------------------------------------------------------------
    // Enum property → inner enum class in model
    // -------------------------------------------------------------------------

    @Test
    void modelEnumPropertyHasInnerEnumClass() throws IOException {
        String content = read(modelFile("Thing.java"));
        assertThat(content).contains("public enum StatusEnum");
    }

    @Test
    void modelEnumPropertyHasEnumConstants() throws IOException {
        String content = read(modelFile("Thing.java"));
        assertThat(content)
                .contains("ACTIVE")
                .contains("INACTIVE")
                .contains("PENDING");
    }

    @Test
    void modelEnumPropertyFieldUsesEnumType() throws IOException {
        assertThat(read(modelFile("Thing.java")))
                .contains("StatusEnum status");
    }

    // -------------------------------------------------------------------------
    // Default values → field initializers
    // -------------------------------------------------------------------------

    @Test
    void modelStringEnumDefaultHasInitializer() throws IOException {
        // status has default: active → StatusEnum.ACTIVE
        assertThat(read(modelFile("Thing.java")))
                .contains("StatusEnum.ACTIVE");
    }

    @Test
    void modelIntegerDefaultHasInitializer() throws IOException {
        // count has default: 0
        assertThat(read(modelFile("Thing.java")))
                .contains("= 0");
    }

    @Test
    void modelDoubleDefaultHasInitializer() throws IOException {
        // score has default: 1.0
        assertThat(read(modelFile("Thing.java")))
                .contains("= 1.0");
    }

    // -------------------------------------------------------------------------
    // Property Javadoc from description
    // -------------------------------------------------------------------------

    @Test
    void modelPropertyHasJavadocFromDescription() throws IOException {
        assertThat(read(modelFile("Thing.java")))
                .contains("Unique identifier");
    }

    // -------------------------------------------------------------------------
    // Parameter-level validation annotations
    // -------------------------------------------------------------------------

    @Test
    void endpointStringParamHasLengthValidation() throws IOException {
        assertThat(read(apiFile("ThingsEndpoint.java")))
                .contains("@Validation.String.Length(min = 1, value = 20)");
    }

    @Test
    void endpointIntParamHasMinMaxValidation() throws IOException {
        String content = read(apiFile("ThingsEndpoint.java"));
        assertThat(content)
                .contains("@Validation.Integer.Min(1)")
                .contains("@Validation.Integer.Max(100)");
    }

    @Test
    void endpointParamValidationImportsValidation() throws IOException {
        assertThat(read(apiFile("ThingsEndpoint.java")))
                .contains("import io.helidon.validation.Validation;");
    }

    @Test
    void apiInterfaceStringParamHasLengthValidation() throws IOException {
        assertThat(read(apiFile("ThingsApi.java")))
                .contains("@Validation.String.Length(min = 1, value = 20)");
    }

    @Test
    void pomHasValidationDependencyForParameterValidation() throws IOException {
        assertThat(read(outputDir.resolve("pom.xml").toFile()))
                .contains("helidon-validation");
    }

    @Test
    void generatedProjectBuildsWithMaven() throws Exception {
        GeneratedProjectBuildSupport.assertMavenPackageSucceeds(outputDir);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private File apiFile(String name) {
        return outputDir.resolve("src/main/java/io/helidon/example/api/" + name).toFile();
    }

    private File modelFile(String name) {
        return outputDir.resolve("src/main/java/io/helidon/example/model/" + name).toFile();
    }

    private String read(File file) throws IOException {
        return Files.readString(file.toPath());
    }
}
