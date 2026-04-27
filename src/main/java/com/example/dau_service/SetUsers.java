package com.example.dau_service;

import java.util.HashSet;
import java.util.Set;

public class SetUsers {
    private Set<Integer> users;

    public SetUsers() {
        this.users = new HashSet<>();
    }
    public SetUsers(Integer firstUser) {
        users = new HashSet<>();
        users.add(firstUser);
    }

    void addNewUser(Integer userId) {
        users.add(userId);
    }

    Long getCountUser() {
        return (long) users.size();
    }

    boolean isEmptySetUsers() {
        return users.isEmpty();
    }



}
