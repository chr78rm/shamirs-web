package de.christofreichardt.json;

import de.christofreichardt.diagnosis.AbstractTracer;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import javax.json.JsonNumber;
import javax.json.JsonValue;

/**
 *
 * @author Developer
 */
public abstract class JsonNumberConstraint extends JsonValueConstraint {

    final Pattern pattern;
    final BigDecimal minimum;
    final BigDecimal maximum;

    public JsonNumberConstraint(String regex) {
        this(regex, null, null);
    }

    public JsonNumberConstraint(String regex, BigDecimal minimum, BigDecimal maximum) {
        super(true);
        this.pattern = Pattern.compile(regex);
        this.minimum = minimum;
        this.maximum = maximum;
    }

    @Override
    public boolean validate(JsonValue jsonValue) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("boolean", this, "validate(JsonValue jsonValue)");

        try {
            if (!isApplicable(jsonValue)) {
                throw new Exception(String.format("Expected a %s but got a %s.", JsonValue.ValueType.NUMBER, jsonValue.getValueType().name()));
            }
            JsonNumber jsonNumber = (JsonNumber) jsonValue;
            
            tracer.out().printfIndentln("jsonNumber = %s", jsonNumber);
            
            if (!this.pattern.matcher(jsonNumber.toString()).matches()) {
                throw new Exception(String.format("JsonNumber doesn't match '%s'.", this.pattern));
            }
            if (this.maximum != null && this.minimum != null) {
                if (!(jsonNumber.bigDecimalValue().compareTo(this.minimum) >= 0 && jsonNumber.bigDecimalValue().compareTo(this.maximum) < 0)) {
                    throw new Exception(String.format("JsonNumber '%s' is outside the interval [%s,%s[.", jsonNumber.bigDecimalValue(), this.minimum, this.maximum));
                }
            }
            
            return true;
        } finally {
            tracer.wayout();
        }
    }

    @Override
    boolean isApplicable(JsonValue jsonValue) {
        return jsonValue.getValueType() == JsonValue.ValueType.NUMBER;
    }
}
