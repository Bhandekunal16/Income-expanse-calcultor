package com.example.generalExpanseTracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.generalExpanseTracker.api.ApiClient;
import com.example.generalExpanseTracker.api.ApiService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class SessionManager {
    private static final String PREFERENCES_NAME = "app";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_USERNAME = "username";
    private static volatile boolean unlocked;

    private SessionManager() {
    }

    public interface ValidationCallback {
        void onValid(Map<String, Object> account);

        void onInvalid();

        void onError(String message);
    }

    public static boolean hasStoredSession(Context context) {
        String mobile = getStoredMobile(context);
        return mobile.matches("^[6-9]\\d{9}$");
    }

    public static String getStoredMobile(Context context) {
        return preferences(context).getString(KEY_MOBILE, "").trim();
    }

    public static void saveSession(Context context, String mobile, String username) {
        preferences(context).edit()
                .putString(KEY_MOBILE, mobile)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public static void markUnlocked() {
        unlocked = true;
    }

    public static boolean isUnlocked() {
        return unlocked;
    }

    public static void lock() {
        unlocked = false;
    }

    public static void clearSession(Context context) {
        preferences(context).edit().clear().apply();
        lock();
    }

    public static void validateSavedSession(Context context, ValidationCallback callback) {
        String mobile = getStoredMobile(context);
        if (!mobile.matches("^[6-9]\\d{9}$")) {
            callback.onInvalid();
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("mobile", mobile);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.loginUser(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(
                    Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("The account service is unavailable. Please try again.");
                    return;
                }

                Map<String, Object> result = response.body();
                if (Boolean.FALSE.equals(result.get("status"))) {
                    callback.onInvalid();
                    return;
                }
                if (!Boolean.TRUE.equals(result.get("status"))) {
                    callback.onError("The account service returned an invalid response.");
                    return;
                }

                Object dataValue = result.get("data");
                if (!(dataValue instanceof Map)) {
                    callback.onError("The account service returned incomplete account data.");
                    return;
                }

                Map<String, Object> account = (Map<String, Object>) dataValue;
                Object usernameValue = account.get("username");
                Object accountMobileValue = account.get("mobile");
                if (usernameValue == null
                        || String.valueOf(usernameValue).trim().isEmpty()
                        || accountMobileValue == null
                        || !mobile.equals(String.valueOf(accountMobileValue))) {
                    callback.onError("The account service returned incomplete account data.");
                    return;
                }

                persistAccountDetails(context, mobile, account);
                callback.onValid(account);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable throwable) {
                String message = throwable.getLocalizedMessage();
                callback.onError(message == null
                        ? "Unable to verify the saved account. Check your connection and try again."
                        : "Unable to verify the saved account: " + message);
            }
        });
    }

    public static void persistAccountDetails(
            Context context,
            String mobile,
            Map<String, Object> account) {
        SharedPreferences.Editor editor = preferences(context).edit()
                .putString(KEY_MOBILE, mobile)
                .putString(KEY_USERNAME, String.valueOf(account.get(KEY_USERNAME)));

        Object accountsValue = account.get("accounts");
        if (accountsValue instanceof List && !((List<?>) accountsValue).isEmpty()) {
            Object firstAccount = ((List<?>) accountsValue).get(0);
            if (firstAccount instanceof Map) {
                Map<?, ?> details = (Map<?, ?>) firstAccount;
                putIfPresent(editor, "bankName", details.get("bankName"));
                putIfPresent(editor, "accountNumber", details.get("number"));
                Object balance = details.get("balance");
                if (balance != null) {
                    try {
                        editor.putFloat("balance", Float.parseFloat(String.valueOf(balance)));
                    } catch (NumberFormatException exception) {
                        Log.w("SessionManager", "Ignoring an invalid account balance from the backend.");
                    }
                }
            }
        }
        editor.apply();
    }

    private static void putIfPresent(
            SharedPreferences.Editor editor,
            String key,
            Object value) {
        if (value != null) {
            editor.putString(key, String.valueOf(value));
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
