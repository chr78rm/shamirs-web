package de.christofreichardt.json;

import de.christofreichardt.diagnosis.AbstractTracer;
import java.util.regex.Pattern;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 *
 * @author Developer
 */
public abstract class JsonStringConstraint extends JsonValueConstraint {

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
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("boolean", this, "validate(JsonValue jsonValue)");

        try {
            if (!isApplicable(jsonValue)) {
                throw new Exception(String.format("Expected a %s but got a %s.", JsonValue.ValueType.STRING, jsonValue.getValueType().name()));
            }
            JsonString jsonString = (JsonString) jsonValue;
            
            tracer.out().printfIndentln("jsonString = %s", jsonString.getString());
            
            if (!this.pattern.matcher(jsonString.getString()).matches()) {
                throw new JsonValueConstraint.Exception(String.format("JsonString doesn't match '%s'.", this.pattern));
            }
            
            return true;
        } finally {
            tracer.wayout();
        }
    }

    @Override
    boolean isApplicable(JsonValue jsonValue) {
        return jsonValue.getValueType() == JsonValue.ValueType.STRING;
    }
    
}
