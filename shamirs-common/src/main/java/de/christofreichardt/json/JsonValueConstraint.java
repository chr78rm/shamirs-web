package de.christofreichardt.json;

import javax.json.JsonValue;

/**
 *
 * @author Developer
 */
abstract public class JsonValueConstraint {
    
    public static class Exception extends RuntimeException {

        public Exception() {
        }

        public Exception(String message) {
            super(message);
        }
        
    }
    
    private final boolean required;

    public JsonValueConstraint(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    abstract boolean validate(JsonValue jsonValue);
    abstract boolean isApplicable(JsonValue jsonValue);
}
