package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.json.JsonValueConstraint;
import java.math.BigDecimal;
import java.util.Map;
import javax.json.JsonValue;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
public class KeystoreInstructionsValidator extends JsonValidator {

    final MyJsonNumberConstraint sharesConstraint = new MyJsonNumberConstraint("[1-9][0-9]?", new BigDecimal(4), new BigDecimal(21));
    final MyJsonNumberConstraint thresholdConstraint = new MyJsonNumberConstraint("[1-9]{1}", new BigDecimal(2), new BigDecimal(11));
    final MyJsonStringConstraint descriptiveNameConstraint = new MyJsonStringConstraint("[A-Za-z[0-9]-]{5,100}");

    final MyJsonStringConstraint jsonAliasConstraint = new MyJsonStringConstraint("[A-Za-z0-9-]{8,30}");

    final MyJsonStringConstraint jsonSecretKeyAlgoConstraint = new MyJsonStringConstraint("AES|HmacSHA512|ChaCha20");
    final MyJsonNumberConstraint jsonSecretKeySizeConstraint = new MyJsonNumberConstraint("128|256|512");
    final MyJsonStringConstraint jsonSecretKeyTypeConstraint = new MyJsonStringConstraint("secret-key");
    MyJsonObjectConstraint secretKeyInfoConstraint = new MyJsonObjectConstraint(
            Map.of("alias", jsonAliasConstraint, "algorithm", jsonSecretKeyAlgoConstraint, "keySize", jsonSecretKeySizeConstraint, "type", jsonSecretKeyTypeConstraint)
    );

    final MyJsonStringConstraint jsonPrivateKeyAlgoConstraint = new MyJsonStringConstraint("EC|DSA|RSA");
    final MyJsonStringConstraint jsonPrivateKeyTypeConstraint = new MyJsonStringConstraint("private-key");
    final MyJsonNumberConstraint validityConstraint = new MyJsonNumberConstraint("[1-9][0-9]{2,3}");
    final MyJsonStringConstraint commonNameConstraint = new MyJsonStringConstraint("[A-Za-z ]{8,30}");
    final MyJsonStringConstraint localityConstraint = new MyJsonStringConstraint("[A-Za-z- ]{3,50}");
    final MyJsonStringConstraint stateConstraint = new MyJsonStringConstraint("[A-Za-z- ]{3,50}");
    final MyJsonStringConstraint countryConstraint = new MyJsonStringConstraint("[A-Za-z- ]{3,50}");
    final MyJsonObjectConstraint x509Constraint = new MyJsonObjectConstraint(
            Map.of("validity", validityConstraint, "commonName", commonNameConstraint, "locality", localityConstraint, "state", stateConstraint, "country", countryConstraint)
    );
    final MyJsonObjectConstraint privateKeyInfoConstraint = new MyJsonObjectConstraint(
            Map.of("alias", jsonAliasConstraint, "algorithm", jsonPrivateKeyAlgoConstraint, "type", jsonPrivateKeyTypeConstraint, "x509", x509Constraint)
    );

    final MyJsonArrayConstraint keyInfosConstraint = new MyJsonArrayConstraint(1, 10, secretKeyInfoConstraint, privateKeyInfoConstraint);

    final MyJsonNumberConstraint sizeConstraint = new MyJsonNumberConstraint("[1-9][0-9]?", new BigDecimal(1), new BigDecimal(11));
    final MyJsonStringConstraint usernameConstraint = new MyJsonStringConstraint("[A-Za-z0-9-]{8,100}");
    final MyJsonObjectConstraint sizeObjConstraint = new MyJsonObjectConstraint(
            Map.of("size", sizeConstraint, "participant", usernameConstraint)
    );

    final MyJsonArrayConstraint sizesConstraint = new MyJsonArrayConstraint(4, 10, sizeObjConstraint);

    final MyJsonObjectConstraint keystoreInstructionsConstraint = new MyJsonObjectConstraint(
            Map.of("shares", sharesConstraint, "threshold", thresholdConstraint, "descriptiveName", descriptiveNameConstraint, "keyinfos", keyInfosConstraint, "sizes", sizesConstraint)
    );

    @Override
    boolean check(JsonValue keystoreInstructions) {
        return this.keystoreInstructionsConstraint.validate(keystoreInstructions);
    }

}
