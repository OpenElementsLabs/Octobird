package com.openelements.octobird.util;

import com.openelements.octobird.util.MessageFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageFormatterTest {

    @Test
    void noPlaceholders() {
        // Given
        final String template = "Hello world";

        // When
        final String result = MessageFormatter.format(template);

        // Then
        assertEquals("Hello world", result);
    }

    @Test
    void singlePlaceholder() {
        // Given
        final String template = "Hi @{}";

        // When
        final String result = MessageFormatter.format(template, "alice");

        // Then
        assertEquals("Hi @alice", result);
    }

    @Test
    void multiplePlaceholders() {
        // Given
        final String template = "Hi @{}, you have {} open issues.";

        // When
        final String result = MessageFormatter.format(template, "alice", 3);

        // Then
        assertEquals("Hi @alice, you have 3 open issues.", result);
    }

    @Test
    void fewerParamsThanPlaceholders() {
        // Given
        final String template = "Hi @{}, limit {}";

        // When
        final String result = MessageFormatter.format(template, "alice");

        // Then
        assertEquals("Hi @alice, limit {}", result);
    }

    @Test
    void noParams() {
        // Given
        final String template = "Hi @{}";

        // When
        final String result = MessageFormatter.format(template);

        // Then
        assertEquals("Hi @{}", result);
    }

    @Test
    void nullParams() {
        // Given
        final String template = "Hi @{}";

        // When
        final String result = MessageFormatter.format(template, (Object[]) null);

        // Then
        assertEquals("Hi @{}", result);
    }

    @Test
    void placeholderAtStart() {
        // Given
        final String template = "{} is here";

        // When
        final String result = MessageFormatter.format(template, "alice");

        // Then
        assertEquals("alice is here", result);
    }

    @Test
    void placeholderAtEnd() {
        // Given
        final String template = "Hello {}";

        // When
        final String result = MessageFormatter.format(template, "alice");

        // Then
        assertEquals("Hello alice", result);
    }

    @Test
    void integerParam() {
        // Given
        final String template = "limit: {}";

        // When
        final String result = MessageFormatter.format(template, 5);

        // Then
        assertEquals("limit: 5", result);
    }
}