package com.example.generalExpanseTracker.Model;

public class Account {

    private String type;
    private String number;
    private String bankName;
    private double balance;

    public Account(String type, String number, String bankName, double balance) {
        this.type = type;
        this.number = number;
        this.bankName = bankName;
        this.balance = balance;
    }
}