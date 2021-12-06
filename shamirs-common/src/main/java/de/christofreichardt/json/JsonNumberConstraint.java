package de.christofreichardt.json;

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

    public JsonNumberConstraint(boolean required, String regex) {
        this(required, regex, null, null);
    }

    public JsonNumberConstraint(String regex, BigDecimal minimum, BigDecimal maximum) {
        super(true);
        this.pattern = Pattern.compile(regex);
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public JsonNumberConstraint(boolean required, String regex, BigDecimal minimum, BigDecimal maximum) {
        super(required);
        this.pattern = Pattern.compile(regex);
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public JsonNumberConstraint(BigDecimal minimum, BigDecimal maximum) {
        super(true);
        this.pattern = null;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public JsonNumberConstraint(boolean required, BigDecimal minimum, BigDecimal maximum) {
        super(required);
        this.pattern = null;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    @Override
    public boolean validate(JsonValue jsonValue) {
        if (!isApplicable(jsonValue)) {
            throw new Exception(String.format("Expected a %s but got a %s.", JsonValue.ValueType.NUMBER, jsonValue.getValueType().name()));
        }
        JsonNumber jsonNumber = (JsonNumber) jsonValue;

        if (this.pattern != null && !this.pattern.matcher(jsonNumber.toString()).matches()) {
            throw new Exception(String.format("JsonNumber doesn't match '%s'.", this.pattern));
        }

        if (this.maximum != null && this.minimum != null) {
            if (!(jsonNumber.bigDecimalValue().compareTo(this.minimum) >= 0 && jsonNumber.bigDecimalValue().compareTo(this.maximum) < 0)) {
                throw new Exception(String.format("JsonNumber '%s' is outside the interval [%s,%s[.", jsonNumber.bigDecimalValue(), this.minimum, this.maximum));
            }
        } else if (this.minimum != null) {
            if (!(jsonNumber.bigDecimalValue().compareTo(this.minimum) >= 0)) {
                throw new Exception(String.format("JsonNumber '%s' is below the minimum %s.", jsonNumber.bigDecimalValue(), this.minimum));
            }
        } else if (this.maximum != null) {
            if (!(jsonNumber.bigDecimalValue().compareTo(this.maximum) < 0)) {
                throw new Exception(String.format("JsonNumber '%s' is above or equals the maximum %s.", jsonNumber.bigDecimalValue(), this.minimum));
            }
        }

        return true;
    }

    @Override
    boolean isApplicable(JsonValue jsonValue) {
        return jsonValue.getValueType() == JsonValue.ValueType.NUMBER;
    }
}
