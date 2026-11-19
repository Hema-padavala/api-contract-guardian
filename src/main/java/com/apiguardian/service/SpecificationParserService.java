
package com.apiguardian.service;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

@Service
public class SpecificationParserService {

    public Set<String> extractEndpoints(String jsonContent) {
        OpenAPI openAPI = parseSpecification(jsonContent);
        return new TreeSet<>(openAPI.getPaths().keySet());
    }

    public Set<String> extractOperations(String jsonContent) {
        OpenAPI openAPI = parseSpecification(jsonContent);
        Set<String> operations = new TreeSet<>();

        for (Map.Entry<String, PathItem> entry
                : openAPI.getPaths().entrySet()) {

            String path = entry.getKey();
            PathItem pathItem = entry.getValue();

            for (PathItem.HttpMethod method
                    : pathItem.readOperationsMap().keySet()) {
                operations.add(method.name() + " " + path);
            }
        }

        return operations;
    }

    public Set<String> extractParameters(String jsonContent) {
        OpenAPI openAPI = parseSpecification(jsonContent);
        Set<String> parameters = new TreeSet<>();

        for (Map.Entry<String, PathItem> entry
                : openAPI.getPaths().entrySet()) {

            String path = entry.getKey();
            PathItem pathItem = entry.getValue();

            for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry
                    : pathItem.readOperationsMap().entrySet()) {

                String method = operationEntry.getKey().name();
                Operation operation = operationEntry.getValue();

                if (operation.getParameters() != null) {
                    for (Parameter parameter : operation.getParameters()) {
                        parameters.add(
                                method + " " + path
                                + " | " + parameter.getIn()
                                + " | " + parameter.getName()
                                + " | required=" + parameter.getRequired());
                    }
                }
            }

            if (pathItem.getParameters() != null) {
                for (Parameter parameter : pathItem.getParameters()) {
                    parameters.add(
                            "ALL " + path
                            + " | " + parameter.getIn()
                            + " | " + parameter.getName()
                            + " | required=" + parameter.getRequired());
                }
            }
        }

        return parameters;
    }

    private OpenAPI parseSpecification(String jsonContent) {
        SwaggerParseResult result =
                new OpenAPIV3Parser()
                        .readContents(jsonContent, null, null);

        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null || openAPI.getPaths() == null) {
            throw new IllegalArgumentException(
                    "Invalid OpenAPI specification.");
        }

        return openAPI;
    }
}
