/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.model;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.jca.shamir.PasswordGenerator;
import de.christofreichardt.jca.shamir.ShamirsLoadParameter;
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.jca.shamir.ShamirsProvider;
import de.christofreichardt.json.JsonValueCollector;
import de.christofreichardt.scala.shamir.SecretSharing;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Security;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Developer
 */
@Entity
@Table(name = "keystore")
@NamedQueries({
    @NamedQuery(name = "DatabasedKeystore.findAll", query = "SELECT k FROM DatabasedKeystore k"),
    @NamedQuery(name = "DatabasedKeystore.findById", query = "SELECT k FROM DatabasedKeystore k WHERE k.id = :id"),
    @NamedQuery(name = "DatabasedKeystore.findByDescriptiveName", query = "SELECT k FROM DatabasedKeystore k WHERE k.descriptiveName = :descriptiveName"),
    @NamedQuery(name = "DatabasedKeystore.findByIdWithPostedSlices",
            query = "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.id = :id AND s.processingState = 'POSTED'"),
    @NamedQuery(name = "DatabasedKeystore.findByIdWithCertainSlices",
            query = "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.id = :id AND s.processingState = :state"),
    @NamedQuery(name = "DatabasedKeystore.findByIdWithActiveSlices",
            query = "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.id = :id AND s.processingState = 'POSTED' OR s.processingState = 'CREATED'"),
    @NamedQuery(name = "DatabasedKeystore.findByIdWithActiveSlicesAndValidSessions",
            query = "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices sl LEFT JOIN FETCH k.sessions se WHERE k.id = :id AND (sl.processingState = 'POSTED' OR sl.processingState = 'CREATED') AND (se.phase = 'PENDING' OR se.phase = 'ACTIVE')"),
    @NamedQuery(name = "DatabasedKeystore.findByIdAndParticipantWithPostedSlices",
            query = "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.id = :id AND s.processingState = 'POSTED' AND s.participant.id = :participantId"),})
public class DatabasedKeystore {

    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "id")
    private String id;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "descriptive_name")
    private String descriptiveName;

    @Lob
    @Column(name = "store")
    private byte[] store;

    @Basic(optional = false)
    @NotNull
    @Column(name = "current_partition_id")
    private String currentPartitionId;

    @Basic(optional = false)
    @NotNull
    @Column(name = "shares")
    private int shares;

    @Basic(optional = false)
    @NotNull
    @Column(name = "threshold")
    private int threshold;

    @Basic(optional = false)
    @NotNull
    @Column(name = "creation_time")
    private LocalDateTime creationTime;

    @Basic(optional = false)
    @NotNull
    @Column(name = "modification_time")
    private LocalDateTime modificationTime;
    
    @Version
    @Basic(optional = false)
    @NotNull
    @Column(name = "version")
    private int version;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "keystore")
    private Set<Slice> slices = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "keystore")
    private Set<Session> sessions = new HashSet<>();

    public DatabasedKeystore() {
        this.id = UUID.randomUUID().toString();
        this.creationTime = LocalDateTime.now();
        this.modificationTime = LocalDateTime.now();
    }

    public DatabasedKeystore(String descriptiveName) {
        this();
        this.descriptiveName = descriptiveName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescriptiveName() {
        return descriptiveName;
    }

    public void setDescriptiveName(String descriptiveName) {
        this.descriptiveName = descriptiveName;
    }

    public byte[] getStore() {
        return store;
    }

    public void setStore(byte[] store) {
        this.store = store;
    }

    public String getCurrentPartitionId() {
        return currentPartitionId;
    }

    public void setCurrentPartitionId(String currentPartitionId) {
        this.currentPartitionId = currentPartitionId;
    }

    public int getShares() {
        return shares;
    }

    public void setShares(int shares) {
        this.shares = shares;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public LocalDateTime getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(LocalDateTime modificationTime) {
        this.modificationTime = modificationTime;
    }

    public Set<Slice> getSlices() {
        return slices;
    }

    public void setSlices(Set<Slice> slices) {
        this.slices = slices;
    }

    public Set<Session> getSessions() {
        return sessions;
    }

    public void setSessions(Set<Session> sessions) {
        this.sessions = sessions;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatabasedKeystore)) {
            return false;
        }
        DatabasedKeystore other = (DatabasedKeystore) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("DatabasedKeystore[id=%s, descriptiveName=%s, currentPartitionId=%s, shares=%d, threshold=%d, creationTime=%s, mofificationTime=%s]",
                this.id, this.descriptiveName, this.currentPartitionId, this.shares, this.threshold,
                this.creationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)),
                this.modificationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)));
    }

    public JsonArray sharePoints() {
        if (getSlices() == null) {
            throw new IllegalStateException("No slices.");
        }

        JsonArray sharePoints = getSlices().stream()
                .filter(slice -> slice.getPartitionId().equals(this.currentPartitionId))
                .filter(slice -> slice.isCreated() || slice.isPosted())
                .map(slice -> new ByteArrayInputStream(slice.getShare()))
                .map(in -> {
                    try (JsonReader jsonReader = Json.createReader(in)) {
                        return jsonReader.read();
                    }
                })
                .collect(new JsonValueCollector());

        return sharePoints;
    }
    
    public int[] sizes() {
        return getSlices().stream()
                .mapToInt(slice -> slice.getSize())
                .toArray();
    }
    
    public byte[] nextKeystoreInstance(ShamirsProtection nextProtection) throws GeneralSecurityException, IOException {
        KeyStore nextKeyStore = KeyStore.getInstance("ShamirsKeystore", Security.getProvider(ShamirsProvider.NAME));
        nextKeyStore.load(null, null);
        KeyStore currentKeyStore = keystoreInstance();
        ShamirsProtection currentProtection = new ShamirsProtection(sharePoints());
        Iterator<String> aliasIter = currentKeyStore.aliases().asIterator();
        while (aliasIter.hasNext()) {
            String alias = aliasIter.next();
            KeyStore.Entry entry = currentKeyStore.getEntry(alias, currentProtection);
            nextKeyStore.setEntry(alias, entry, nextProtection);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ShamirsLoadParameter shamirsLoadParameter = new ShamirsLoadParameter(byteArrayOutputStream, nextProtection);
        nextKeyStore.store(shamirsLoadParameter);
        
        return byteArrayOutputStream.toByteArray();
    }

    public KeyStore keystoreInstance() throws GeneralSecurityException, IOException {
        if (getStore() == null) {
            throw new IllegalStateException("No store bytes.");
        }

        try {
            ShamirsProtection shamirsProtection = new ShamirsProtection(sharePoints());
            ByteArrayInputStream in = new ByteArrayInputStream(getStore());
            ShamirsLoadParameter shamirsLoadParameter = new ShamirsLoadParameter(in, shamirsProtection);
            KeyStore shamirsKeystore = KeyStore.getInstance("ShamirsKeystore", Security.getProvider(ShamirsProvider.NAME));
            shamirsKeystore.load(shamirsLoadParameter);

            return shamirsKeystore;
        } catch (IllegalArgumentException ex) {
            throw new KeyStoreException(ex);
        }
    }

    public Map.Entry<String, JsonArray> nextPartition() throws GeneralSecurityException {

        class JsonSliceComparator implements Comparator<JsonObject> {

            @Override
            public int compare(JsonObject slice1, JsonObject slice2) {
                int size1 = slice1.getJsonArray("SharePoints").size();
                int size2 = slice2.getJsonArray("SharePoints").size();
                if (size1 < size2) {
                    return -1;
                } else if (size1 > size2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

        final int DEFAULT_PASSWORD_LENGTH = 32;
        
        PasswordGenerator passwordGenerator = new PasswordGenerator(DEFAULT_PASSWORD_LENGTH);
        CharSequence passwordSequence = passwordGenerator.generate().findFirst().get();
        SecretSharing secretSharing = new SecretSharing(this.getShares(), this.getThreshold(), passwordSequence);
        JsonArray nextPartition = secretSharing.partitionAsJson(this.sizes()).stream()
                .map(slice -> slice.asJsonObject())
                .sorted(new JsonSliceComparator())
                .collect(new JsonValueCollector());
        String nextPartitionId = nextPartition.getJsonObject(0).getString("PartitionId");

        return new AbstractMap.SimpleImmutableEntry<>(nextPartitionId, nextPartition);
    }
 
    public Stream<Slice> currentSlices() {
        return this.getSlices().stream()
                .filter(slice -> Objects.equals(slice.getPartitionId(), this.getCurrentPartitionId()));
    }

    void addNextSlices(Map.Entry<String, JsonArray> nextPartition) {
        Iterator<JsonValue> iter = nextPartition.getValue().iterator();
        Set<Slice> nextSlices = this.currentSlices()
                .sorted()
                .map(slice -> {
                    slice.expired();
                    JsonValue share = iter.next();
                    Slice nextSlice = new Slice(nextPartition.getKey(), slice.getSize(), share);
                    nextSlice.createdFor(this, slice.getParticipant());
                    return nextSlice;
                })
                .collect(Collectors.toSet());
        this.slices.addAll(nextSlices);
    }

    void allocateNextSession() {
        this.sessions.stream()
                .filter(session -> session.isActive())
                .forEach(session -> session.closed());
        Session nextSession = new Session();
        nextSession.provisionedFor(this);
        this.sessions.add(nextSession);
    }
    
    public void modificated() {
        this.modificationTime = LocalDateTime.now();
    }
    
    void computeNextKeystoreInstance(ShamirsProtection nextProtection) throws GeneralSecurityException, IOException {
        KeyStore nextKeyStore = KeyStore.getInstance("ShamirsKeystore", Security.getProvider(ShamirsProvider.NAME));
        nextKeyStore.load(null, null);
        KeyStore currentKeyStore = keystoreInstance();
        ShamirsProtection currentProtection = new ShamirsProtection(sharePoints());
        Iterator<String> aliasIter = currentKeyStore.aliases().asIterator();
        while (aliasIter.hasNext()) {
            String alias = aliasIter.next();
            KeyStore.Entry entry = currentKeyStore.getEntry(alias, currentProtection);
            nextKeyStore.setEntry(alias, entry, nextProtection);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ShamirsLoadParameter shamirsLoadParameter = new ShamirsLoadParameter(byteArrayOutputStream, nextProtection);
        nextKeyStore.store(shamirsLoadParameter);
        this.store = byteArrayOutputStream.toByteArray();
    }

    public void rollover(Map.Entry<String, JsonArray> nextPartition) throws GeneralSecurityException, IOException {
        ShamirsProtection nextProtection = new ShamirsProtection(nextPartition.getValue());
        this.computeNextKeystoreInstance(nextProtection);
        this.addNextSlices(nextPartition);
        this.currentPartitionId = nextPartition.getKey();
        this.allocateNextSession();
        this.modificated();
    }
    
    public Session currentSession() {
        List<Session> activeSessions = this.sessions.stream()
                .filter(session -> session.isActive() || session.isProvisioned())
                .collect(Collectors.toList());
        if (activeSessions.isEmpty()) {
            throw new IllegalStateException("No session found.");
        } else if (activeSessions.size() > 1) {
            throw new IllegalStateException("Non unique current session.");
        } else {
            return activeSessions.get(0);
        }
    }

    public JsonObject toJson() {
        return toJson(false);
    }
    
    public void trace(AbstractTracer tracer, boolean inFull) {
        tracer.entry("void", this, "trace(AbstractTracer tracer, boolean inFull)");
        try {
            tracer.out().printfIndentln("inFull = %b", inFull);
            
            tracer.out().printfIndentln("keystore = %s", this);
            if (inFull) {
                this.slices.forEach(slice -> tracer.out().printfIndentln("slice = %s", slice));
                this.sessions.forEach(session -> tracer.out().printfIndentln("session = %s", session));
            }
        } finally {
            tracer.wayout();
        }
    }
    
    JsonArray selfLinkTypes() {
        return Json.createArrayBuilder()
                .add("GET")
                .build();
    }

    public JsonObject toJson(boolean inFull) {
        JsonObject jsonKeystore;
        JsonArrayBuilder linkEntriesBuilder = Json.createArrayBuilder();
        JsonObjectBuilder jsonKeystoreBuilder = Json.createObjectBuilder()
                .add("id", this.id)
                .add("descriptiveName", this.descriptiveName)
                .add("currentPartitionId", this.currentPartitionId)
                .add("shares", this.shares)
                .add("threshold", this.threshold)
                .add("creationTime", this.creationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .add("mofificationTime", this.modificationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        linkEntriesBuilder
                .add(Json.createObjectBuilder()
                        .add("rel", "self")
                        .add("href", String.format("/keystores/%s", this.id))
                        .add("type", selfLinkTypes()
                        )
                );
        if (inFull) {
            linkEntriesBuilder
                    .add(Json.createObjectBuilder()
                            .add("rel", "sessions")
                            .add("href", String.format("/keystores/%s/sessions", this.id))
                            .add("type", Json.createArrayBuilder()
                                    .add("GET")
                            )
                    );
            linkEntriesBuilder
                        .add(Json.createObjectBuilder()
                                .add("rel", "currentSession")
                                .add("href", String.format("/keystores/%s/sessions/%s", this.id, currentSession().getId()))
                                .add("type", Json.createArrayBuilder()
                                        .add("GET")
                                        .add("PATCH")
                                )
                        );
            try {
                ShamirsProtection shamirsProtection = new ShamirsProtection(sharePoints());
                KeyStore shamirsKeystore = keystoreInstance();
                JsonArrayBuilder keyEntriesBuilder = Json.createArrayBuilder();
                Map<String, String> oid2Identifier = Map.of("1.2.840.113549.1.9.21", "localKeyID", "1.2.840.113549.1.9.20", "friendlyName");
                Iterator<String> iter = shamirsKeystore.aliases().asIterator();
                while (iter.hasNext()) {
                    JsonObjectBuilder keyEntryBuilder = Json.createObjectBuilder();
                    String alias = iter.next();
                    keyEntryBuilder.add("alias", alias);
                    KeyStore.Entry entry = shamirsKeystore.getEntry(alias, shamirsProtection);
                    Map<String, String> attributes = entry.getAttributes().stream()
                            .map(attr -> new AbstractMap.SimpleImmutableEntry<>(oid2Identifier.get(attr.getName()), attr.getValue()))
                            .collect(Collectors.toMap(attr -> attr.getKey(), attr -> attr.getValue()));
                    attributes.entrySet().forEach(attr -> keyEntryBuilder.add(attr.getKey(), attr.getValue()));
                    keyEntriesBuilder.add(keyEntryBuilder);
                }
                jsonKeystoreBuilder.add("keyEntries", keyEntriesBuilder);
            } catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
                jsonKeystoreBuilder.add("keyEntries", "unloadable");
            }
        }
        jsonKeystoreBuilder.add("links", linkEntriesBuilder);
        jsonKeystore = jsonKeystoreBuilder.build();

        return jsonKeystore;
    }

}
