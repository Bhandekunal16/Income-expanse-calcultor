package com.example.generalExpanseTracker.Model;

import java.util.List;

import com.example.generalExpanseTracker.Model.Account;

public class User {

    private String name;
    private String mobile;
    private String email;
    private String username;
    private String password;
    private List<Account> accounts;

    public User(String name, String mobile, String email,
            String username, String password, List<Account> accounts) {
        this.name = name;
        this.mobile = mobile;
        this.email = email;
        this.username = username;
        this.password = password;
        this.accounts = accounts;
    }
}