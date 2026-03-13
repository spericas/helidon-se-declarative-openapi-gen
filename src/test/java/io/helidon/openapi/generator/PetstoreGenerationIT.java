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
 * Integration test — runs the full openapi-generator pipeline against
 * {@code petstore.yaml} and asserts that the expected files are generated
 * with correct content.
 */
class PetstoreGenerationIT {

    @TempDir
    static Path outputDir;

    /** Path to petstore.yaml bundled in test resources. */
    private static String specPath;

    @BeforeAll
    static void generate() throws Exception {
        URL resource = PetstoreGenerationIT.class
                .getClassLoader()
                .getResource("petstore.yaml");
        specPath = Paths.get(resource.toURI()).toAbsolutePath().toString();

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
    // File existence
    // -------------------------------------------------------------------------

    @Test
    void pomXmlIsGenerated() {
        assertThat(outputDir.resolve("pom.xml")).exists();
    }

    @Test
    void mainJavaIsGenerated() {
        assertThat(javaFile("io/helidon/example/Main.java")).exists();
    }

    @Test
    void applicationYamlIsGenerated() {
        assertThat(resourceFile("application.yaml")).exists();
    }

    @Test
    void loggingPropertiesIsGenerated() {
        assertThat(resourceFile("logging.properties")).exists();
    }

    @Test
    void petsEndpointIsGenerated() {
        assertThat(apiFile("PetsEndpoint.java")).exists();
    }

    @Test
    void petsApiIsGenerated() {
        assertThat(apiFile("PetsApi.java")).exists();
    }

    @Test
    void petsClientIsGenerated() {
        assertThat(apiFile("PetsClient.java")).exists();
    }

    @Test
    void petsExceptionIsGenerated() {
        assertThat(apiFile("PetsException.java")).exists();
    }

    @Test
    void petsErrorHandlerIsGenerated() {
        assertThat(apiFile("PetsErrorHandler.java")).exists();
    }

    @Test
    void petModelIsGenerated() {
        assertThat(modelFile("Pet.java")).exists();
    }

    @Test
    void apiErrorModelIsGenerated() {
        // "Error" schema → renamed to "ApiError"
        assertThat(modelFile("ApiError.java")).exists();
    }

    @Test
    void petsEndpointUnitTestIsGenerated() {
        assertThat(apiTestFile("PetsEndpointTest.java")).exists();
    }

    // -------------------------------------------------------------------------
    // PetsEndpoint.java content
    // -------------------------------------------------------------------------

    @Test
    void endpointHasRestServerEndpointAnnotation() throws IOException {
        assertThat(read(apiFile("PetsEndpoint.java")))
                .contains("@RestServer.Endpoint");
    }

    @Test
    void endpointHasServiceSingletonAnnotation() throws IOException {
        assertThat(read(apiFile("PetsEndpoint.java")))
                .contains("@Service.Singleton");
    }

    @Test
    void endpointDoesNotDuplicateHttpPathAnnotation() throws IOException {
        assertThat(read(apiFile("PetsEndpoint.java")))
                .doesNotContain("@Http.Path(\"/pets\")");
    }

    @Test
    void endpointImplementsSharedApi() throws IOException {
        assertThat(read(apiFile("PetsEndpoint.java")))
                .contains("implements PetsApi");
    }

    @Test
    void apiInterfaceListPetsHasGetAnnotation() throws IOException {
        assertThat(read(apiFile("PetsApi.java")))
                .contains("@Http.GET");
    }

    @Test
    void apiInterfaceCreatePetsHasPostAndStatus201() throws IOException {
        String content = read(apiFile("PetsApi.java"));
        assertThat(content)
                .contains("@Http.POST")
                .contains("@RestServer.Status(201)");
    }

    @Test
    void endpointListPetsHasOptionalLimitParam() throws IOException {
        String content = read(apiFile("PetsEndpoint.java"));
        assertThat(content).contains("Optional<Integer>");
        assertThat(content).contains("@Http.QueryParam(\"limit\")");
    }

    @Test
    void endpointListPetsHasComputedHeaderAnnotation() throws IOException {
        // listPets has x-next response header → @RestServer.ComputedHeader annotation
        String content = read(apiFile("PetsEndpoint.java"));
        assertThat(content).contains("@RestServer.ComputedHeader(name = \"x-next\"");
        assertThat(content).doesNotContain("ServerResponse");
    }

    @Test
    void computedHeaderFunctionIsGeneratedAsOwnFile() throws IOException {
        String content = read(apiFile("PetsXNextHeaderFn.java"));
        assertThat(content).contains("@Service.Named(\"xNextHeaderFn\")");
        assertThat(content).contains("class PetsXNextHeaderFn implements Http.HeaderFunction");
        assertThat(content).contains("Optional<Header> apply(HeaderName headerName)");
    }

    @Test
    void computedHeaderFunctionImportsHeaderTypes() throws IOException {
        String content = read(apiFile("PetsXNextHeaderFn.java"));
        assertThat(content).contains("import io.helidon.http.Header;");
        assertThat(content).contains("import io.helidon.http.HeaderName;");
    }

    @Test
    void endpointShowPetByIdHasPathParam() throws IOException {
        assertThat(read(apiFile("PetsEndpoint.java")))
                .contains("@Http.PathParam(\"petId\")");
    }

    @Test
    void endpointListPetsReturnsListOfPet() throws IOException {
        assertThat(read(apiFile("PetsEndpoint.java")))
                .contains("List<Pet>");
    }

    // -------------------------------------------------------------------------
    // PetsApi.java content
    // -------------------------------------------------------------------------

    @Test
    void apiInterfaceIsAnInterface() throws IOException {
        assertThat(read(apiFile("PetsApi.java")))
                .contains("public interface PetsApi");
    }

    @Test
    void apiInterfaceHasHttpPathAnnotation() throws IOException {
        assertThat(read(apiFile("PetsApi.java")))
                .contains("@Http.Path(\"/pets\")");
    }

    // -------------------------------------------------------------------------
    // Generated unit test content
    // -------------------------------------------------------------------------

    @Test
    void endpointUnitTestInstantiatesEndpoint() throws IOException {
        String content = read(apiTestFile("PetsEndpointTest.java"));
        assertThat(content).contains("class PetsEndpointTest");
        assertThat(content).contains("void testListPets()");
        assertThat(content).contains("void testCreatePets()");
        assertThat(content).contains("void testShowPetById()");
        assertThat(content)
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("@ServerTest"),
                        c -> assertThat(c).contains("assertThat(new PetsEndpoint(), notNullValue())"));
    }

    // -------------------------------------------------------------------------
    // Pet.java model content
    // -------------------------------------------------------------------------

    @Test
    void petModelHasCorrectPackage() throws IOException {
        assertThat(read(modelFile("Pet.java")))
                .contains("package io.helidon.example.model;");
    }

    @Test
    void petModelHasJsonEntityAnnotation() throws IOException {
        assertThat(read(modelFile("Pet.java")))
                .contains("@Json.Entity");
    }

    @Test
    void petModelRequiredFieldsHaveJsonRequired() throws IOException {
        String content = read(modelFile("Pet.java"));
        // id and name are required in petstore.yaml
        assertThat(content).contains("@Json.Required");
    }

    @Test
    void petModelHasIdNameTagFields() throws IOException {
        String content = read(modelFile("Pet.java"));
        assertThat(content)
                .contains("Long id")
                .contains("String name")
                .contains("String tag");
    }

    @Test
    void petModelHasGettersAndSetters() throws IOException {
        String content = read(modelFile("Pet.java"));
        assertThat(content)
                .contains("getId()")
                .contains("setId(")
                .contains("getName()")
                .contains("setName(");
    }

    @Test
    void petModelNoSwaggerImports() throws IOException {
        String content = read(modelFile("Pet.java"));
        assertThat(content)
                .doesNotContain("io.swagger.annotations")
                .doesNotContain("ApiModel")
                .doesNotContain("ApiModelProperty");
    }

    // -------------------------------------------------------------------------
    // ApiError.java model content
    // -------------------------------------------------------------------------

    @Test
    void apiErrorModelIsNamedApiError() throws IOException {
        assertThat(read(modelFile("ApiError.java")))
                .contains("public class ApiError");
    }

    @Test
    void apiErrorModelHasCodeAndMessageFields() throws IOException {
        String content = read(modelFile("ApiError.java"));
        assertThat(content)
                .contains("Integer code")
                .contains("String message");
    }

    // -------------------------------------------------------------------------
    // pom.xml content
    // -------------------------------------------------------------------------

    @Test
    void pomXmlContainsHelidonVersion() throws IOException {
        assertThat(read(outputDir.resolve("pom.xml").toFile()))
                .contains("4.4.0");
    }

    @Test
    void pomXmlContainsHelidonWebserver() throws IOException {
        assertThat(read(outputDir.resolve("pom.xml").toFile()))
                .contains("helidon-webserver");
    }

    @Test
    void pomXmlContainsJsonBindingDependency() throws IOException {
        assertThat(read(outputDir.resolve("pom.xml").toFile()))
                .contains("helidon-http-media-json-binding");
    }

    @Test
    void pomXmlContainsJUnitDependency() throws IOException {
        assertThat(read(outputDir.resolve("pom.xml").toFile()))
                .contains("<artifactId>junit-jupiter</artifactId>");
    }

    @Test
    void pomXmlContainsServerTestDependency() throws IOException {
        assertThat(read(outputDir.resolve("pom.xml").toFile()))
                .contains("<artifactId>helidon-webserver-testing-junit5</artifactId>");
    }

    @Test
    void pomXmlContainsWebclientApiDependency() throws IOException {
        assertThat(read(outputDir.resolve("pom.xml").toFile()))
                .contains("<artifactId>helidon-webclient-api</artifactId>");
    }

    @Test
    void pomXmlDisablesShadePlugin() throws IOException {
        String pom = read(outputDir.resolve("pom.xml").toFile());
        assertThat(pom)
                .contains("<artifactId>maven-shade-plugin</artifactId>")
                .contains("<skip>true</skip>");
    }

    @Test
    void clientHasDeclarativeEndpointAnnotation() throws IOException {
        assertThat(read(apiFile("PetsClient.java")))
                .contains("@RestClient.Endpoint(\"${app.client.endpoint:http://localhost:8080}\")");
    }

    // -------------------------------------------------------------------------
    // Main.java content
    // -------------------------------------------------------------------------

    @Test
    void mainJavaHasGenerateBindingAnnotation() throws IOException {
        assertThat(read(javaFile("io/helidon/example/Main.java")))
                .contains("@Service.GenerateBinding");
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

    private File apiTestFile(String name) {
        return outputDir.resolve("src/test/java/io/helidon/example/api/" + name).toFile();
    }

    @Test
    void openapiYamlIsGenerated() {
        assertThat(outputDir.resolve("src/main/resources/META-INF/openapi.yaml")).exists();
    }

    @Test
    void openapiYamlContainsSpecContent() throws IOException {
        String content = Files.readString(
                outputDir.resolve("src/main/resources/META-INF/openapi.yaml"));
        assertThat(content)
                .contains("openapi:")
                .contains("paths:")
                .contains("/pets")
                .doesNotContain("null");
    }

    @Test
    void applicationTestYamlIsGeneratedWithServerTestEndpoint() throws IOException {
        String content = Files.readString(
                outputDir.resolve("src/test/resources/application-test.yaml"));
        assertThat(content)
                .contains("app:")
                .contains("endpoint: \"http://localhost:${test.server.port}\"");
    }

    private File javaFile(String relativePath) {
        return outputDir.resolve("src/main/java/" + relativePath).toFile();
    }

    private File resourceFile(String name) {
        return outputDir.resolve("src/main/resources/" + name).toFile();
    }

    private String read(File file) throws IOException {
        return Files.readString(file.toPath());
    }
}
