package com.example.generalExpanseTracker.Model;

public class PaymentRequest {

    private String bankName;
    private String username;
    private double transactionAmount;
    private String transactionDesc;
    private String type;
    private String categories;

    public PaymentRequest(String bankName, String username,
            double transactionAmount, String transactionDesc, String type, String categories) {
        this.bankName = bankName;
        this.username = username;
        this.transactionAmount = transactionAmount;
        this.transactionDesc = transactionDesc;
        this.type = type;
        this.categories = categories;
    }
}