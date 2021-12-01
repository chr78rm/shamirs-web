package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.restapp.shamir.common.SessionPhase;
import java.math.BigDecimal;
import java.util.Map;
import javax.json.JsonValue;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
public class SessionInstructionsValidator extends JsonValidator {

    final String phasePattern = SessionPhase.ACTIVE.name() + "|" + SessionPhase.CLOSED.name();
    final MyJsonStringConstraint phaseConstraint = new MyJsonStringConstraint(phasePattern);
    final String idleTimePattern = "[1-9][0-9]{0,3}";
    final BigDecimal min = new BigDecimal("1"), max = new BigDecimal("3601");
    final MyJsonNumberConstraint idleTimeConstraint = new MyJsonNumberConstraint(false, idleTimePattern, min, max);
    final MyJsonObjectConstraint sessionConstraint = new MyJsonObjectConstraint(
            Map.of("id", uuidConstraint, "phase", phaseConstraint, "idleTime", idleTimeConstraint)
    );

    @Override
    boolean check(JsonValue sessionInstructions) {
        return this.sessionConstraint.validate(sessionInstructions);
    }

}
