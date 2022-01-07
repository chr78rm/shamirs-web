package de.christofreichardt.json;

import javax.json.JsonValue;

/**
 *
 * @author Developer
 */
public class JsonAnyObjectConstraint extends JsonStructureConstraint {

    public JsonAnyObjectConstraint() {
        super(true);
    }

    public JsonAnyObjectConstraint(boolean required) {
        super(required);
    }

    @Override
    boolean validate(JsonValue jsonValue) {
        if (!isApplicable(jsonValue)) {
            throw new Exception(String.format("Expected a %s but got a %s.", JsonValue.ValueType.OBJECT, jsonValue.getValueType().name()));
        }

        return true;
    }

    @Override
    boolean isApplicable(JsonValue jsonValue) {
        return jsonValue.getValueType() == JsonValue.ValueType.OBJECT;
    }

}
