package org.hiero.bot.service;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.persistence.TransactionManager;
import org.hiero.bot.persistence.entity.RepoConfigEntity;
import org.hiero.bot.persistence.mapper.EntityRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepoConfigServiceTest {

    @Mock
    private TransactionManager txManager;

    private RepoConfigService service;

    @BeforeEach
    void setUp() {
        service = new RepoConfigService(txManager);
    }

    @Test
    void loadConfigReturnsDbConfigWhenPresent() {
        // Given
        final RepoConfigEntity entity = new RepoConfigEntity();
        entity.setRepoId(42);
        entity.setRepoFullName("owner/repo");
        entity.setNormalUserMax(5);
        final DefaultRepoConfig expected = EntityRecordMapper.toRepoConfig(entity);

        when(txManager.executeReadOnly(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            final Function<jakarta.persistence.EntityManager, Object> fn = invocation.getArgument(0);
            // Simulate DB returning the config
            return expected;
        });

        // When
        final RepoConfig result = service.loadConfig(42, "owner/repo");

        // Then
        assertEquals(5, result.assignmentLimits().normalUserMax());
    }

    @Test
    void loadConfigReturnsDefaultsWhenNotInDb() {
        // Given
        when(txManager.executeReadOnly(any())).thenReturn(null);

        // When
        final RepoConfig result = service.loadConfig(42, "owner/repo");

        // Then
        assertNotNull(result);
        assertEquals("owner/repo", result.repoFullName());
        assertEquals(DefaultRepoConfig.allDefaults("owner/repo"), result);
    }

    @Test
    void loadConfigByRepoIdReturnsNullWhenNotInDb() {
        // Given
        when(txManager.executeReadOnly(any())).thenReturn(null);

        // When
        final RepoConfig result = service.loadConfigByRepoId(999);

        // Then
        assertNull(result);
    }

    @Test
    void saveConfigCallsTransaction() {
        // Given
        final DefaultRepoConfig config = DefaultRepoConfig.allDefaults("owner/repo");

        // When
        service.saveConfig(42, "owner/repo", config);

        // Then
        verify(txManager).runInTransaction(any());
    }
}
