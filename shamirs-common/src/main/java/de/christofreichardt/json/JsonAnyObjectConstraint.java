package de.christofreichardt.json;

import de.christofreichardt.diagnosis.AbstractTracer;
import javax.json.JsonValue;

/**
 *
 * @author Developer
 */
abstract public class JsonAnyObjectConstraint extends JsonValueConstraint {

    public JsonAnyObjectConstraint() {
        super(true);
    }

    @Override
    boolean validate(JsonValue jsonValue) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("boolean", this, "validate(JsonValue jsonValue)");

        try {
            if (!isApplicable(jsonValue)) {
                throw new Exception(String.format("Expected a %s but got a %s.", JsonValue.ValueType.OBJECT, jsonValue.getValueType().name()));
            }
            
            return true;
        } finally {
            tracer.wayout();
        }
    }

    @Override
    boolean isApplicable(JsonValue jsonValue) {
        return jsonValue.getValueType() == JsonValue.ValueType.OBJECT;
    }
    
}
