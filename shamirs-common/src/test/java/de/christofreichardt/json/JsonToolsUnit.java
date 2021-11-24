package de.christofreichardt.json;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.restapp.shamir.common.SliceProcessingState;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 *
 * @author Developer
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JsonToolsUnit implements Traceable, WithAssertions {

    class MyJsonStringConstraint extends JsonStringConstraint {

        public MyJsonStringConstraint(String regex) {
            super(regex);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return JsonToolsUnit.this.getCurrentTracer();
        }

    }

    class MyJsonNumberConstraint extends JsonNumberConstraint {

        public MyJsonNumberConstraint(String regex) {
            super(regex);
        }

        public MyJsonNumberConstraint(String regex, BigDecimal minimum, BigDecimal maximum) {
            super(regex, minimum, maximum);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return JsonToolsUnit.this.getCurrentTracer();
        }

    }

    class MyJsonObjectConstraint extends JsonObjectConstraint {

        public MyJsonObjectConstraint(Map<String, JsonValueConstraint> constraints) {
            super(constraints);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return JsonToolsUnit.this.getCurrentTracer();
        }

    }
    
    class MyJsonArrayConstraint extends JsonArrayConstraint {

        public MyJsonArrayConstraint(int minSize, int maxSize, JsonValueConstraint... constraints) {
            super(minSize, maxSize, constraints);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return JsonToolsUnit.this.getCurrentTracer();
        }
    }
    
    class MyJsonAnyObjectConstraint extends JsonAnyObjectConstraint {

        public MyJsonAnyObjectConstraint() {
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return JsonToolsUnit.this.getCurrentTracer();
        }
    }

    @Test
    void jsonStringConstraint() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "jsonStringConstraint()");

        try {
            String userNamePattern = "[A-Za-z0-9-]{8,20}";
            final MyJsonStringConstraint jsonUsernameConstraint = new MyJsonStringConstraint(userNamePattern);

            String[] validUserNames = {"christof", "test-user-0"};
            String[] invalidUserNames = {"minor", "-------superior-------", "test_user_1"};

            assertThat(
                    Arrays.stream(validUserNames)
                            .peek(validUserName -> tracer.out().printfIndentln("validUserName = %s", validUserName))
                            .allMatch(validUserName -> jsonUsernameConstraint.validate(Json.createValue(validUserName)))
            ).isTrue();

            Arrays.stream(invalidUserNames)
                    .peek(invalidUserName -> tracer.out().printfIndentln("validUserName = %s", invalidUserName))
                    .forEach(invalidUserName -> {
                        assertThatThrownBy(
                                () -> jsonUsernameConstraint.validate(Json.createValue(invalidUserName))
                        ).isInstanceOf(JsonValueConstraint.Exception.class);
                    });

            assertThatThrownBy(
                    () -> new MyJsonStringConstraint("[0-9]+").validate(Json.createValue(new BigInteger("1024")))
            )
                    .isInstanceOf(JsonValueConstraint.Exception.class)
                    .hasMessage("Expected a STRING but got a NUMBER.");
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void jsonNumberConstraint() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "jsonNumberConstraint()");

        try {
            String numberPattern = "[1-9][0-9]?";
            BigDecimal min = new BigDecimal("4"), max = new BigDecimal("12.1");
            final MyJsonNumberConstraint jsonNumberConstraint = new MyJsonNumberConstraint(numberPattern, min, max);

            BigInteger[] validNumbers = {new BigInteger("5"), new BigInteger("10"), new BigInteger("12")};
            BigInteger[] invalidNumbers = {new BigInteger("0"), new BigInteger("13")};

            assertThat(
                    Arrays.stream(validNumbers)
                            .peek(validNumber -> tracer.out().printfIndentln("validNumber = %s", validNumber))
                            .allMatch(validNumber -> jsonNumberConstraint.validate(Json.createValue(validNumber)))
            ).isTrue();

            Arrays.stream(invalidNumbers)
                    .peek(invalidNumber -> tracer.out().printfIndentln("invalidNumber = %s", invalidNumber))
                    .forEach(invalidNumber -> {
                        assertThatThrownBy(
                                () -> jsonNumberConstraint.validate(Json.createValue(invalidNumber))
                        ).isInstanceOf(JsonValueConstraint.Exception.class);
                    });
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void jsonSecretKeyInfoConstraint() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "jsonSecretKeyInfoConstraint()");

        try {
            String aliasPattern = "[A-Za-z0-9-]{8,30}";
            final MyJsonStringConstraint jsonAliasConstraint = new MyJsonStringConstraint(aliasPattern);
            String algoPattern = "AES";
            final MyJsonStringConstraint jsonAlgoConstraint = new MyJsonStringConstraint(algoPattern);
            String keysizePattern = "128|256|512";
            final MyJsonNumberConstraint jsonKeySizeConstraint = new MyJsonNumberConstraint(keysizePattern);
            String keyTypePattern = "secret-key";
            final MyJsonStringConstraint jsonKeyTypeConstraint = new MyJsonStringConstraint(keyTypePattern);

            MyJsonObjectConstraint secretKeyInfoConstraint = new MyJsonObjectConstraint(
                    Map.of("alias", jsonAliasConstraint, "algorithm", jsonAlgoConstraint, "keySize", jsonKeySizeConstraint, "type", jsonKeyTypeConstraint)
            );

            JsonObject[] validKeyInfos = {
                Json.createObjectBuilder()
                    .add("alias", "my-secret-key")
                    .add("algorithm", "AES")
                    .add("keySize", 256)
                    .add("type", "secret-key")
                    .build(),
                Json.createObjectBuilder()
                    .add("alias", "my-aes-key")
                    .add("algorithm", "AES")
                    .add("keySize", 128)
                    .add("type", "secret-key")
                    .build(),
                Json.createObjectBuilder()
                    .add("alias", "my-aes-key")
                    .add("algorithm", "AES")
                    .add("keySize", 128)
                    .add("type", "secret-key")
                    .add("ext", "some-stuff")
                    .build(),
            };

            JsonObject[] invalidKeyInfos = {
                Json.createObjectBuilder()
                    .add("alias", "my-secret-key")
                    .add("algorithm", "AES")
                    .add("keySize", 128)
                    .add("type", "private-key")
                    .build(),
                Json.createObjectBuilder()
                    .add("alias", "my-des-key")
                    .add("algorithm", "DES")
                    .add("keySize", 64)
                    .add("type", "secret-key")
                    .build(),
                Json.createObjectBuilder()
                    .add("algorithm", "AES")
                    .add("keySize", 128)
                    .add("type", "secret-key")
                    .build(),
                Json.createObjectBuilder()
                    .add("alias", "my-secret-key")
                    .add("algorithm", "AES")
                    .add("keySize", "128")
                    .add("type", "secret-key")
                    .build(),
            };

            assertThat(
                    Arrays.stream(validKeyInfos).allMatch(validKeyInfo -> secretKeyInfoConstraint.validate(validKeyInfo))
            ).isTrue();

            Arrays.stream(invalidKeyInfos)
                    .forEach(invalidKeyInfo -> {
                        try {
                            secretKeyInfoConstraint.validate(invalidKeyInfo);
                            Assertions.fail();
                        } catch (Exception ex) {
                            tracer.out().printfIndentln("==> %s", ex.getMessage());
                            assertThat(ex).isInstanceOf(JsonValueConstraint.Exception.class);
                        }
                    });
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void jsonPrivateKeyInfoConstraint() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "jsonPrivateKeyInfoConstraint()");

        try {
            String aliasPattern = "[A-Za-z0-9-]{8,30}";
            final MyJsonStringConstraint jsonAliasConstraint = new MyJsonStringConstraint(aliasPattern);
            String algoPattern = "EC|DSA|RSA";
            final MyJsonStringConstraint jsonAlgoConstraint = new MyJsonStringConstraint(algoPattern);
            String keyTypePattern = "private-key";
            final MyJsonStringConstraint jsonKeyTypeConstraint = new MyJsonStringConstraint(keyTypePattern);

            String validityPattern = "[1-9][0-9]{2,3}";
            final MyJsonNumberConstraint validityConstraint = new MyJsonNumberConstraint(validityPattern);
            String commonNamePattern = "[A-Za-z ]{8,30}";
            final MyJsonStringConstraint commonNameConstraint = new MyJsonStringConstraint(commonNamePattern);
            String localityPattern = "[A-Za-z-]{3,50}";
            final MyJsonStringConstraint localityConstraint = new MyJsonStringConstraint(localityPattern);
            String statePattern = "[A-Za-z-]{3,50}";
            final MyJsonStringConstraint stateConstraint = new MyJsonStringConstraint(statePattern);
            String countryPattern = "[A-Za-z-]{3,50}";
            final MyJsonStringConstraint countryConstraint = new MyJsonStringConstraint(countryPattern);

            MyJsonObjectConstraint x509Constraint = new MyJsonObjectConstraint(
                    Map.of("validity", validityConstraint, "commonName", commonNameConstraint, "locality", localityConstraint, "state", stateConstraint, "country", countryConstraint)
            );

            MyJsonObjectConstraint privateKeyInfoConstraint = new MyJsonObjectConstraint(
                    Map.of("alias", jsonAliasConstraint, "algorithm", jsonAlgoConstraint, "type", jsonKeyTypeConstraint, "x509", x509Constraint)
            );

            JsonObject[] validKeyInfos = {
                Json.createObjectBuilder()
                    .add("alias", "donalds-private-ec-key")
                    .add("algorithm", "EC")
                    .add("type", "private-key")
                    .add("x509", Json.createObjectBuilder()
                        .add("validity", 100)
                        .add("commonName", "Donald Duck")
                        .add("locality", "Entenhausen")
                        .add("state", "Bayern")
                        .add("country", "Deutschland"))
                    .build(),
                Json.createObjectBuilder()
                    .add("alias", "dagoberts-private-dsa-key")
                    .add("algorithm", "DSA")
                    .add("type", "private-key")
                    .add("x509", Json.createObjectBuilder()
                        .add("validity", 100)
                        .add("commonName", "Dagobert Duck")
                        .add("locality", "Entenhausen")
                        .add("state", "Bayern")
                        .add("country", "Deutschland"))
                    .build(),
                Json.createObjectBuilder()
                    .add("alias", "daisies-private-rsa-key")
                    .add("algorithm", "RSA")
                    .add("type", "private-key")
                    .add("x509", Json.createObjectBuilder()
                        .add("validity", 100)
                        .add("commonName", "Daisy Duck")
                        .add("locality", "Entenhausen")
                        .add("state", "Bayern")
                        .add("country", "Deutschland"))
                    .build(),
            };

            assertThat(
                    Arrays.stream(validKeyInfos).allMatch(validKeyInfo -> privateKeyInfoConstraint.validate(validKeyInfo))
            ).isTrue();

            JsonObject[] invalidKeyInfos = {
                Json.createObjectBuilder()
                    .add("algorithm", "EC")
                    .add("type", "private-key")
                    .add("x509", Json.createObjectBuilder()
                        .add("validity", 100)
                        .add("commonName", "Donald Duck")
                        .add("locality", "Entenhausen")
                        .add("state", "Bayern")
                        .add("country", "Deutschland"))
                    .build(),
                Json.createObjectBuilder()
                    .add("alias", "dag")
                    .add("algorithm", "DSA")
                    .add("type", "private-key")
                    .add("x509", Json.createObjectBuilder()
                        .add("validity", 100)
                        .add("commonName", "Dagobert Duck")
                        .add("locality", "Entenhausen")
                        .add("state", "Bayern")
                        .add("country", "Deutschland"))
                    .build(),
                Json.createObjectBuilder()
                    .add("alias", "daisies-private-rsa-key")
                    .add("algorithm", "RSA")
                    .add("type", "private-key")
                    .add("x509", Json.createObjectBuilder()
                        .add("validity", 10000)
                        .add("commonName", "Daisy Duck")
                        .add("locality", "Entenhausen")
                        .add("state", "Bayern")
                        .add("country", "Deutschland"))
                    .build(),
                Json.createObjectBuilder()
                    .add("alias", "daisies-private-rsa-key")
                    .add("algorithm", "RSA")
                    .add("type", "private-key")
                    .add("x509", "test")
                    .build(),
            };

            Arrays.stream(invalidKeyInfos)
                    .forEach(invalidKeyInfo -> {
                        try {
                            privateKeyInfoConstraint.validate(invalidKeyInfo);
                            Assertions.fail();
                        } catch (Exception ex) {
                            tracer.out().printfIndentln("==> %s", ex.getMessage());
                            assertThat(ex).isInstanceOf(JsonValueConstraint.Exception.class);
                        }
                    });
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void sizesConstraint() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "sizesConstraint()");

        try {
            String numberPattern = "[1-9][0-9]?";
            BigDecimal min = new BigDecimal("1"), max = new BigDecimal("10.1");
            final MyJsonNumberConstraint sizeConstraint = new MyJsonNumberConstraint(numberPattern, min, max);
            
            String userNamePattern = "[A-Za-z0-9-]{8,20}";
            final MyJsonStringConstraint usernameConstraint = new MyJsonStringConstraint(userNamePattern);

            MyJsonObjectConstraint sizeObjConstraint = new MyJsonObjectConstraint(
                    Map.of("size", sizeConstraint, "participant", usernameConstraint)
            );
            JsonValueConstraint[] constraints = {sizeObjConstraint};
            MyJsonArrayConstraint sizesConstraint = new MyJsonArrayConstraint(4, 10, constraints);
            
            JsonArray[] validSizes = {
                Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("size", 4)
                        .add("participant", "christof")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 2)
                        .add("participant", "test-user-1")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 2)
                        .add("participant", "test-user-2")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-3")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-4")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-5")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-6")
                    )
                .build(),
                Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("size", 2)
                        .add("participant", "christof")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 2)
                        .add("participant", "test-user-1")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-3")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-4")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-5")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-6")
                    )
                .build(),
            };

            assertThat(
                    Arrays.stream(validSizes).allMatch(validSize -> sizesConstraint.validate(validSize))
            ).isTrue();
            
            JsonArray[] invalidSizes = {
                Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("size", 12)
                        .add("participant", "christof")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 2)
                        .add("participant", "test-user-1")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 2)
                        .add("participant", "test-user-2")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-3")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-4")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-5")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-6")
                    )
                .build(),
                Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("size", 2)
                        .add("participant", "chr")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 2)
                        .add("participant", "test-user-1")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-3")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-4")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-5")
                    )
                    .add(Json.createObjectBuilder()
                        .add("size", 1)
                        .add("participant", "test-user-6")
                    )
                .build(),
            };

            Arrays.stream(invalidSizes)
                    .forEach(invalidSize -> {
                        try {
                            sizesConstraint.validate(invalidSize);
                            Assertions.fail();
                        } catch (Exception ex) {
                            tracer.out().printfIndentln("==> %s", ex.getMessage());
                            assertThat(ex).isInstanceOf(JsonValueConstraint.Exception.class);
                        }
                    });
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void shareConstraint() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "shareConstraint()");

        try {
            String uuidPattern = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}";
            MyJsonStringConstraint uuidConstraint = new MyJsonStringConstraint(uuidPattern);
//            String statePattern = SliceProcessingState.CREATED.name() + "|"
//                    + SliceProcessingState.EXPIRED.name() + "|"
//                    + SliceProcessingState.FETCHED.name() + "|" 
//                    + SliceProcessingState.POSTED.name();
//            MyJsonStringConstraint stateConstraint = new MyJsonStringConstraint(statePattern);
            String bigIntegerPattern = "[1-9][0-9]*";
            MyJsonNumberConstraint bigIntegerConstraint = new MyJsonNumberConstraint(bigIntegerPattern);
            String thresholdPattern = "[1-9][0-9]?";
            MyJsonNumberConstraint thresholdConstraint = new MyJsonNumberConstraint(thresholdPattern);
            MyJsonObjectConstraint sharePointContraint = new MyJsonObjectConstraint(
                    Map.of("x", bigIntegerConstraint, "y", bigIntegerConstraint)
            );
            MyJsonObjectConstraint wrapConstraint = new MyJsonObjectConstraint(
                    Map.of("SharePoint", sharePointContraint)
            );
            MyJsonArrayConstraint sharePointsConstraint = new MyJsonArrayConstraint(1, 20, wrapConstraint);
            MyJsonObjectConstraint shareConstraint = new MyJsonObjectConstraint(
                    Map.of("PartitionId", uuidConstraint, "Prime", bigIntegerConstraint, "Threshold", thresholdConstraint, "SharePoints", sharePointsConstraint)
            );
            
            JsonObject validShare = Json.createObjectBuilder()
                    .add("PartitionId", "4c8817d8-cafd-4dff-8538-02567071fbea")
                    .add("Prime", new BigInteger("20578480839578433406690946154785465029530804091460622363478793090835305100929087"))
                    .add("Threshold", 4)
                    .add("SharePoints", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("SharePoint", Json.createObjectBuilder()
                                            .add("x", new BigInteger("5343537088766651467237183685639144915303458993519496215604768621258607770190557"))
                                            .add("y", new BigInteger("12123040870242525799199784089760842359830853555007955848266717787238675882765492"))
                                    )
                            )
                            .add(Json.createObjectBuilder()
                                    .add("SharePoint", Json.createObjectBuilder()
                                            .add("x", new BigInteger("4992587400096589644966411458474479840083891127803171906966615570472454048811046"))
                                            .add("y", new BigInteger("3331447472596458512646320851992615013974771345825613648005571050286395467356219"))
                                    )
                            )
                            .add(Json.createObjectBuilder()
                                    .add("SharePoint", Json.createObjectBuilder()
                                            .add("x", new BigInteger("4776132650646788302694822741410760125034403808856592636268649697857192978140080"))
                                            .add("y", new BigInteger("2489889427894710311098404634701006983293811668071372095790060111933668950408234"))
                                    )
                            )
                            .add(Json.createObjectBuilder()
                                    .add("SharePoint", Json.createObjectBuilder()
                                            .add("x", new BigInteger("11817840164080823997574382054580179660869802129441618659560820293465203931452873"))
                                            .add("y", new BigInteger("15622273729109725244555139052709355747200820317138470284340626448011133037374039"))
                                    )
                            )
                    )
                    .build();
            
            assertThat(
                    shareConstraint.validate(validShare)
            ).isTrue();

            JsonObject[] invalidShares
                    = {
                        Json.createObjectBuilder()
                                .add("PartitionId", "4c8817d8-cafd-4dff-8538-02567071fbea")
                                .add("Prime", new BigInteger("20578480839578433406690946154785465029530804091460622363478793090835305100929087"))
                                .add("Threshold", 4)
                                .add("SharePoints", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigInteger("5343537088766651467237183685639144915303458993519496215604768621258607770190557"))
                                                        .add("z", new BigInteger("12123040870242525799199784089760842359830853555007955848266717787238675882765492"))
                                                )
                                        )
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigInteger("4992587400096589644966411458474479840083891127803171906966615570472454048811046"))
                                                        .add("y", new BigInteger("3331447472596458512646320851992615013974771345825613648005571050286395467356219"))
                                                )
                                        )
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigInteger("4776132650646788302694822741410760125034403808856592636268649697857192978140080"))
                                                        .add("y", new BigInteger("2489889427894710311098404634701006983293811668071372095790060111933668950408234"))
                                                )
                                        )
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigInteger("11817840164080823997574382054580179660869802129441618659560820293465203931452873"))
                                                        .add("y", new BigInteger("15622273729109725244555139052709355747200820317138470284340626448011133037374039"))
                                                )
                                        )
                                )
                                .build(),
                        Json.createObjectBuilder()
                                .add("PartitionId", "4c8817d8-cafd-4dFf-8538-02567071fbea")
                                .add("Prime", new BigInteger("20578480839578433406690946154785465029530804091460622363478793090835305100929087"))
                                .add("Threshold", 4)
                                .add("SharePoints", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigInteger("5343537088766651467237183685639144915303458993519496215604768621258607770190557"))
                                                        .add("y", new BigInteger("12123040870242525799199784089760842359830853555007955848266717787238675882765492"))
                                                )
                                        )
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigInteger("4992587400096589644966411458474479840083891127803171906966615570472454048811046"))
                                                        .add("y", new BigInteger("3331447472596458512646320851992615013974771345825613648005571050286395467356219"))
                                                )
                                        )
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigInteger("4776132650646788302694822741410760125034403808856592636268649697857192978140080"))
                                                        .add("y", new BigInteger("2489889427894710311098404634701006983293811668071372095790060111933668950408234"))
                                                )
                                        )
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigInteger("11817840164080823997574382054580179660869802129441618659560820293465203931452873"))
                                                        .add("y", new BigInteger("15622273729109725244555139052709355747200820317138470284340626448011133037374039"))
                                                )
                                        )
                                )
                                .build(),
                        Json.createObjectBuilder()
                                .add("PartitionId", "4c8817d8-cafd-4daf-8538-02567071fbea")
                                .add("Prime", new BigInteger("20578480839578433406690946154785465029530804091460622363478793090835305100929087"))
                                .add("Threshold", 4)
                                .add("SharePoints", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigDecimal("534353708876665146723718368563914491530345.8993519496215604768621258607770190557"))
                                                        .add("y", new BigInteger("12123040870242525799199784089760842359830853555007955848266717787238675882765492"))
                                                )
                                        )
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigInteger("4992587400096589644966411458474479840083891127803171906966615570472454048811046"))
                                                        .add("y", new BigInteger("3331447472596458512646320851992615013974771345825613648005571050286395467356219"))
                                                )
                                        )
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigInteger("4776132650646788302694822741410760125034403808856592636268649697857192978140080"))
                                                        .add("y", new BigInteger("2489889427894710311098404634701006983293811668071372095790060111933668950408234"))
                                                )
                                        )
                                        .add(Json.createObjectBuilder()
                                                .add("SharePoint", Json.createObjectBuilder()
                                                        .add("x", new BigInteger("11817840164080823997574382054580179660869802129441618659560820293465203931452873"))
                                                        .add("y", new BigInteger("15622273729109725244555139052709355747200820317138470284340626448011133037374039"))
                                                )
                                        )
                                )
                                .build(),
                    };
            
            Arrays.stream(invalidShares)
                    .forEach(invalidShare -> {
                        try {
                            shareConstraint.validate(invalidShare);
                            Assertions.fail();
                        } catch (Exception ex) {
                            tracer.out().printfIndentln("==> %s", ex.getMessage());
                            assertThat(ex).isInstanceOf(JsonValueConstraint.Exception.class);
                        }
                    });
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void sliceConstraint() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "sliceConstraint()");

        try {
            String uuidPattern = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}";
            MyJsonStringConstraint uuidConstraint = new MyJsonStringConstraint(uuidPattern);
            String statePattern = SliceProcessingState.FETCHED.name() + "|"
                    + SliceProcessingState.POSTED.name();
            MyJsonStringConstraint stateConstraint = new MyJsonStringConstraint(statePattern);
            MyJsonObjectConstraint sliceConstraint = new MyJsonObjectConstraint(
                    Map.of("id", uuidConstraint, "state", stateConstraint, "share", new MyJsonAnyObjectConstraint())
            );

            JsonObject validPostedSlice = Json.createObjectBuilder()
                    .add("id", "1495545e-0cf9-4c7f-b46f-8768decdaf73")
                    .add("state", SliceProcessingState.POSTED.name())
                    .add("share", Json.createObjectBuilder()
                            .add("PartitionId", "4c8817d8-cafd-4dff-8538-02567071fbea")
                            .add("Prime", new BigInteger("20578480839578433406690946154785465029530804091460622363478793090835305100929087"))
                            .add("Threshold", 4)
                            .add("SharePoints", Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder()
                                            .add("SharePoint", Json.createObjectBuilder()
                                                    .add("x", new BigInteger("5343537088766651467237183685639144915303458993519496215604768621258607770190557"))
                                                    .add("y", new BigInteger("12123040870242525799199784089760842359830853555007955848266717787238675882765492"))
                                            )
                                    )
                                    .add(Json.createObjectBuilder()
                                            .add("SharePoint", Json.createObjectBuilder()
                                                    .add("x", new BigInteger("4992587400096589644966411458474479840083891127803171906966615570472454048811046"))
                                                    .add("y", new BigInteger("3331447472596458512646320851992615013974771345825613648005571050286395467356219"))
                                            )
                                    )
                                    .add(Json.createObjectBuilder()
                                            .add("SharePoint", Json.createObjectBuilder()
                                                    .add("x", new BigInteger("4776132650646788302694822741410760125034403808856592636268649697857192978140080"))
                                                    .add("y", new BigInteger("2489889427894710311098404634701006983293811668071372095790060111933668950408234"))
                                            )
                                    )
                                    .add(Json.createObjectBuilder()
                                            .add("SharePoint", Json.createObjectBuilder()
                                                    .add("x", new BigInteger("11817840164080823997574382054580179660869802129441618659560820293465203931452873"))
                                                    .add("y", new BigInteger("15622273729109725244555139052709355747200820317138470284340626448011133037374039"))
                                            )
                                    )
                            )
                    )
                    .build();

            JsonObject validFetchedSlice = Json.createObjectBuilder()
                    .add("id", "1495545e-0cf9-4c7f-b46f-8768decdaf73")
                    .add("state", SliceProcessingState.FETCHED.name())
                    .add("share", Json.createObjectBuilder().build()
                    )
                    .build();

            JsonObject[] validSlices = {validPostedSlice, validFetchedSlice};

            assertThat(
                    Arrays.stream(validSlices).allMatch(validSlice -> sliceConstraint.validate(validSlice))
            ).isTrue();

            JsonObject invalidSlice1 = Json.createObjectBuilder()
                    .add("id", "1495545e-0cf9-4c7f-b46f-8768decdaf73")
                    .add("state", SliceProcessingState.CREATED.name())
                    .add("share", Json.createObjectBuilder().build()
                    )
                    .build();

            JsonObject invalidSlice2 = Json.createObjectBuilder()
                    .add("state", SliceProcessingState.FETCHED.name())
                    .add("share", Json.createObjectBuilder().build()
                    )
                    .build();

            JsonObject invalidSlice3 = Json.createObjectBuilder()
                    .add("id", "1495545e-0cf9-4c7f-b46f-8768decdaf73")
                    .add("share", Json.createObjectBuilder().build()
                    )
                    .build();

            JsonObject invalidSlice4 = Json.createObjectBuilder()
                    .add("id", "1495545e-0cf9-4c7f-b46f-8768decdaf73")
                    .add("state", SliceProcessingState.FETCHED.name())
                    .build();

            JsonObject invalidSlice5 = Json.createObjectBuilder()
                    .add("id", "1495545e-0cf9-4c7f-b46f-8768decdaf73")
                    .add("state", SliceProcessingState.FETCHED.name())
                    .add("share", "error")
                    .build();
            
            JsonObject[] invalidSlices = {invalidSlice1, invalidSlice2, invalidSlice3, invalidSlice4, invalidSlice5};
            
            Arrays.stream(invalidSlices)
                    .forEach(invalidSlice -> {
                        try {
                            sliceConstraint.validate(invalidSlice);
                            Assertions.fail();
                        } catch (Exception ex) {
                            tracer.out().printfIndentln("==> %s", ex.getMessage());
                            assertThat(ex).isInstanceOf(JsonValueConstraint.Exception.class);
                        }
                    });
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentPoolTracer();
    }
}
