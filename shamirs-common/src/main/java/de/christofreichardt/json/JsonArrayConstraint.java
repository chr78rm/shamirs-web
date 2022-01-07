package de.christofreichardt.json;

import javax.json.JsonArray;
import javax.json.JsonValue;

/**
 *
 * @author Developer
 */
public class JsonArrayConstraint extends JsonStructureConstraint {

    final int maxSize;
    final int minSize;
    JsonValueConstraint[] constraints;

    public JsonArrayConstraint(int minSize, int maxSize, JsonValueConstraint... constraints) {
        super(true);
        this.maxSize = maxSize;
        this.minSize = minSize;
        this.constraints = constraints;
    }

    @Override
    boolean validate(JsonValue jsonValue) {
        if (!isApplicable(jsonValue)) {
            throw new Exception(String.format("Expected a %s but got a %s.", JsonValue.ValueType.ARRAY, jsonValue.getValueType().name()));
        }

        JsonArray jsonArray = jsonValue.asJsonArray();
        if (jsonArray.size() > this.maxSize) {
            throw new Exception(String.format("Array exceeds maximum size %d.", this.maxSize));
        }
        if (jsonArray.size() < this.minSize) {
            throw new Exception(String.format("Array falls below the minimum size %d.", this.minSize));
        }

        jsonArray.forEach(value -> {
            boolean validated = false;
            JsonValueConstraint.Exception cause = new JsonValueConstraint.Exception();
            for (JsonValueConstraint jsonValueConstraint : this.constraints) {
                try {
                    validated = jsonValueConstraint.validate(value);
                    if (validated) {
                        break;
                    }
                } catch (JsonValueConstraint.Exception ex) {
                    cause = ex;
                }
            }
            if (!validated) {
                throw new JsonValueConstraint.Exception(String.format("JsonValue '%s' doesn't match any of the given constraints for this array: %s", value, cause.getMessage()));
            }
        });

        return true;
    }

    @Override
    boolean isApplicable(JsonValue jsonValue) {
        return jsonValue.getValueType() == JsonValue.ValueType.ARRAY;
    }

}
