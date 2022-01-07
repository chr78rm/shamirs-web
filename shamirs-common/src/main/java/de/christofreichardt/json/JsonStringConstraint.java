package de.christofreichardt.json;

import java.util.regex.Pattern;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 *
 * @author Developer
 */
public class JsonStringConstraint extends JsonValueConstraint {

    final Pattern pattern;

    public JsonStringConstraint(String regex) {
        this(true, regex);
    }

    public JsonStringConstraint(boolean required, String regex) {
        super(required);
        this.pattern = Pattern.compile(regex);
    }

    @Override
    public boolean validate(JsonValue jsonValue) {
        if (!isApplicable(jsonValue)) {
            throw new Exception(String.format("Expected a %s but got a %s.", JsonValue.ValueType.STRING, jsonValue.getValueType().name()));
        }
        JsonString jsonString = (JsonString) jsonValue;

        if (!this.pattern.matcher(jsonString.getString()).matches()) {
            throw new JsonValueConstraint.Exception(String.format("JsonString doesn't match '%s'.", this.pattern));
        }

        return true;
    }

    @Override
    boolean isApplicable(JsonValue jsonValue) {
        return jsonValue.getValueType() == JsonValue.ValueType.STRING;
    }

}
