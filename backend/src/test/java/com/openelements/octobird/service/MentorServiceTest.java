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
class MentorServiceTest {

    @Mock
    private TransactionManager txManager;

    private MentorService service;

    @BeforeEach
    void setUp() {
        service = new MentorService(txManager);
    }

    @Test
    void selectMentorReturnsMentorUsername() {
        // Given
        when(txManager.executeInTransaction(any())).thenReturn("mentor1");

        // When
        final String result = service.selectMentor(1);

        // Then
        assertEquals("mentor1", result);
    }

    @Test
    void selectMentorReturnsNullWhenNoMentors() {
        // Given
        when(txManager.executeInTransaction(any())).thenReturn(null);

        // When
        final String result = service.selectMentor(1);

        // Then
        assertNull(result);
    }

    @Test
    void getMentorsReturnsDtoListFromDb() {
        // Given
        final List<GitHubAccountDto> expected = List.of(
                new GitHubAccountDto(100, "mentor1"),
                new GitHubAccountDto(200, "mentor2"));
        when(txManager.executeReadOnly(any())).thenReturn(expected);

        // When
        final List<GitHubAccountDto> result = service.getMentors(1);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void setMentorsCallsTransaction() {
        // Given
        final List<GitHubAccountDto> accounts = List.of(
                new GitHubAccountDto(100, "mentor1"),
                new GitHubAccountDto(200, "mentor2"));

        // When
        service.setMentors(1, accounts);

        // Then
        verify(txManager).runInTransaction(any());
    }
}
