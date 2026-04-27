package com.example.dau_service;

import java.util.List;
import java.util.Map;

public interface DauService {

    void postEvent(Event event);

    Map<Integer, Long> getDauStatistics(List<Integer> authorIds);

    Long getAuthorDauStatistics(int authorId);

}