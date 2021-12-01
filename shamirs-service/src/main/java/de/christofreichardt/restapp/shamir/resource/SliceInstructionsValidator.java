package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.restapp.shamir.common.SliceProcessingState;
import java.util.Map;
import javax.json.JsonValue;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
public class SliceInstructionsValidator extends JsonValidator {

    final String statePattern = SliceProcessingState.FETCHED.name() + "|" + SliceProcessingState.POSTED.name();
    final MyJsonStringConstraint stateConstraint = new MyJsonStringConstraint(statePattern);
    final MyJsonObjectConstraint sliceConstraint = new MyJsonObjectConstraint(
            Map.of("id", uuidConstraint, "state", stateConstraint, "share", new MyJsonAnyObjectConstraint())
    );

    @Override
    boolean check(JsonValue instructions) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("boolean", this, "check(JsonValue jsonValue)");

        try {

            return this.sliceConstraint.validate(instructions);
        } finally {
            tracer.wayout();
        }
    }

}
