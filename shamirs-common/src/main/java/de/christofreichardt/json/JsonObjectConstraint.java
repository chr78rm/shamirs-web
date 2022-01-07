package de.christofreichardt.json;

import java.util.Map;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 *
 * @author Developer
 */
public class JsonObjectConstraint extends JsonAnyObjectConstraint {

    final Map<String, JsonValueConstraint> constraints;

    public JsonObjectConstraint(Map<String, JsonValueConstraint> constraints) {
        super(true);
        this.constraints = constraints;
    }

    @Override
    public boolean validate(JsonValue jsonValue) {
        if (!isApplicable(jsonValue)) {
            throw new Exception(String.format("Expected a %s but got a %s.", JsonValue.ValueType.OBJECT, jsonValue.getValueType().name()));
        }
        JsonObject jsonObject = jsonValue.asJsonObject();

        this.constraints.entrySet().forEach(entry -> {
            String name = entry.getKey();
            JsonValueConstraint constraint = entry.getValue();
            if (constraint.isRequired()) {
                if (!jsonObject.containsKey(name)) {
                    throw new JsonValueConstraint.Exception(String.format("Required attribut '%s' missing.", name));
                }
                try {
                    constraint.validate(jsonObject.get(name));
                } catch (JsonValueConstraint.Exception ex) {
                    throw new JsonValueConstraint.Exception(String.format("Attribut '%s' violates constraint: %s", name, ex.getMessage()));
                }
            } else {
                if (jsonObject.containsKey(name)) {
                    try {
                        constraint.validate(jsonObject.get(name));
                    } catch (JsonValueConstraint.Exception ex) {
                        throw new JsonValueConstraint.Exception(String.format("Attribut %s violates constraint: %s", name, ex.getMessage()));
                    }
                }
            }
        });

        return true;
    }

}
