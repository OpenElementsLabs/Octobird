package com.openelements.octobird.persistence.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import com.openelements.octobird.persistence.entity.AuditLogEntity;
import com.openelements.octobird.persistence.repository.AuditLogRepository.FilteredResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogRepositoryTest {

    private static final long REPO_ID = 100L;
    private static final long OTHER_REPO_ID = 999L;
    private static final String REPO_NAME = "owner/repo";

    private static EntityManagerFactory emf;

    @BeforeAll
    static void initFactory() {
        emf = Persistence.createEntityManagerFactory("octobird-test");
    }

    @AfterAll
    static void closeFactory() {
        if (emf != null) {
            emf.close();
        }
    }

    @BeforeEach
    void cleanTable() {
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.createQuery("DELETE FROM AuditLogEntity").executeUpdate();
        tx.commit();
        em.close();
    }

    // --- Default pagination ---

    @Test
    void defaultPaginationReturns25EntriesNewestFirst() {
        // Given
        insertEntries(50, "SomeHandler", "some-action",
                LocalDateTime.of(2026, 3, 1, 0, 0));

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, null, null, null, null, 0, 25);
        em.close();

        // Then
        assertEquals(50, result.total());
        assertEquals(25, result.entries().size());
        assertTrue(isDescendingByTimestamp(result.entries()),
                "Entries must be ordered newest first");
    }

    // --- Custom offset and limit ---

    @Test
    void customOffsetReturnsSecondPage() {
        // Given
        insertEntries(50, "Handler", "action",
                LocalDateTime.of(2026, 3, 1, 0, 0));

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, null, null, null, null, 25, 25);
        em.close();

        // Then
        assertEquals(50, result.total());
        assertEquals(25, result.entries().size());
    }

    // --- Last page has fewer entries ---

    @Test
    void lastPageReturnsFewerEntries() {
        // Given
        insertEntries(50, "Handler", "action",
                LocalDateTime.of(2026, 3, 1, 0, 0));

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, null, null, null, null, 40, 25);
        em.close();

        // Then
        assertEquals(50, result.total());
        assertEquals(10, result.entries().size());
    }

    // --- Empty result ---

    @Test
    void emptyResultWhenNoEntriesExist() {
        // Given — no entries inserted

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, null, null, null, null, 0, 25);
        em.close();

        // Then
        assertEquals(0, result.total());
        assertTrue(result.entries().isEmpty());
    }

    // --- Filter by handler name ---

    @Test
    void filterByHandlerNameCaseInsensitiveSubstring() {
        // Given
        insertEntry("UnassignCommandHandler", "unassign",
                LocalDateTime.of(2026, 3, 10, 12, 0));
        insertEntry("MergeConflictHandler", "comment",
                LocalDateTime.of(2026, 3, 10, 13, 0));
        insertEntry("UnassignCommandHandler", "unassign",
                LocalDateTime.of(2026, 3, 10, 14, 0));

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, "unassign", null, null, null, 0, 25);
        em.close();

        // Then
        assertEquals(2, result.total());
        assertTrue(result.entries().stream()
                .allMatch(e -> e.getHandlerName().toLowerCase().contains("unassign")));
    }

    // --- Filter by action ---

    @Test
    void filterByAction() {
        // Given
        insertEntry("Handler1", "unassign", LocalDateTime.of(2026, 3, 10, 12, 0));
        insertEntry("Handler2", "comment", LocalDateTime.of(2026, 3, 10, 13, 0));
        insertEntry("Handler3", "close", LocalDateTime.of(2026, 3, 10, 14, 0));

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, null, "comment", null, null, 0, 25);
        em.close();

        // Then
        assertEquals(1, result.total());
        assertEquals("comment", result.entries().getFirst().getAction());
    }

    // --- Filter by date range ---

    @Test
    void filterByDateRangeInclusive() {
        // Given — entries from 2026-03-01 to 2026-03-20
        for (int day = 1; day <= 20; day++) {
            insertEntry("Handler", "action",
                    LocalDateTime.of(2026, 3, day, 12, 0));
        }

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, null, null,
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 15), 0, 25);
        em.close();

        // Then
        assertEquals(6, result.total());
        for (final AuditLogEntity entry : result.entries()) {
            final LocalDate date = entry.getCreatedAt().toLocalDate();
            assertFalse(date.isBefore(LocalDate.of(2026, 3, 10)));
            assertFalse(date.isAfter(LocalDate.of(2026, 3, 15)));
        }
    }

    // --- Filter with dateFrom only ---

    @Test
    void filterWithDateFromOnly() {
        // Given
        for (int day = 1; day <= 20; day++) {
            insertEntry("Handler", "action",
                    LocalDateTime.of(2026, 3, day, 12, 0));
        }

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, null, null,
                LocalDate.of(2026, 3, 15), null, 0, 25);
        em.close();

        // Then
        assertEquals(6, result.total()); // 15,16,17,18,19,20
        for (final AuditLogEntity entry : result.entries()) {
            assertFalse(entry.getCreatedAt().toLocalDate().isBefore(LocalDate.of(2026, 3, 15)));
        }
    }

    // --- Filter with dateTo only ---

    @Test
    void filterWithDateToOnly() {
        // Given
        for (int day = 1; day <= 20; day++) {
            insertEntry("Handler", "action",
                    LocalDateTime.of(2026, 3, day, 12, 0));
        }

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, null, null,
                null, LocalDate.of(2026, 3, 10), 0, 25);
        em.close();

        // Then
        assertEquals(10, result.total()); // 1..10
        for (final AuditLogEntity entry : result.entries()) {
            assertFalse(entry.getCreatedAt().toLocalDate().isAfter(LocalDate.of(2026, 3, 10)));
        }
    }

    // --- Multiple filters combined ---

    @Test
    void multipleFiltersCombinedWithAndLogic() {
        // Given
        insertEntry("AssignCommandHandler", "comment",
                LocalDateTime.of(2026, 3, 5, 10, 0));
        insertEntry("AssignCommandHandler", "comment",
                LocalDateTime.of(2026, 3, 15, 10, 0));
        insertEntry("UnassignCommandHandler", "comment",
                LocalDateTime.of(2026, 3, 15, 11, 0));
        insertEntry("AssignCommandHandler", "assign",
                LocalDateTime.of(2026, 3, 15, 12, 0));

        // When — handler contains "assign", action is "comment", dateFrom 2026-03-01
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, "assign", "comment",
                LocalDate.of(2026, 3, 1), null, 0, 25);
        em.close();

        // Then — both AssignCommandHandler+comment entries match (handler substring "assign" matches both handlers)
        assertEquals(3, result.total());
    }

    // --- Filters with pagination ---

    @Test
    void filtersWithPagination() {
        // Given — 40 entries matching handler=assign
        for (int i = 0; i < 40; i++) {
            insertEntry("AssignCommandHandler", "assign",
                    LocalDateTime.of(2026, 3, 1, 0, 0).plusMinutes(i));
        }
        insertEntry("OtherHandler", "other",
                LocalDateTime.of(2026, 3, 1, 12, 0));

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, "assign", null, null, null, 25, 25);
        em.close();

        // Then
        assertEquals(40, result.total());
        assertEquals(15, result.entries().size());
    }

    // --- Entries are scoped to repo ID ---

    @Test
    void entriesAreScopedToRepoId() {
        // Given
        insertEntry(REPO_ID, "Handler", "action", LocalDateTime.of(2026, 3, 10, 12, 0));
        insertEntry(OTHER_REPO_ID, "Handler", "action", LocalDateTime.of(2026, 3, 10, 13, 0));

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, null, null, null, null, 0, 25);
        em.close();

        // Then
        assertEquals(1, result.total());
    }

    // --- dateTo is inclusive (end-of-day entries are included) ---

    @Test
    void dateToIncludesEndOfDay() {
        // Given — entry at 23:59 on the dateTo date
        insertEntry("Handler", "action",
                LocalDateTime.of(2026, 3, 15, 23, 59, 59));

        // When
        final EntityManager em = emf.createEntityManager();
        final AuditLogRepository repo = new AuditLogRepository(em);
        final FilteredResult result = repo.findFiltered(REPO_ID, null, null,
                null, LocalDate.of(2026, 3, 15), 0, 25);
        em.close();

        // Then
        assertEquals(1, result.total());
    }

    // --- Helpers ---

    private void insertEntry(final String handlerName, final String action,
                             final LocalDateTime createdAt) {
        insertEntry(REPO_ID, handlerName, action, createdAt);
    }

    private void insertEntry(final long repoId, final String handlerName, final String action,
                             final LocalDateTime createdAt) {
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final AuditLogEntity entity = new AuditLogEntity();
        entity.setRepoId(repoId);
        entity.setRepoFullName(REPO_NAME);
        entity.setHandlerName(handlerName);
        entity.setAction(action);
        entity.setTarget("owner/repo#1");
        entity.setCreatedAt(createdAt);
        entity.setDetails("Test detail");
        em.persist(entity);
        tx.commit();
        em.close();
    }

    private void insertEntries(final int count, final String handlerName, final String action,
                               final LocalDateTime startTime) {
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        for (int i = 0; i < count; i++) {
            final AuditLogEntity entity = new AuditLogEntity();
            entity.setRepoId(REPO_ID);
            entity.setRepoFullName(REPO_NAME);
            entity.setHandlerName(handlerName);
            entity.setAction(action);
            entity.setTarget("owner/repo#" + i);
            entity.setCreatedAt(startTime.plusMinutes(i));
            entity.setDetails("Entry " + i);
            em.persist(entity);
        }
        tx.commit();
        em.close();
    }

    private boolean isDescendingByTimestamp(final List<AuditLogEntity> entries) {
        for (int i = 1; i < entries.size(); i++) {
            if (entries.get(i).getCreatedAt().isAfter(entries.get(i - 1).getCreatedAt())) {
                return false;
            }
        }
        return true;
    }
}
