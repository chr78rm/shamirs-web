/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.jca.shamir.PasswordGenerator;
import de.christofreichardt.jca.shamir.ShamirsLoadParameter;
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.jca.shamir.ShamirsProvider;
import de.christofreichardt.json.JsonTracer;
import de.christofreichardt.json.JsonValueCollector;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Participant;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.model.Slice;
import de.christofreichardt.scala.shamir.SecretSharing;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonPointer;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 *
 * @author Developer
 */
public class KeystoreGenerator implements Traceable {

    static final int DEFAULT_PASSWORD_LENGTH = 32;

    static final JsonPointer SHARES_POINTER = Json.createPointer("/shares");
    static final JsonPointer THRESHOLD_POINTER = Json.createPointer("/threshold");
    static final JsonPointer DESCRIPTIVE_NAME_POINTER = Json.createPointer("/descriptiveName");
    static final JsonPointer KEY_INFOS_POINTER = Json.createPointer("/keyinfos");
    static final JsonPointer SIZES_POINTER = Json.createPointer("/sizes");

    final KeyStore keyStore;
    final char[] password;
    final JsonArray partition;
    final JsonArray requestedSizes;
    final int shares;
    final int threshold;
    final String descriptiveName;
    final Set<String> participantNames;

    final JsonTracer jsonTracer = new JsonTracer() {
        @Override
        public AbstractTracer getCurrentTracer() {
            return KeystoreGenerator.this.getCurrentTracer();
        }
    };

    public KeystoreGenerator(JsonObject keystoreInstructions) throws GeneralSecurityException, IOException {
        this.requestedSizes = keystoreInstructions.getJsonArray("sizes");
        this.keyStore = makeKeyStore();
        this.password = password();
        int[] sizes = keystoreInstructions.getJsonArray("sizes").stream()
                .map(slice -> slice.asJsonObject())
                .mapToInt(slice -> slice.getInt("size"))
                .toArray();
        this.shares = keystoreInstructions.getInt("shares");
        this.threshold = keystoreInstructions.getInt("threshold");
        this.partition = computeSharePoints(this.shares, this.threshold, sizes);
        generateSecretKeys(keystoreInstructions.getJsonArray("keyinfos"), "AES");
        generatePrivateKeys(keystoreInstructions.getJsonArray("keyinfos"));
        this.descriptiveName = keystoreInstructions.getString("descriptiveName");
        this.participantNames = keystoreInstructions.getJsonArray("sizes").stream() // TODO: sanitize the names of the participants and make sure that they are distinct
                .map(size -> size.asJsonObject())
                .map(size -> size.getString("participant"))
                .collect(Collectors.toUnmodifiableSet());
    }

    final KeyStore makeKeyStore() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("KeyStore", this, "makeKeyStore()");

        try {
            KeyStore keyStore = KeyStore.getInstance("ShamirsKeystore", Security.getProvider(ShamirsProvider.NAME));
            keyStore.load(null, null);

            return keyStore;
        } finally {
            tracer.wayout();
        }
    }

    final void generateSecretKeys(JsonArray keyInfos, String algorithm) throws NoSuchAlgorithmException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "generateSecretKeys(JsonArray keyInfos, String algorithm)");

        try {
//            this.jsonTracer.trace(keyInfos);
//            this.jsonTracer.trace(this.partition);

            keyInfos.stream()
                    .map(jsonValue -> jsonValue.asJsonObject())
                    .filter(keyInfo -> Objects.equals("secret-key", keyInfo.getString("type")))
                    .filter(keyInfo -> Objects.equals(algorithm, keyInfo.getString("algorithm")))
                    .peek(keyInfo -> this.jsonTracer.trace(keyInfo))
                    .forEach(keyInfo -> {
                        try {
                            KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
                            keyGenerator.init(keyInfo.getInt("keySize"));
                            SecretKey secretKey = keyGenerator.generateKey();
                            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
                            this.keyStore.setEntry(keyInfo.getString("alias"), secretKeyEntry, new ShamirsProtection(this.partition));
                        } catch (GeneralSecurityException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        } finally {
            tracer.wayout();
        }
    }

    final void generatePrivateKeys(JsonArray keyInfos) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "generatePrivateKeys(JsonArray keyInfos)");

        try {
            keyInfos.stream()
                    .map(jsonValue -> jsonValue.asJsonObject())
                    .filter(keyInfo -> Objects.equals("private-key", keyInfo.getString("type")))
                    .peek(keyInfo -> this.jsonTracer.trace(keyInfo))
                    .forEach(keyInfo -> {
                        try {
                            String alias = keyInfo.getString("alias");
                            String algorithm = keyInfo.getString("algorithm");
                            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
                            String signatureAlgo;
                            switch (algorithm) {
                                case "DSA":
                                    keyPairGenerator.initialize(2048);
                                    signatureAlgo = "SHA256withDSA";
                                    break;
                                case "RSA":
                                    keyPairGenerator.initialize(4096);
                                    signatureAlgo = "SHA256withRSA";
                                    break;
                                case "EC":
                                    ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp521r1");
                                    keyPairGenerator.initialize(ecGenParameterSpec);
                                    signatureAlgo = "SHA256withECDSA";
                                    break;
                                default:
                                    throw new NoSuchAlgorithmException(String.format("%s is not supported.", algorithm));
                            }
                            KeyPair keyPair = keyPairGenerator.generateKeyPair();
                            JsonObject x509 = keyInfo.getJsonObject("x509");
                            int validity = x509.getInt("validity");
                            String commonName = x509.getString("commonName");
                            String locality = x509.getString("locality");
                            String state = x509.getString("state");
                            String country = x509.getString("country");
                            String distinguishedName = String.format("CN=%s, L=%s, ST=%s, C=%s", commonName, locality, state, country);
                            Instant now = Instant.now();
                            Date notBefore = Date.from(now);
                            Date notAfter = Date.from(now.plus(Duration.ofDays(validity)));
                            ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgo).build(keyPair.getPrivate());
                            X500Name x500Name = new X500Name(distinguishedName);
                            JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                                    x500Name,
                                    BigInteger.valueOf(now.toEpochMilli()),
                                    notBefore,
                                    notAfter,
                                    x500Name,
                                    keyPair.getPublic()
                            );
                            X509CertificateHolder x509CertificateHolder = certificateBuilder.build(contentSigner);
                            JcaX509CertificateConverter x509CertificateConverter = new JcaX509CertificateConverter();
                            x509CertificateConverter.setProvider(new BouncyCastleProvider());
                            X509Certificate x509Certificate = x509CertificateConverter.getCertificate(x509CertificateHolder);
                            this.keyStore.setEntry(
                                    alias,
                                    new KeyStore.PrivateKeyEntry(keyPair.getPrivate(),
                                            new Certificate[]{x509Certificate}),
                                    new ShamirsProtection(this.partition)
                            );
                        } catch (GeneralSecurityException | OperatorCreationException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        } finally {
            tracer.wayout();
        }
    }

    final JsonArray computeSharePoints(int shares, int threshold, int[] sizes) throws CharacterCodingException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("JsonObject", this, "computeSharePoints(int shares, int threshold, int[] sizes)");

        try {
            tracer.out().printfIndentln("shares = %d", shares);
            tracer.out().printfIndentln("threshold = %d", threshold);

            SecretSharing secretSharing = new SecretSharing(shares, threshold, encode(this.password));

            return secretSharing.partitionAsJson(sizes);
        } finally {
            tracer.wayout();
        }
    }

    final char[] password() throws GeneralSecurityException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("char[]", this, "password()");

        try {
            PasswordGenerator passwordGenerator = new PasswordGenerator(DEFAULT_PASSWORD_LENGTH);
            CharSequence passwordSequence = passwordGenerator.generate().findFirst().get();
            CharBuffer charBuffer = CharBuffer.wrap(passwordSequence);
            char[] chars = new char[charBuffer.remaining()];
            charBuffer.get(chars);

            return chars;
        } finally {
            tracer.wayout();
        }
    }

    final byte[] encode(char[] password) throws CharacterCodingException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("byte[]", this, "encode(char[] password)");

        try {
            CharsetEncoder charsetEncoder = StandardCharsets.UTF_8.newEncoder();
            CharBuffer charBuffer = CharBuffer.wrap(password);
            ByteBuffer byteBuffer = charsetEncoder.encode(charBuffer);
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);

            return bytes;
        } finally {
            tracer.wayout();
        }
    }

    String partitionId() {
        return this.partition.get(0).asJsonObject().getString("PartitionId");
    }

    int shares() {
        return this.shares;
    }

    int threshold() {
        return this.threshold;
    }

    int size(String participant) {
        Optional<JsonObject> sizeForParticipant = this.requestedSizes.stream()
                .map(size -> size.asJsonObject())
                .filter(size -> Objects.equals(participant, size.getString("participant")))
                .findFirst();
        JsonObject size = sizeForParticipant.orElseThrow(() -> new IllegalArgumentException(String.format("No such participant '%s' found.", participant)));

        return size.getInt("size");
    }

    Map<String, byte[]> partition() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Map<String, byte[]>", this, "partition()");

        try {
            Map<String, byte[]> sharePoints = new HashMap<>();

            JsonArray orderedSizes = this.requestedSizes.stream()
                    .map(size -> size.asJsonObject())
                    .sorted((JsonObject size1, JsonObject size2) -> {
                        if (size1.getInt("size") < size2.getInt("size")) {
                            return -1;
                        } else if (size1.getInt("size") > size2.getInt("size")) {
                            return 1;
                        } else {
                            return 0;
                        }
                    })
                    .collect(new JsonValueCollector());

//            this.jsonTracer.trace(orderedSizes);
            JsonArray orderedSlices = this.partition.stream()
                    .map(slice -> slice.asJsonObject())
                    .sorted((JsonObject slice1, JsonObject slice2) -> {
                        if (slice1.getJsonArray("SharePoints").size() < slice2.getJsonArray("SharePoints").size()) {
                            return -1;
                        } else if (slice1.getJsonArray("SharePoints").size() > slice2.getJsonArray("SharePoints").size()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    })
                    .collect(new JsonValueCollector());

//            this.jsonTracer.trace(orderedSlices);
            Iterator<JsonValue> iter = orderedSizes.iterator();
            orderedSlices.stream()
                    .map(slice -> {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        JsonWriter jsonWriter = Json.createWriter(byteArrayOutputStream);
                        jsonWriter.write(slice);

                        return byteArrayOutputStream.toByteArray();
                    })
                    .forEach(slice -> {
                        String participant = iter.next().asJsonObject().getString("participant");
                        sharePoints.put(participant, slice);
                    });

            return sharePoints;
        } finally {
            tracer.wayout();
        }
    }

    byte[] keystoreBytes() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("byte[]", this, "keystoreBytes()");

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ShamirsLoadParameter shamirsLoadParameter = new ShamirsLoadParameter(byteArrayOutputStream, new ShamirsProtection(this.partition));
            this.keyStore.store(shamirsLoadParameter);

            return byteArrayOutputStream.toByteArray();
        } finally {
            tracer.wayout();
        }
    }

    Set<String> participantNames() {
        return this.participantNames;
    }

    DatabasedKeystore makeDBKeystore(Map<String, Participant> participants) throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("DatabasedKeystore", this, "makeKeystore(Set<String> preferredNames)");

        try {
            tracer.out().printfIndentln("participants = %s", participants);

            DatabasedKeystore keystore = new DatabasedKeystore(this.descriptiveName);
            keystore.setStore(this.keystoreBytes());

            Set<Slice> slices = this.partition().entrySet().stream()
                    .map(entry -> {
                        Participant participant = participants.get(entry.getKey());
                        tracer.out().printfIndentln("participant = %s", participant);
                        Slice slice = new Slice();
                        slice.setPartitionId(this.partitionId());
                        slice.setSize(this.size(participant.getPreferredName()));
                        slice.setShare(entry.getValue());
                        slice.createdFor(keystore, participant);

                        return slice;
                    })
                    .collect(Collectors.toSet());

            keystore.setSlices(slices);
            keystore.setCurrentPartitionId(this.partitionId());
            keystore.setShares(this.shares());
            keystore.setThreshold(this.threshold());
            Session session = new Session();
            session.provisionedFor(keystore);
            keystore.getSessions().add(session);
            
            return keystore;
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }
}
