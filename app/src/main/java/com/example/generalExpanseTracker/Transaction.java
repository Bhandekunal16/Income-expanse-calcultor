package com.example.generalExpanseTracker;

public class Transaction {
    private String title;
    private double amount;
    private boolean isIncome;

    public Transaction(String title, double amount, boolean isIncome) {
        this.title = title;
        this.amount = amount;
        this.isIncome = isIncome;
    }

    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public boolean isIncome() { return isIncome; }
}