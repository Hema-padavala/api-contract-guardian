
package com.apiguardian.service;

import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

@Service
public class SpecificationComparisonService {

    public Set<String> findAddedEndpoints(
            Set<String> oldEndpoints,
            Set<String> newEndpoints) {

        Set<String> added = new TreeSet<>(newEndpoints);
        added.removeAll(oldEndpoints);
        return added;
    }

    public Set<String> findRemovedEndpoints(
            Set<String> oldEndpoints,
            Set<String> newEndpoints) {

        Set<String> removed = new TreeSet<>(oldEndpoints);
        removed.removeAll(newEndpoints);
        return removed;
    }

    public Set<String> findBreakingChanges(
            Set<String> oldEndpoints,
            Set<String> newEndpoints) {

        Set<String> breakingChanges = new TreeSet<>(oldEndpoints);
        breakingChanges.removeAll(newEndpoints);
        return breakingChanges;
    }

    public Set<String> findAddedOperations(
            Set<String> oldOperations,
            Set<String> newOperations) {

        Set<String> added = new TreeSet<>(newOperations);
        added.removeAll(oldOperations);
        return added;
    }

    public Set<String> findRemovedOperations(
            Set<String> oldOperations,
            Set<String> newOperations) {

        Set<String> removed = new TreeSet<>(oldOperations);
        removed.removeAll(newOperations);
        return removed;
    }

public Set<String> findAddedParameters(
        Set<String> oldParameters,
        Set<String> newParameters) {

    Set<String> oldIdentities = new java.util.HashSet<>();

    for (String parameter : oldParameters) {
        oldIdentities.add(getParameterIdentity(parameter));
    }

    Set<String> added = new java.util.HashSet<>();

    for (String parameter : newParameters) {
        if (!oldIdentities.contains(getParameterIdentity(parameter))) {
            added.add(parameter);
        }
    }

    return added;
}

public Set<String> findRemovedParameters(
        Set<String> oldParameters,
        Set<String> newParameters) {

    Set<String> newIdentities = new java.util.HashSet<>();

    for (String parameter : newParameters) {
        newIdentities.add(getParameterIdentity(parameter));
    }

    Set<String> removed = new java.util.HashSet<>();

    for (String parameter : oldParameters) {
        if (!newIdentities.contains(getParameterIdentity(parameter))) {
            removed.add(parameter);
        }
    }

    return removed;
}

public Set<String> findNewlyRequiredParameters(
        Set<String> oldParameters,
        Set<String> newParameters) {

    Set<String> oldParameterMap = new java.util.HashSet<>();
    Set<String> newlyRequired = new java.util.HashSet<>();

    for (String parameter : oldParameters) {
        if (parameter.endsWith("| required=false")) {
            oldParameterMap.add(getParameterIdentity(parameter));
        }
    }

    for (String parameter : newParameters) {
        if (parameter.endsWith("| required=true")
                && oldParameterMap.contains(getParameterIdentity(parameter))) {
            newlyRequired.add(parameter);
        }
    }

    return newlyRequired;
}

private String getParameterIdentity(String parameter) {
    return parameter.replaceFirst("\\s*\\| required=(true|false)$", "");
}

    public String generateCompatibilityReport(
            Set<String> oldEndpoints,
            Set<String> newEndpoints) {

        Set<String> added =
                findAddedEndpoints(oldEndpoints, newEndpoints);

        Set<String> removed =
                findRemovedEndpoints(oldEndpoints, newEndpoints);

        Set<String> breaking =
                findBreakingChanges(oldEndpoints, newEndpoints);

        StringBuilder report = new StringBuilder();

        report.append("API COMPATIBILITY REPORT\n");
        report.append("========================\n\n");

        report.append("Old API endpoint count: ")
                .append(oldEndpoints.size()).append("\n");

        report.append("New API endpoint count: ")
                .append(newEndpoints.size()).append("\n\n");

        report.append("Added endpoints: ")
                .append(added).append("\n");

        report.append("Removed endpoints: ")
                .append(removed).append("\n");

        report.append("Potentially breaking changes: ")
                .append(breaking).append("\n\n");

        if (!breaking.isEmpty()) {
            report.append(
                    "COMPATIBILITY STATUS: BREAKING CHANGES DETECTED\n");
            report.append(
                    "Reason: Existing API endpoints have been removed.\n");
        } else {
            report.append(
                    "COMPATIBILITY STATUS: NO REMOVED ENDPOINTS DETECTED\n");
        }

        return report.toString();
    }


public String generateDetailedCompatibilityReport(
        Set<String> oldEndpoints,
        Set<String> newEndpoints,
        Set<String> oldOperations,
        Set<String> newOperations) {

    StringBuilder report = new StringBuilder();

    report.append(generateCompatibilityReport(
            oldEndpoints, newEndpoints));

    Set<String> addedOperations =
            findAddedOperations(oldOperations, newOperations);

    Set<String> removedOperations =
            findRemovedOperations(oldOperations, newOperations);

    Set<String> addedEndpoints =
            findAddedEndpoints(oldEndpoints, newEndpoints);

    Set<String> removedEndpoints =
            findRemovedEndpoints(oldEndpoints, newEndpoints);

    report.append("\nHTTP OPERATION COMPARISON\n");
    report.append("=========================\n\n");

    report.append("Old API operation count: ")
            .append(oldOperations.size()).append("\n");

    report.append("New API operation count: ")
            .append(newOperations.size()).append("\n\n");

    report.append("Added operations: ")
            .append(addedOperations).append("\n");

    report.append("Removed operations: ")
            .append(removedOperations).append("\n\n");

    if (!removedOperations.isEmpty()) {
        report.append(
                "OPERATION STATUS: BREAKING CHANGES DETECTED\n");
        report.append(
                "Reason: Existing HTTP operations have been removed.\n");
    } else {
        report.append(
                "OPERATION STATUS: NO REMOVED OPERATIONS DETECTED\n");
    }

    report.append("\nSEVERITY DETAILS\n");
    report.append("================\n");

    for (String endpoint : removedEndpoints) {
        report.append("[")
                .append(getSeverity("REMOVED_ENDPOINT"))
                .append("] Removed endpoint: ")
                .append(endpoint)
                .append("\n");
    }

    for (String operation : removedOperations) {
        report.append("[")
                .append(getSeverity("REMOVED_OPERATION"))
                .append("] Removed operation: ")
                .append(operation)
                .append("\n");
    }

    for (String endpoint : addedEndpoints) {
        report.append("[")
                .append(getSeverity("ADDED_ENDPOINT"))
                .append("] Added endpoint: ")
                .append(endpoint)
                .append("\n");
    }

    for (String operation : addedOperations) {
        report.append("[")
                .append(getSeverity("ADDED_OPERATION"))
                .append("] Added operation: ")
                .append(operation)
                .append("\n");
    }

    if (removedEndpoints.isEmpty()
            && removedOperations.isEmpty()
            && addedEndpoints.isEmpty()
            && addedOperations.isEmpty()) {
        report.append("No endpoint or operation changes detected.\n");
    }

    return report.toString();
}



public String generateFullCompatibilityReport(
        Set<String> oldEndpoints,
        Set<String> newEndpoints,
        Set<String> oldOperations,
        Set<String> newOperations,
        Set<String> oldParameters,
        Set<String> newParameters) {

    StringBuilder report = new StringBuilder();

    report.append(generateDetailedCompatibilityReport(
            oldEndpoints, newEndpoints, oldOperations, newOperations));

    Set<String> addedParameters =
            findAddedParameters(oldParameters, newParameters);

    Set<String> removedParameters =
            findRemovedParameters(oldParameters, newParameters);

    Set<String> newlyRequiredParameters =
            findNewlyRequiredParameters(oldParameters, newParameters);

    report.append("\nREQUEST PARAMETER COMPARISON\n");
    report.append("============================\n\n");

    report.append("Added parameters: ")
            .append(addedParameters).append("\n");

    report.append("Removed parameters: ")
            .append(removedParameters).append("\n");

    report.append("Newly required parameters: ")
            .append(newlyRequiredParameters).append("\n\n");

    if (!newlyRequiredParameters.isEmpty()) {
        report.append(
                "PARAMETER STATUS: BREAKING CHANGES DETECTED\n");
        report.append(
                "Reason: Previously optional parameters are now required.\n");
    } else {
        report.append(
                "PARAMETER STATUS: NO NEWLY REQUIRED PARAMETERS DETECTED\n");
    }

    report.append("\nSEVERITY DETAILS\n");
    report.append("================\n");

    for (String parameter : newlyRequiredParameters) {
        report.append("[")
                .append(getSeverity("NEWLY_REQUIRED_PARAMETER"))
                .append("] Newly required parameter: ")
                .append(parameter)
                .append("\n");
    }

    for (String parameter : removedParameters) {
        report.append("[")
                .append(getSeverity("REMOVED_PARAMETER"))
                .append("] Removed parameter: ")
                .append(parameter)
                .append("\n");
    }

    for (String parameter : addedParameters) {
        report.append("[")
                .append(getSeverity("ADDED_PARAMETER"))
                .append("] Added parameter: ")
                .append(parameter)
                .append("\n");
    }

    if (newlyRequiredParameters.isEmpty()
            && removedParameters.isEmpty()
            && addedParameters.isEmpty()) {
        report.append("No parameter changes detected.\n");
    }

    return report.toString();
}


public Map<String, Object> generateStructuredCompatibilityReport(
        Set<String> oldEndpoints,
        Set<String> newEndpoints,
        Set<String> oldOperations,
        Set<String> newOperations,
        Set<String> oldParameters,
        Set<String> newParameters) {

    Map<String, Object> report = new LinkedHashMap<>();

    List<Map<String, String>> changes = new ArrayList<>();

    Set<String> removedEndpoints =
            findRemovedEndpoints(oldEndpoints, newEndpoints);

    Set<String> addedEndpoints =
            findAddedEndpoints(oldEndpoints, newEndpoints);

    Set<String> removedOperations =
            findRemovedOperations(oldOperations, newOperations);

    Set<String> addedOperations =
            findAddedOperations(oldOperations, newOperations);

    Set<String> removedParameters =
            findRemovedParameters(oldParameters, newParameters);

    Set<String> addedParameters =
            findAddedParameters(oldParameters, newParameters);

    Set<String> newlyRequiredParameters =
            findNewlyRequiredParameters(oldParameters, newParameters);

    for (String item : removedEndpoints) {
        changes.add(Map.of(
                "type", "REMOVED_ENDPOINT",
                "description", item,
                "severity", getSeverity("REMOVED_ENDPOINT")));
    }

    for (String item : addedEndpoints) {
        changes.add(Map.of(
                "type", "ADDED_ENDPOINT",
                "description", item,
                "severity", getSeverity("ADDED_ENDPOINT")));
    }

    for (String item : removedOperations) {
        changes.add(Map.of(
                "type", "REMOVED_OPERATION",
                "description", item,
                "severity", getSeverity("REMOVED_OPERATION")));
    }

    for (String item : addedOperations) {
        changes.add(Map.of(
                "type", "ADDED_OPERATION",
                "description", item,
                "severity", getSeverity("ADDED_OPERATION")));
    }

    for (String item : newlyRequiredParameters) {
        changes.add(Map.of(
                "type", "NEWLY_REQUIRED_PARAMETER",
                "description", item,
                "severity", getSeverity("NEWLY_REQUIRED_PARAMETER")));
    }

    for (String item : removedParameters) {
        changes.add(Map.of(
                "type", "REMOVED_PARAMETER",
                "description", item,
                "severity", getSeverity("REMOVED_PARAMETER")));
    }

    for (String item : addedParameters) {
        changes.add(Map.of(
                "type", "ADDED_PARAMETER",
                "description", item,
                "severity", getSeverity("ADDED_PARAMETER")));
    }

    boolean breakingChanges =
            !removedEndpoints.isEmpty()
            || !removedOperations.isEmpty()
            || !newlyRequiredParameters.isEmpty()
            || !removedParameters.isEmpty();

    report.put("status", breakingChanges
            ? "BREAKING_CHANGES_DETECTED"
            : "COMPATIBLE");

    report.put("oldEndpointCount", oldEndpoints.size());
    report.put("newEndpointCount", newEndpoints.size());

    report.put("oldOperationCount", oldOperations.size());
    report.put("newOperationCount", newOperations.size());

    report.put("totalChanges", changes.size());
    report.put("changes", changes);

    return report;
}


    public String getSeverity(String changeType) {
        if (changeType == null) {
            return "UNKNOWN";
        }

        switch (changeType.toUpperCase()) {
            case "REMOVED_ENDPOINT":
            case "REMOVED_OPERATION":
            case "NEWLY_REQUIRED_PARAMETER":
                return "HIGH";

            case "MODIFIED_PARAMETER":
            case "MODIFIED_OPERATION":
                return "MEDIUM";

            case "ADDED_ENDPOINT":
            case "ADDED_OPERATION":
            case "ADDED_PARAMETER":
                return "LOW";
                
            case "REMOVED_PARAMETER":
                return "HIGH";

            default:
                return "UNKNOWN";
        }
    }
}
