
package com.apiguardian.service;

import org.springframework.stereotype.Service;

@Service
public class ChangeExplanationService {

    public String explainChange(String type, String description) {

        switch (type) {

            case "REMOVED_ENDPOINT":
                return "What changed: The endpoint "
                        + description + " was removed.\n"
                        + "Why it is breaking: Existing applications "
                        + "may still call this endpoint.\n"
                        + "Impact: Those API requests may return "
                        + "404 Not Found.\n"
                        + "Suggested fix: Restore the endpoint or "
                        + "provide an alternative endpoint.";

            case "REMOVED_OPERATION":
                return "What changed: The operation "
                        + description + " was removed.\n"
                        + "Why it is breaking: Existing clients may "
                        + "depend on this HTTP method.\n"
                        + "Impact: Requests using this operation "
                        + "may fail.\n"
                        + "Suggested fix: Restore the operation or "
                        + "update clients to use a replacement.";

            case "NEWLY_REQUIRED_PARAMETER":
                return "What changed: A previously optional "
                        + "parameter is now required.\n"
                        + "Why it is breaking: Older clients may not "
                        + "send this parameter.\n"
                        + "Impact: Requests may fail validation.\n"
                        + "Suggested fix: Keep the parameter optional "
                        + "or update clients to provide it.";

            case "REMOVED_PARAMETER":
                return "What changed: A parameter was removed.\n"
                        + "Why it is breaking: Existing clients may "
                        + "still send this parameter.\n"
                        + "Impact: Client requests may no longer "
                        + "match the API contract.\n"
                        + "Suggested fix: Restore the parameter "
                        + "or provide a migration path.";
            

            case "MODIFIED_METHOD":
                return "Exact change:\n" + description
                        + "\nImpact: The method's implementation has changed."
                        + "\nLimitation: Caller impact cannot be determined from the supplied code."
                        + "\nSuggested action: Review the changed statements and verify the method's expected behavior.";
            case "RETURN_TYPE_CHANGED":
                return "Exact change:\n" + description
                        + "\nImpact: The method now has a different return type."
                        + "\nLimitation: Caller compatibility cannot be determined from the supplied code."
                        + "\nSuggested action: Check all usages of this method and update incompatible assignments if necessary.";
            case "ENDPOINT_IMPLEMENTATION_CHANGED":
                return "Exact change:\n" + description
                        + "\nImpact: The endpoint's internal logic has changed."
                        + "\nLimitation: The effect on API consumers cannot be determined from the supplied code alone."
                        + "\nSuggested action: Review the changed statements and test the endpoint.";

            default:
                return "What changed: " + description
                        + "\nFurther analysis is required.";
        }
    }
}
