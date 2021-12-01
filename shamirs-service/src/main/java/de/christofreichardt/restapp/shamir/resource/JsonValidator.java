package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.json.JsonAnyObjectConstraint;
import de.christofreichardt.json.JsonArrayConstraint;
import de.christofreichardt.json.JsonNumberConstraint;
import de.christofreichardt.json.JsonObjectConstraint;
import de.christofreichardt.json.JsonStringConstraint;
import de.christofreichardt.json.JsonValueConstraint;
import java.math.BigDecimal;
import java.util.Map;
import javax.json.JsonValue;

/**
 *
 * @author Developer
 */
public abstract class JsonValidator implements Traceable {

    final String uuidPattern = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}";
    final MyJsonStringConstraint uuidConstraint = new MyJsonStringConstraint(uuidPattern);
    
    class MyJsonStringConstraint extends JsonStringConstraint {

        public MyJsonStringConstraint(String regex) {
            super(regex);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return JsonValidator.this.getCurrentTracer();
        }

    }

    class MyJsonObjectConstraint extends JsonObjectConstraint {

        public MyJsonObjectConstraint(Map<String, JsonValueConstraint> constraints) {
            super(constraints);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return JsonValidator.this.getCurrentTracer();
        }

    }
    
    class MyJsonAnyObjectConstraint extends JsonAnyObjectConstraint {

        public MyJsonAnyObjectConstraint() {
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return JsonValidator.this.getCurrentTracer();
        }
    }

    class MyJsonNumberConstraint extends JsonNumberConstraint {

        public MyJsonNumberConstraint(String regex) {
            super(regex);
        }

        public MyJsonNumberConstraint(String regex, BigDecimal minimum, BigDecimal maximum) {
            super(regex, minimum, maximum);
        }

        public MyJsonNumberConstraint(boolean required, String regex, BigDecimal minimum, BigDecimal maximum) {
            super(required, regex, minimum, maximum);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return JsonValidator.this.getCurrentTracer();
        }

    }
    
    class MyJsonArrayConstraint extends JsonArrayConstraint {

        public MyJsonArrayConstraint(int minSize, int maxSize, JsonValueConstraint... constraints) {
            super(minSize, maxSize, constraints);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return JsonValidator.this.getCurrentTracer();
        }
    }
    
    abstract boolean check(JsonValue jsonValue);

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }
    
}
