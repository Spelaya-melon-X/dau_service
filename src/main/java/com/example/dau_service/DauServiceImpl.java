package com.example.dau_service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DauServiceImpl  implements  DauService{
    LocalDate lastUpdateDate;
    Map<LocalDate , VisitorStatistic> visitorStatisticMap;


    public DauServiceImpl() {
        this.visitorStatisticMap = new ConcurrentHashMap<>();
        LocalDate today = LocalDate.now();
        lastUpdateDate = today;
        visitorStatisticMap.put(today , new VisitorStatistic()) ;
        visitorStatisticMap.put(today.minusDays(1) , new VisitorStatistic()) ;

    }

    private void updateDate() {
        synchronized (visitorStatisticMap) {
            LocalDate dateNow = LocalDate.now();
            if (!dateNow.isEqual(lastUpdateDate)) {
                visitorStatisticMap.put(dateNow, new VisitorStatistic());
                lastUpdateDate = dateNow;
            }
            visitorStatisticMap.keySet().removeIf(d -> d.isBefore(LocalDate.now().minusDays(3)));
        }
    }

    @Override
    public void postEvent(Event event) {
        LocalDate eventDate = event.timestamp() != null
                ? event.timestamp().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();

        updateDate();

        synchronized (visitorStatisticMap) {
            visitorStatisticMap.computeIfAbsent(eventDate, k -> new VisitorStatistic());
            visitorStatisticMap.get(eventDate).updateStatistic(event);
        }
    }

    @Override
    public Map<Integer, Long> getDauStatistics(List<Integer> authorIds) {
        Map<Integer, Long> result = new HashMap<>();

        LocalDate yesterday = LocalDate.now().minusDays(1);

        synchronized (visitorStatisticMap) {

            VisitorStatistic visitorStatistic = visitorStatisticMap.get(yesterday);
            if (visitorStatistic == null) {
                for (var authorId : authorIds) {
                    result.put(authorId , 0L);
                }
                return result;
            }

            for ( var authorId : authorIds) {
                if ( visitorStatistic.uniqueVisitors.containsKey(authorId)) {
                    result.put( authorId , visitorStatistic.uniqueVisitors.get(authorId).getCountUser() ) ;
                }else {
                    result.put(authorId , 0L);
                }
            }
        }
        return result;
    }

    @Override
    public Long getAuthorDauStatistics(int authorId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        synchronized (visitorStatisticMap) {
            VisitorStatistic visitorStatistic = visitorStatisticMap.get(yesterday);
            if (visitorStatistic == null) {
                return 0L;
            }

            if (visitorStatistic.uniqueVisitors.containsKey(authorId)) {
                return visitorStatistic.uniqueVisitors.get(authorId).getCountUser();
            }else {
                return 0L;
            }
        }

    }
}
