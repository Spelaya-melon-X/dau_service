package com.example.dau_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DauServiceImplTest {

    DauServiceImpl dauService = new DauServiceImpl();

    // Хелпер: создаёт метку времени "вчера 12:00"
    private Instant getYesterdayInstant() {
        return LocalDate.now()
                .minusDays(1)
                .atTime(12, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }

    @Test
    void getDauStatistics() {
        Instant yesterday = getYesterdayInstant();

        // Создаём события с явной датой "вчера"
        dauService.postEvent(new Event(1, 1, yesterday));
        dauService.postEvent(new Event(1, 2, yesterday));
        dauService.postEvent(new Event(1, 3, yesterday));

        Map<Integer, Long> result = dauService.getDauStatistics(List.of(1, 2, 3));

        assertEquals(1L, result.get(1));
        assertEquals(1L, result.get(2));
        assertEquals(1L, result.get(3));
    }

    @Test
    void testUniqueness_sameUserMultipleClicks() {
        Instant yesterday = getYesterdayInstant();

        // Один юзер кликает 5 раз по одному автору ВЧЕРА
        for (int i = 0; i < 5; i++) {
            dauService.postEvent(new Event(999, 42, yesterday));
        }

        Long dau = dauService.getAuthorDauStatistics(42);
        assertEquals(1L, dau, "Один пользователь должен считаться как 1, независимо от количества кликов");
    }

    @Test
    void testNoEvents_returnsZero() {
        Long dau = dauService.getAuthorDauStatistics(9999);
        assertEquals(0L, dau);
    }

    @Test
    @DisplayName("Параллельные события от разных потоков не ломают уникальность")
    void testConcurrentEvents() throws InterruptedException {
        Instant yesterday = getYesterdayInstant();

        int authorId = 100;
        int threadCount = 10;
        int eventsPerThread = 100;
        List<Thread> threads = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            int finalT = t;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < eventsPerThread; i++) {
                    int uniqueUserId = finalT * eventsPerThread + i;
                    dauService.postEvent(new Event(uniqueUserId, authorId, yesterday));
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Long dau = dauService.getAuthorDauStatistics(authorId);
        assertEquals(1000L, dau, "Все уникальные пользователи должны посчитаться корректно");
    }

    @Test
    @DisplayName("Запрос статистики до любых событий возвращает 0")
    void testGetStatsBeforeAnyEvents() {
        Map<Integer, Long> result = dauService.getDauStatistics(List.of(1, 2, 3));
        assertEquals(0L, result.get(1));
        assertEquals(0L, result.get(2));
        assertEquals(0L, result.get(3));
    }

    @Test
    @DisplayName("getAuthorDauStatistics для несуществующего автора не падает")
    void testGetSingleAuthorNonExistent() {
        assertDoesNotThrow(() -> {
            Long result = dauService.getAuthorDauStatistics(9999);
            assertEquals(0L, result);
        });
    }

    @Test
    @DisplayName("Один юзер кликает разным авторам — считается у каждого отдельно")
    void testOneUserMultipleAuthors() {
        Instant yesterday = getYesterdayInstant();
        int userId = 777;

        dauService.postEvent(new Event(userId, 1, yesterday));
        dauService.postEvent(new Event(userId, 2, yesterday));
        dauService.postEvent(new Event(userId, 3, yesterday));

        Map<Integer, Long> stats = dauService.getDauStatistics(List.of(1, 2, 3, 4));

        assertEquals(1L, stats.get(1));
        assertEquals(1L, stats.get(2));
        assertEquals(1L, stats.get(3));
        assertEquals(0L, stats.get(4));
    }

    @Test
    @DisplayName("Очистка старых данных: данные старше 3 дней удаляются")
    void testCleanupOldData() {
        // Этот тест пока заглушка, так как метод очистки не публичный
        // Можно реализовать, если добавишь public void cleanupOlderThan(int days)
    }
}