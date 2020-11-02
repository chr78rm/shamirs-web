/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.json;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

/**
 *
 * @author CReichardt
 */
abstract public class JsonTracer implements Traceable {

    private final JsonWriterFactory jsonWriterFactory;

    public JsonTracer() {
        Map<String, Object> writerProps = new HashMap<>();
        writerProps.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        this.jsonWriterFactory = Json.createWriterFactory(writerProps);
    }

    public void trace(final JsonArray jsonArray) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "trace(JsonArray jsonArray)");

        try {
            try {
                byte[] bytes;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try (JsonWriter jsonWriter = this.jsonWriterFactory.createWriter(byteArrayOutputStream, Charset.forName("UTF-8"));) {
                    jsonWriter.writeArray(jsonArray);
                }
                bytes = byteArrayOutputStream.toByteArray();
                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                        InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream, Charset.forName("UTF-8"));
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    bufferedReader.lines().forEach(line -> tracer.out().printfIndentln(line));
                }
                tracer.out().println();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        } finally {
            tracer.wayout();
        }
    }

    public void trace(JsonObject jsonObject) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "trace(JsonObject jsonObject)");

        try {
            try {
                byte[] bytes;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try (JsonWriter jsonWriter = this.jsonWriterFactory.createWriter(byteArrayOutputStream);) {
                    jsonWriter.writeObject(jsonObject);
                }
                bytes = byteArrayOutputStream.toByteArray();
                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                        InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream, Charset.forName("UTF-8"));
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    bufferedReader.lines().forEach(line -> tracer.out().printfIndentln(line));
                }
                tracer.out().println();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        } finally {
            tracer.wayout();
        }
    }

    public void trace(JsonStructure jsonStructure) {
        AbstractTracer tracer = getCurrentTracer();
        switch (jsonStructure.getValueType()) {
            case ARRAY:
                trace(jsonStructure.asJsonArray());
                break;
            case OBJECT:
                trace(jsonStructure.asJsonObject());
                break;
            default:
                tracer.out().printfIndentln("jsonStructure.getValueType() = %s", jsonStructure.getValueType());
                break;
        }
    }

}
