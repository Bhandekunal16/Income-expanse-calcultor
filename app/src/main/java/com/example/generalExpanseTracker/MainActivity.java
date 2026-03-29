package com.example.generalExpanseTracker;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;

import com.example.generalExpanseTracker.api.ApiClient;
import com.example.generalExpanseTracker.api.ApiService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    TextView tvBalance, tvMonthlySpend;
    Button btnAddExpense;

    BarChart barChart;

    ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBalance = findViewById(R.id.tvBalance);
        tvMonthlySpend = findViewById(R.id.tvMonthlySpend);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        barChart = findViewById(R.id.BarChart);

        apiService = ApiClient.getClient().create(ApiService.class);

        String mobile = getIntent().getStringExtra("mobile");

        String username = getSharedPreferences("app", MODE_PRIVATE)
                .getString("username", "");

        float balance = getSharedPreferences("app", MODE_PRIVATE)
                .getFloat("balance", 0f);

        tvBalance.setText(String.valueOf(balance));

        loadDashboardData(mobile);
        loadStatistics(username); // 🔥 GRAPH CALL

        btnAddExpense.setOnClickListener(v -> startActivity(new Intent(this, AddTransactionActivity.class)));
    }

    private void loadDashboardData(String mobile) {

        Map<String, String> body = new HashMap<>();
        body.put("mobile", mobile);

        apiService.getMonthlyTxn(body).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                tvMonthlySpend.setText("Spent: ₹ " + response.body());
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔥 NEW METHOD FOR GRAPH
    private void loadStatistics(String username) {

        if (username == null || username.isEmpty())
            return;

        Map<String, String> body = new HashMap<>();
        body.put("username", username);

        apiService.getStatistics(body).enqueue(new Callback<Map<String, Object>>() {

            @Override
            public void onResponse(Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    try {
                        Map<String, Object> res = response.body();
                        Map<String, Object> data = (Map<String, Object>) res.get("data");

                        List<String> dates = (List<String>) data.get("date");

                        Map<String, Object> dateWise = (Map<String, Object>) data.get("dateWise");

                        List<BarEntry> creditEntries = new ArrayList<>();
                        List<BarEntry> debitEntries = new ArrayList<>();

                        int index = 0;

                        for (String date : dates) {

                            List<Map<String, Object>> txns = (List<Map<String, Object>>) dateWise.get(date);

                            float creditSum = 0f;
                            float debitSum = 0f;

                            if (txns != null) {
                                for (Map<String, Object> txn : txns) {

                                    String type = (String) txn.get("type");

                                    double amount = Double.parseDouble(
                                            txn.get("transactionAmount").toString());

                                    if ("credit".equalsIgnoreCase(type)) {
                                        creditSum += amount;
                                    } else {
                                        debitSum += amount;
                                    }
                                }
                            }

                            creditEntries.add(new BarEntry(index, creditSum));
                            debitEntries.add(new BarEntry(index, debitSum));

                            index++;
                        }

                        renderChart(dates, creditEntries, debitEntries);

                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,
                                "Parse error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Graph error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔥 CHART RENDER
    private void renderChart(List<String> labels,
            List<BarEntry> creditEntries,
            List<BarEntry> debitEntries) {

        BarDataSet creditSet = new BarDataSet(creditEntries, "Credit");
        creditSet.setColor(Color.GREEN);

        BarDataSet debitSet = new BarDataSet(debitEntries, "Debit");
        debitSet.setColor(Color.RED);

        BarData data = new BarData(creditSet, debitSet);
        data.setBarWidth(0.35f);

        barChart.setData(data);

        barChart.getXAxis().setAxisMinimum(0);
        barChart.getXAxis().setAxisMaximum(labels.size());

        barChart.groupBars(0f, 0.2f, 0.05f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        barChart.getDescription().setText("Transactions");
        barChart.animateY(1000);
        barChart.invalidate();
    }
}