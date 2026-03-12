package io.helidon.openapi.generator;

import java.io.IOException;
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
 * Integration test for security code generation.
 * Uses a spec that has operations with security requirements to verify
 * that @RoleValidator.Roles, SecurityContext, and security dependencies are generated.
 */
class SecurityGenerationIT {

    @TempDir
    static Path outputDir;

    @BeforeAll
    static void generate() throws Exception {
        var resource = SecurityGenerationIT.class.getClassLoader().getResource("secured-api.yaml");
        String specPath = Paths.get(resource.toURI()).toAbsolutePath().toString();

        CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("helidon-se-declarative")
                .setInputSpec(specPath)
                .setOutputDir(outputDir.toString())
                .addAdditionalProperty("apiPackage", "io.helidon.example.api")
                .addAdditionalProperty("modelPackage", "io.helidon.example.model")
                .addAdditionalProperty("invokerPackage", "io.helidon.example");

        new DefaultGenerator().opts(configurator.toClientOptInput()).generate();
    }

    // -------------------------------------------------------------------------
    // Endpoint: SecurityContext and API contract implementation
    // -------------------------------------------------------------------------

    @Test
    void endpoint_implementsApi_contract() throws IOException {
        assertThat(read(apiFile("ItemsEndpoint.java")))
                .contains("implements ItemsApi");
    }

    @Test
    void endpoint_securedMethod_hasSecurityContextParam() throws IOException {
        // createItem is secured → SecurityContext injected as unannotated param
        assertThat(read(apiFile("ItemsEndpoint.java")))
                .contains("SecurityContext securityContext");
    }

    @Test
    void endpoint_multiRoleMethod_hasArrayAnnotation() throws IOException {
        // deleteItem has security: [{basicAuth: [admin, moderator]}] on the API interface
        assertThat(read(apiFile("ItemsApi.java")))
                .contains("@RoleValidator.Roles({\"admin\", \"moderator\"})");
    }

    @Test
    void apiInterface_securedMethod_hasRoleValidatorAnnotation() throws IOException {
        // createItem has security: [{basicAuth: [admin]}]
        assertThat(read(apiFile("ItemsApi.java")))
                .contains("@RoleValidator.Roles(\"admin\")");
    }

    @Test
    void endpoint_hasSecurityImports() throws IOException {
        String content = read(apiFile("ItemsEndpoint.java"));
        assertThat(content).contains("import io.helidon.security.SecurityContext;");
    }

    // -------------------------------------------------------------------------
    // Interface: @RoleValidator.Roles, no SecurityContext
    // -------------------------------------------------------------------------

    @Test
    void interface_securedMethod_hasRoleValidatorAnnotation() throws IOException {
        assertThat(read(apiFile("ItemsApi.java")))
                .contains("@RoleValidator.Roles(\"admin\")");
    }

    @Test
    void interface_hasNoSecurityContextParam() throws IOException {
        // SecurityContext is server-side only — must not appear in the interface
        assertThat(read(apiFile("ItemsApi.java")))
                .doesNotContain("SecurityContext");
    }

    @Test
    void interface_hasRoleValidatorImport() throws IOException {
        assertThat(read(apiFile("ItemsApi.java")))
                .contains("import io.helidon.security.abac.role.RoleValidator;");
    }

    // -------------------------------------------------------------------------
    // pom.xml: security dependencies
    // -------------------------------------------------------------------------

    @Test
    void pom_hasHelidonSecurityDependency() throws IOException {
        assertThat(read(outputDir.resolve("pom.xml").toFile().toPath()))
                .contains("helidon-security")
                .contains("helidon-webserver-security")
                .contains("helidon-security-abac-role")
                .contains("helidon-security-providers-abac");
    }

    // -------------------------------------------------------------------------
    // application.yaml: security config section
    // -------------------------------------------------------------------------

    @Test
    void applicationYaml_hasSecurity_showsRequiredComment() throws IOException {
        assertThat(read(outputDir.resolve("src/main/resources/application.yaml")))
                .contains("required: one or more operations use @RoleValidator.Roles");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private java.io.File apiFile(String name) {
        return outputDir.resolve("src/main/java/io/helidon/example/api/" + name).toFile();
    }

    private String read(Path path) throws IOException {
        return Files.readString(path);
    }

    private String read(java.io.File file) throws IOException {
        return Files.readString(file.toPath());
    }
}
