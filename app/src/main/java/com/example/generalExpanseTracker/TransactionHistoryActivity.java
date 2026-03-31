package com.example.generalExpanseTracker;

import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

import java.util.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.example.generalExpanseTracker.api.ApiClient;
import com.example.generalExpanseTracker.api.ApiService;
import com.example.generalExpanseTracker.Model.TransactionModel;
import com.example.generalExpanseTracker.Adapter.TransactionAdapter;
import com.example.generalExpanseTracker.BaseActivity;

import retrofit2.*;

public class TransactionHistoryActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<TransactionModel> list = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        enableBackButton();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(list);
        recyclerView.setAdapter(adapter);
        apiService = ApiClient.getClient().create(ApiService.class);
        loadTransactions();
    }

    private void loadTransactions() {
        String username = getSharedPreferences("app", MODE_PRIVATE).getString("username", "");
        Log.d("username", username);
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        apiService.getTransactions(body).enqueue(new Callback<Map<String, Object>>() {

            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {

                if (response.body() != null) {
                    Log.d("API_RESPONSE", response.body().toString());
                }

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> res = response.body();
                        Object dataObj = res.get("data");
                        list.clear();

                        if (dataObj instanceof List) {
                            List<?> rawList = (List<?>) dataObj;

                            for (Object obj : rawList) {
                                if (obj instanceof Map) {
                                    Map<String, Object> item = (Map<String, Object>) obj;
                                    String bankName = safeString(item.get("bankName"));
                                    String desc = safeString(item.get("transactionDesc"));
                                    String type = safeString(item.get("type"));
                                    String category = safeString(item.get("categories"));
                                    double amount = safeDouble(item.get("transactionAmount"));
                                    long time = safeLong(item.get("time"));
                                    TransactionModel txn = new TransactionModel(bankName, desc, type, category, amount,
                                            time);
                                    list.add(txn);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        Log.e("PARSE_ERROR", e.getMessage());
                        notification("Parse error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                notification("Error: " + t.getMessage());
            }
        });
    }

    private String safeString(Object obj) {
        return obj != null ? String.valueOf(obj) : "";
    }

    private double safeDouble(Object obj) {
        try {
            return obj != null ? Double.parseDouble(String.valueOf(obj)) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private long safeLong(Object obj) {
        try {
            return obj != null ? (long) Double.parseDouble(String.valueOf(obj)) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private void notification(String string) {
        Toast.makeText(TransactionHistoryActivity.this, string, Toast.LENGTH_SHORT).show();
    }
}