package org.hiero.bot.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * Shared JSON serialization helper for REST endpoints.
 */
public final class JsonHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonHelper() {
    }

    /**
     * Writes the given object as JSON to the response with status 200.
     *
     * @param res   the server response
     * @param value the object to serialize
     */
    public static void sendJson(final ServerResponse res, final Object value) {
        try {
            final String json = MAPPER.writeValueAsString(value);
            res.header(HeaderNames.CONTENT_TYPE, "application/json");
            res.send(json);
        } catch (final JsonProcessingException e) {
            res.status(Status.INTERNAL_SERVER_ERROR_500).send("JSON serialization error");
        }
    }

    /**
     * Reads JSON from an input stream into the given type.
     *
     * @param is   the input stream
     * @param type the target class
     * @param <T>  the target type
     * @return the deserialized object
     * @throws IOException if reading or parsing fails
     */
    public static <T> T readJson(final InputStream is, final Class<T> type) throws IOException {
        return MAPPER.readValue(is, type);
    }

    /**
     * Returns the shared ObjectMapper instance.
     *
     * @return the configured ObjectMapper
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
