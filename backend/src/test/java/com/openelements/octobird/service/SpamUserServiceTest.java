package com.openelements.octobird.service;

import com.openelements.octobird.persistence.TransactionManager;
import com.openelements.octobird.rest.GitHubAccountDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpamUserServiceTest {

    @Mock
    private TransactionManager txManager;

    private SpamUserService service;

    @BeforeEach
    void setUp() {
        service = new SpamUserService(txManager);
    }

    @Test
    void isSpamUserReturnsTrueWhenInDb() {
        // Given
        when(txManager.executeReadOnly(any())).thenReturn(true);

        // When
        final boolean result = service.isSpamUser(1, 100);

        // Then
        assertTrue(result);
    }

    @Test
    void isSpamUserReturnsFalseWhenNotInDb() {
        // Given
        when(txManager.executeReadOnly(any())).thenReturn(false);

        // When
        final boolean result = service.isSpamUser(1, 999);

        // Then
        assertFalse(result);
    }

    @Test
    void getSpamUsersReturnsDtoListFromDb() {
        // Given
        final List<GitHubAccountDto> expected = List.of(
                new GitHubAccountDto(100, "alice"),
                new GitHubAccountDto(200, "bob"));
        when(txManager.executeReadOnly(any())).thenReturn(expected);

        // When
        final List<GitHubAccountDto> result = service.getSpamUsers(1);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void setSpamUsersCallsTransaction() {
        // Given
        final List<GitHubAccountDto> accounts = List.of(
                new GitHubAccountDto(100, "alice"),
                new GitHubAccountDto(200, "bob"));

        // When
        service.setSpamUsers(1, accounts);

        // Then
        verify(txManager).runInTransaction(any());
    }
}
