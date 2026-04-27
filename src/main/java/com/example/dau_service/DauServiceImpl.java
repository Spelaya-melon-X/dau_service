package com.example.dau_service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DauServiceImpl  implements  DauService{
    LocalDate lastUpdateDate;
    Map<LocalDate , VisitorStatistic> visitorStatisticMap;


    public DauServiceImpl() {
        this.visitorStatisticMap = new HashMap<>();
        LocalDate createDate = LocalDate.now();
        lastUpdateDate = createDate;
        visitorStatisticMap.put(createDate , new VisitorStatistic()) ;
    }

    private void updateDate() {
        synchronized (visitorStatisticMap) {
            LocalDate dateNow = LocalDate.now();
            long daysBetween = Math.abs(ChronoUnit.DAYS.between(dateNow, lastUpdateDate));
            if (daysBetween >= 1 ) {
                visitorStatisticMap.put(dateNow, new VisitorStatistic());
                lastUpdateDate = dateNow;
            }
        }
    }

    @Override
    public void postEvent(Event event) {
        updateDate();
        synchronized (visitorStatisticMap) {
            visitorStatisticMap.get(lastUpdateDate).updateStatistic(event);
        }
    }

    @Override
    public Map<Integer, Long> getDauStatistics(List<Integer> authorIds) {
        Map<Integer, Long> result = new HashMap<>();
        updateDate();
        synchronized (visitorStatisticMap) {
            VisitorStatistic visitorStatistic = visitorStatisticMap.get(lastUpdateDate);
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
        updateDate();
        synchronized (visitorStatisticMap) {
            VisitorStatistic visitorStatistic = visitorStatisticMap.get(lastUpdateDate);
            if (visitorStatistic.uniqueVisitors.containsKey(authorId)) {
                return visitorStatistic.uniqueVisitors.get(authorId).getCountUser();
            }else {
                return 0L;
            }
        }

    }
}
