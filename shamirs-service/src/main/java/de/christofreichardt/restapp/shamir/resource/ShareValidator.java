package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import java.util.Map;
import javax.json.JsonValue;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
public class ShareValidator extends JsonValidator {

    final String bigIntegerPattern = "[1-9][0-9]*";
    final MyJsonNumberConstraint bigIntegerConstraint = new MyJsonNumberConstraint(bigIntegerPattern);
    final String thresholdPattern = "[1-9][0-9]?";
    final MyJsonNumberConstraint thresholdConstraint = new MyJsonNumberConstraint(thresholdPattern);
    final MyJsonObjectConstraint sharePointContraint = new MyJsonObjectConstraint(
            Map.of("x", bigIntegerConstraint, "y", bigIntegerConstraint)
    );
    final MyJsonObjectConstraint wrapConstraint = new MyJsonObjectConstraint(
            Map.of("SharePoint", sharePointContraint)
    );
    final MyJsonArrayConstraint sharePointsConstraint = new MyJsonArrayConstraint(1, 20, wrapConstraint);
    final MyJsonObjectConstraint shareConstraint = new MyJsonObjectConstraint(
            Map.of("PartitionId", uuidConstraint, "Prime", bigIntegerConstraint, "Threshold", thresholdConstraint, "SharePoints", sharePointsConstraint)
    );

    @Override
    boolean check(JsonValue share) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("boolean", this, "validated(JsonObject share)");

        try {
            return this.shareConstraint.validate(share);
        } finally {
            tracer.wayout();
        }
    }

}
