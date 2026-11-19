package com.clbs.portfolio.web;

import com.clbs.portfolio.IntegrationTest;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Keeps api-spec/openapi.yaml and the implementation in sync: the spec must be valid OpenAPI 3, and every
 * operation (method, path, operationId, response codes) must match what springdoc generates from the controllers.
 */
@IntegrationTest
class OpenApiContractTest {

    private static final Path SPEC = Path.of("api-spec/openapi.yaml");

    @Autowired
    private MockMvc mvc;

    @Test
    void specIsValidOpenApi3() throws Exception {
        SwaggerParseResult result = new OpenAPIParser().readContents(Files.readString(SPEC), null, null);
        assertThat(result.getMessages()).isEmpty();
        assertThat(result.getOpenAPI().getOpenapi()).startsWith("3.");
    }

    @Test
    void specMatchesImplementation() throws Exception {
        OpenAPI spec = new OpenAPIParser().readContents(Files.readString(SPEC), null, null).getOpenAPI();
        String generatedJson = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        OpenAPI generated = new OpenAPIParser().readContents(generatedJson, null, null).getOpenAPI();

        assertThat(operations(spec, "/api/v1")).isEqualTo(operations(generated, ""));
    }

    @Test
    void specDocumentsSuccessAndErrorCodesOfEveryOperation() throws Exception {
        OpenAPI spec = new OpenAPIParser().readContents(Files.readString(SPEC), null, null).getOpenAPI();
        spec.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, op) -> {
            Set<String> codes = op.getResponses().keySet();
            assertThat(codes).as("%s %s", method, path).anyMatch(c -> c.startsWith("2"));
            assertThat(codes).as("%s %s", method, path).contains("401");
        }));
    }

    /** "METHOD /full/path" -> operationId. */
    private static Map<String, String> operations(OpenAPI api, String serverPrefix) {
        Map<String, String> ops = new TreeMap<>();
        api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((PathItem.HttpMethod method,
                                                                               Operation op) ->
                ops.put(method + " " + serverPrefix + path, op.getOperationId())));
        return ops;
    }
}
