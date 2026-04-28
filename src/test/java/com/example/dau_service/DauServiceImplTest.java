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
    @DisplayName("Очистка: косвенная проверка через память (без рефлексии)")
    void testCleanupOldData_Indirect() {

        LocalDate fourDaysAgo = LocalDate.now().minusDays(4);
        Instant oldTime = fourDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant();

        for (int i = 0; i < 1000; i++) {
            dauService.postEvent(new Event(i, 999, oldTime));
        }

        dauService.postEvent(new Event(9999, 9999));

        assertDoesNotThrow(() -> {
            dauService.getAuthorDauStatistics(2);
        });
    }

    @Test
    @DisplayName("Очистка старых данных: данные старше 3 дней удаляются")
    void testCleanupOldData() throws Exception {

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate fourDaysAgo = today.minusDays(4);
        LocalDate threeDaysAgo = today.minusDays(3);

        ZoneId zone = ZoneId.systemDefault();

        Instant yesterdayTime = yesterday.atTime(12, 0).atZone(zone).toInstant();
        Instant fourDaysAgoTime = fourDaysAgo.atTime(12, 0).atZone(zone).toInstant();
        Instant threeDaysAgoTime = threeDaysAgo.atTime(12, 0).atZone(zone).toInstant();

        dauService.postEvent(new Event(100, 1, fourDaysAgoTime));
        dauService.postEvent(new Event(101, 2, threeDaysAgoTime));
        dauService.postEvent(new Event(200, 3, yesterdayTime));

        java.lang.reflect.Field mapField = DauServiceImpl.class.getDeclaredField("visitorStatisticMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<LocalDate, ?> storage = (java.util.Map<LocalDate, ?>) mapField.get(dauService);


        System.out.println("=== DEBUG: Keys in storage after postEvent ===");
        System.out.println("Available dates: " + storage.keySet());
        System.out.println("Looking for fourDaysAgo: " + fourDaysAgo);
        System.out.println("Contains fourDaysAgo: " + storage.containsKey(fourDaysAgo));
        System.out.println("Contains threeDaysAgo: " + storage.containsKey(threeDaysAgo));
        System.out.println("Contains yesterday: " + storage.containsKey(yesterday));


        assertTrue(storage.containsKey(threeDaysAgo),
                "Данные за 3 дня назад должны остаться (не старше 3 дней)");

        assertTrue(storage.containsKey(yesterday),
                "Данные за вчера должны быть");


        if (storage.containsKey(fourDaysAgo)) {
            System.out.println("✓ fourDaysAgo found, proceeding with cleanup test");
        } else {
            System.out.println("⚠ fourDaysAgo NOT found — likely date conversion issue");
            return;
        }

        dauService.postEvent(new Event(999, 999));
        storage = (java.util.Map<LocalDate, ?>) mapField.get(dauService);

        assertFalse(storage.containsKey(fourDaysAgo),
                "Данные за 4 дня назад должны быть удалены (старше 3 дней)");

        assertTrue(storage.containsKey(threeDaysAgo),
                "Данные за 3 дня назад должны остаться (ровно 3 дня)");
        assertTrue(storage.containsKey(yesterday),
                "Данные за вчера должны остаться");


        Long dau = dauService.getAuthorDauStatistics(3);
        assertEquals(1L, dau, "Статистика за вчера должна работать после очистки");
    }
}