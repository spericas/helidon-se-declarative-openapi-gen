package io.helidon.openapi.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HelidonSeDeclarativeCodegen} — verifies naming conventions,
 * metadata, and template registration without running the full generation pipeline.
 */
class HelidonSeDeclarativeCodegenTest {

    private HelidonSeDeclarativeCodegen codegen;

    @BeforeEach
    void setUp() {
        codegen = new HelidonSeDeclarativeCodegen();
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Test
    void getNameReturnsCorrectId() {
        assertThat(codegen.getName()).isEqualTo("helidon-se-declarative");
    }

    @Test
    void getTagIsServer() {
        assertThat(codegen.getTag()).isEqualTo(CodegenType.SERVER);
    }

    @Test
    void getHelpIsNotBlank() {
        assertThat(codegen.getHelp()).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // toApiName
    // -------------------------------------------------------------------------

    @Test
    void toApiNameSimpleTagReturnsCamelCase() {
        assertThat(codegen.toApiName("pets")).isEqualTo("Pets");
    }

    @Test
    void toApiNameEmptyTagReturnsDefault() {
        assertThat(codegen.toApiName("")).isEqualTo("Default");
    }

    @Test
    void toApiNameNullTagReturnsDefault() {
        assertThat(codegen.toApiName(null)).isEqualTo("Default");
    }

    @Test
    void toApiNameHyphenatedTagReturnsCamelCase() {
        assertThat(codegen.toApiName("pet-store")).isEqualTo("PetStore");
    }

    @Test
    void toApiNameMultiWordTagReturnsCamelCase() {
        assertThat(codegen.toApiName("store orders")).isEqualTo("StoreOrders");
    }

    // -------------------------------------------------------------------------
    // toModelName
    // -------------------------------------------------------------------------

    @Test
    void toModelNameErrorMappedToApiError() {
        // "Error" clashes with java.lang.Error — must be remapped
        assertThat(codegen.toModelName("Error")).isEqualTo("ApiError");
    }

    @Test
    void toModelNamePetUnchanged() {
        assertThat(codegen.toModelName("Pet")).isEqualTo("Pet");
    }

    // -------------------------------------------------------------------------
    // apiFilename
    // -------------------------------------------------------------------------

    @Test
    void apiFilenameApiMustacheProducesEndpointJava() {
        String filename = codegen.apiFilename("api.mustache", "pets");
        assertThat(filename).endsWith("PetsEndpoint.java");
    }

    @Test
    void apiFilenameApiInterfaceMustacheProducesApiJava() {
        String filename = codegen.apiFilename("api-interface.mustache", "pets");
        assertThat(filename).endsWith("PetsApi.java");
    }

    @Test
    void apiFilenameRestClientMustacheProducesClientJava() {
        String filename = codegen.apiFilename("restClient.mustache", "pets");
        assertThat(filename).endsWith("PetsClient.java");
    }

    @Test
    void apiFilenameApiExceptionMustacheProducesExceptionJava() {
        String filename = codegen.apiFilename("apiException.mustache", "pets");
        assertThat(filename).endsWith("PetsException.java");
    }

    @Test
    void apiFilenameErrorHandlerMustacheProducesErrorHandlerJava() {
        String filename = codegen.apiFilename("errorHandler.mustache", "pets");
        assertThat(filename).endsWith("PetsErrorHandler.java");
    }

    // -------------------------------------------------------------------------
    // Template registration
    // -------------------------------------------------------------------------

    @Test
    void constructorRegistersApiAndApiInterfaceTemplates() {
        assertThat(codegen.apiTemplateFiles())
                .containsKey("api.mustache")
                .containsKey("api-interface.mustache");
    }

    @Test
    void constructorRegistersModelTemplate() {
        assertThat(codegen.modelTemplateFiles()).containsKey("model.mustache");
    }

    @Test
    void constructorClearsDocTemplatesAndRegistersUnitTestTemplates() {
        assertThat(codegen.modelDocTemplateFiles()).isEmpty();
        assertThat(codegen.apiDocTemplateFiles()).isEmpty();
        assertThat(codegen.apiTestTemplateFiles()).containsEntry("api-test.mustache", ".java");
        assertThat(codegen.modelTestTemplateFiles()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Security role annotation value formatting
    // -------------------------------------------------------------------------

    @Test
    void singleSecurityRoleFormattedAsQuotedString() {
        // x-roles-annotation-value for a single role: "admin"
        // Verify via fromOperation by checking the vendor extension directly
        io.swagger.v3.oas.models.Operation op = new io.swagger.v3.oas.models.Operation();
        op.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                .addList("basicAuth", java.util.List.of("admin")));
        org.openapitools.codegen.CodegenOperation cop =
                codegen.fromOperation("/items", "get", op, java.util.List.of());
        assertThat(cop.vendorExtensions)
                .containsEntry("x-roles-annotation-value", "\"admin\"");
    }

    @Test
    void multipleSecurityRolesFormattedAsArray() {
        io.swagger.v3.oas.models.Operation op = new io.swagger.v3.oas.models.Operation();
        op.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                .addList("basicAuth", java.util.List.of("admin", "moderator")));
        org.openapitools.codegen.CodegenOperation cop =
                codegen.fromOperation("/items", "delete", op, java.util.List.of());
        assertThat(cop.vendorExtensions)
                .containsEntry("x-roles-annotation-value", "{\"admin\", \"moderator\"}");
    }

    @Test
    void noSecurityNoSecurityVendorExtensions() {
        io.swagger.v3.oas.models.Operation op = new io.swagger.v3.oas.models.Operation();
        org.openapitools.codegen.CodegenOperation cop =
                codegen.fromOperation("/items", "get", op, java.util.List.of());
        assertThat(cop.vendorExtensions)
                .doesNotContainKey("x-has-security-roles")
                .doesNotContainKey("x-roles-annotation-value");
    }

    // -------------------------------------------------------------------------
    // Defaults
    // -------------------------------------------------------------------------

    @Test
    void defaultPackagesMatchExpectedValues() {
        assertThat(codegen.apiPackage()).isEqualTo("io.helidon.example.api");
        assertThat(codegen.modelPackage()).isEqualTo("io.helidon.example.model");
        assertThat(codegen.getInvokerPackage()).isEqualTo("io.helidon.example");
    }
}
