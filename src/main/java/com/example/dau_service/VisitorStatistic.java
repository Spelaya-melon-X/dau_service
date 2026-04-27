package com.example.dau_service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VisitorStatistic {
    Map<Integer , SetUsers> uniqueVisitors ;

    public VisitorStatistic() {
        this.uniqueVisitors = new ConcurrentHashMap<>();
    }

    public Map<Integer, SetUsers> getUniqueVisitors() {
        return uniqueVisitors;
    }

    void updateStatistic(Event event) {
        Integer userId = event.userId();
        Integer authorId = event.authorId();
        synchronized (uniqueVisitors) {
            if (!uniqueVisitors.containsKey(authorId)) {
                uniqueVisitors.put(authorId, new SetUsers());
            }
            uniqueVisitors.get(authorId).addNewUser(userId);
        }
    }

}
