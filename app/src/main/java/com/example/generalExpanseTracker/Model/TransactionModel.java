package com.example.generalExpanseTracker.Model;

public class TransactionModel {

    public String bankName;
    public String desc;
    public String type;
    public String category;
    public double amount;
    public long time;

    public TransactionModel(String bankName, String desc, String type,
            String category, double amount, long time) {
        this.bankName = bankName;
        this.desc = desc;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.time = time;
    }
}