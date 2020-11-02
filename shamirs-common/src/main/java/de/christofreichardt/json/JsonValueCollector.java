/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.json;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;

/**
 *
 * @author Developer
 */
public class JsonValueCollector implements Collector<JsonValue, JsonArrayBuilder, JsonArray> {

    @Override
    public Supplier<JsonArrayBuilder> supplier() {
        return () -> Json.createArrayBuilder();
    }

    @Override
    public BiConsumer<JsonArrayBuilder, JsonValue> accumulator() {
        return (JsonArrayBuilder jsonArrayBuilder, JsonValue jsonValue) -> jsonArrayBuilder.add(jsonValue);
    }

    @Override
    public BinaryOperator<JsonArrayBuilder> combiner() {
        return null;
    }

    @Override
    public Function<JsonArrayBuilder, JsonArray> finisher() {
        return (JsonArrayBuilder jsonArrayBuilder) -> jsonArrayBuilder.build();
    }

    @Override
    public Set<Collector.Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
