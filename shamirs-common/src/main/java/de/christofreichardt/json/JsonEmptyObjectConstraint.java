package de.christofreichardt.json;

import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 *
 * @author Developer
 */
public class JsonEmptyObjectConstraint extends JsonAnyObjectConstraint {

    public JsonEmptyObjectConstraint() {
        super(true);
    }

    @Override
    boolean validate(JsonValue jsonValue) {
        if (!isApplicable(jsonValue)) {
            throw new Exception(String.format("Expected a %s but got a %s.", JsonValue.ValueType.OBJECT, jsonValue.getValueType().name()));
        }
        JsonObject jsonObject = jsonValue.asJsonObject();

        if (!jsonObject.entrySet().isEmpty()) {
            throw new JsonValueConstraint.Exception("Expected an empty object.");
        }

        return true;
    }

}
