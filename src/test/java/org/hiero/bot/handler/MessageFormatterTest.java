package org.hiero.bot.handler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageFormatterTest {

    @Test
    void noPlaceholders() {
        assertEquals("Hello world", MessageFormatter.format("Hello world"));
    }

    @Test
    void singlePlaceholder() {
        assertEquals("Hi @alice", MessageFormatter.format("Hi @{}", "alice"));
    }

    @Test
    void multiplePlaceholders() {
        assertEquals("Hi @alice, you have 3 open issues.",
                MessageFormatter.format("Hi @{}, you have {} open issues.", "alice", 3));
    }

    @Test
    void fewerParamsThanPlaceholders() {
        assertEquals("Hi @alice, limit {}",
                MessageFormatter.format("Hi @{}, limit {}", "alice"));
    }

    @Test
    void noParams() {
        assertEquals("Hi @{}", MessageFormatter.format("Hi @{}"));
    }

    @Test
    void nullParams() {
        assertEquals("Hi @{}", MessageFormatter.format("Hi @{}", (Object[]) null));
    }

    @Test
    void placeholderAtStart() {
        assertEquals("alice is here", MessageFormatter.format("{} is here", "alice"));
    }

    @Test
    void placeholderAtEnd() {
        assertEquals("Hello alice", MessageFormatter.format("Hello {}", "alice"));
    }

    @Test
    void integerParam() {
        assertEquals("limit: 5", MessageFormatter.format("limit: {}", 5));
    }
}