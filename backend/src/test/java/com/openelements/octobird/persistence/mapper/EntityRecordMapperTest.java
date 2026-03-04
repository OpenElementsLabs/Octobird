package com.openelements.octobird.persistence.mapper;

import com.openelements.octobird.config.*;
import com.openelements.octobird.persistence.entity.RepoConfigEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityRecordMapperTest {

    @Test
    void toRepoConfigUsesDefaultsForNullFields() {
        // Given
        final RepoConfigEntity entity = new RepoConfigEntity();
        entity.setRepoId(42);
        entity.setRepoFullName("owner/repo");

        // When
        final DefaultRepoConfig config = EntityRecordMapper.toRepoConfig(entity);

        // Then
        assertEquals(42, config.repoId());
        assertEquals("owner/repo", config.repoFullName());
        assertEquals(LabelsConfig.defaults().labelFor(IssueLevel.GOOD_FIRST_ISSUE),
                config.labels().labelFor(IssueLevel.GOOD_FIRST_ISSUE));
        assertEquals(AssignmentLimitsConfig.defaults().normalUserMax(),
                config.assignmentLimits().normalUserMax());
        assertEquals(FeaturesConfig.defaults().assignCommand(), config.features().assignCommand());
        assertEquals(MarkersConfig.defaults().mentorAssignment(), config.markers().mentorAssignment());
        assertEquals(CommandsConfig.defaults().assignPattern(), config.commands().assignPattern());
        assertEquals(TeamsConfig.defaults().gfiCandidateTeam(), config.teams().gfiCandidateTeam());
        assertEquals(ScheduledConfig.defaults().inactivityDays(), config.scheduled().inactivityDays());
    }

    @Test
    void toRepoConfigUsesEntityValuesWhenPresent() {
        // Given
        final RepoConfigEntity entity = new RepoConfigEntity();
        entity.setRepoId(42);
        entity.setRepoFullName("owner/repo");
        entity.setLabelGfi("Custom GFI");
        entity.setNormalUserMax(5);
        entity.setFeatAssignCommand(false);
        entity.setMarkerMentorAssignment("<!-- custom-mentor -->");
        entity.setCmdAssignPattern("/custom-assign\\b");
        entity.setTeamGfiCandidate("@org/team");
        entity.setSchedInactivityDays(30);

        // When
        final DefaultRepoConfig config = EntityRecordMapper.toRepoConfig(entity);

        // Then
        assertEquals(42, config.repoId());
        assertEquals("Custom GFI", config.labels().labelFor(IssueLevel.GOOD_FIRST_ISSUE));
        assertEquals(5, config.assignmentLimits().normalUserMax());
        assertFalse(config.features().assignCommand());
        assertEquals("<!-- custom-mentor -->", config.markers().mentorAssignment());
        assertEquals("/custom-assign\\b", config.commands().assignPattern());
        assertEquals("@org/team", config.teams().gfiCandidateTeam());
        assertEquals(30, config.scheduled().inactivityDays());
    }

    @Test
    void updateEntitySetsAllFields() {
        // Given
        final RepoConfigEntity entity = new RepoConfigEntity();
        entity.setRepoId(42);
        entity.setRepoFullName("owner/repo");
        final DefaultRepoConfig config = DefaultRepoConfig.allDefaults("owner/repo");

        // When
        EntityRecordMapper.updateEntity(entity, config);

        // Then
        assertEquals("Good First Issue", entity.getLabelGfi());
        assertEquals("beginner", entity.getLabelBeginner());
        assertEquals(2, entity.getNormalUserMax());
        assertTrue(entity.getFeatAssignCommand());
        assertEquals(MarkersConfig.defaults().mentorAssignment(), entity.getMarkerMentorAssignment());
        assertEquals(CommandsConfig.defaults().assignPattern(), entity.getCmdAssignPattern());
    }

    @Test
    void roundTripPreservesValues() {
        // Given
        final DefaultRepoConfig original = DefaultRepoConfig.allDefaults("owner/repo");
        final RepoConfigEntity entity = new RepoConfigEntity();
        entity.setRepoId(42);
        entity.setRepoFullName("owner/repo");

        // When
        EntityRecordMapper.updateEntity(entity, original);
        final DefaultRepoConfig restored = EntityRecordMapper.toRepoConfig(entity);

        // Then
        assertEquals(42, restored.repoId());
        assertEquals(original.repoFullName(), restored.repoFullName());
        assertEquals(original.labels().labelFor(IssueLevel.GOOD_FIRST_ISSUE),
                restored.labels().labelFor(IssueLevel.GOOD_FIRST_ISSUE));
        assertEquals(original.assignmentLimits().normalUserMax(), restored.assignmentLimits().normalUserMax());
        assertEquals(original.features().assignCommand(), restored.features().assignCommand());
        assertEquals(original.guards().requiredCounts(), restored.guards().requiredCounts());
        assertEquals(original.scheduled().inactivityDays(), restored.scheduled().inactivityDays());
        assertEquals(original.scheduled().communityCall().anchorDate(),
                restored.scheduled().communityCall().anchorDate());
    }

    @Test
    void toRepoConfigHandlesCollectionFields() {
        // Given
        final RepoConfigEntity entity = new RepoConfigEntity();
        entity.setRepoId(42);
        entity.setRepoFullName("owner/repo");
        entity.setSchedCcCancelledDates(List.of("2025-01-01", "2025-02-01"));
        entity.setSchedCcExcludedAuthors(List.of("bot1"));

        // When
        final DefaultRepoConfig config = EntityRecordMapper.toRepoConfig(entity);

        // Then
        assertEquals(List.of("2025-01-01", "2025-02-01"),
                config.scheduled().communityCall().cancelledDates());
        assertEquals(List.of("bot1"),
                config.scheduled().communityCall().excludedAuthors());
    }
}
