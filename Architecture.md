# Application Architecture

## Overview

Daily Expense Tracker is a single-module Android application written in Java. Its Android activities render the screens and coordinate user actions, while a Retrofit service communicates with the remote expense-tracker backend. Android resources provide the layouts, styling, and icons.

```text
User
  │
  ▼
Activities ───────────────► Android layouts and resources
  │
  ├──► SharedPreferences (small set of locally retained account fields)
  │
  ▼
ApiService (Retrofit interface)
  │
  ▼
ApiClient (Retrofit configuration)
  │
  ▼
Remote backend
```

The backend is the source of account, transaction, and budget data. Network requests are asynchronous Retrofit calls; activities handle responses and update their views.

## Main components

### Activities and user flows

| Component | Responsibility |
| --- | --- |
| `LoginActivity` | Validates a mobile number, requests the account from the backend, retains account context, and opens the dashboard. |
| `RegisterActivity` | Validates registration and account fields, submits a user and account, then opens the dashboard. |
| `MainActivity` | Loads the account balance and transaction statistics; displays a credit/debit chart and navigation to the transaction, history, and budget screens. |
| `AddTransactionActivity` | Collects transaction details and submits a payment/transaction request; checks budget usage for notifications. |
| `TransactionHistoryActivity` | Requests transactions, maps response data to `TransactionModel`, and displays them in a `RecyclerView`. |
| `BudgetActivity` | Creates or updates a budget, retrieves transaction and budget data, and displays budget usage. |
| `VpaActivity` | Loads and updates VPA details and presents a QR code. |

`BaseActivity` provides shared back-navigation behavior. `TransactionAdapter` binds transaction models to transaction list rows. `NotificationHelper` creates the Android notification channel and displays budget alerts.

### API and models

- `api/ApiClient.java` creates the Retrofit instance and defines the backend base URL.
- `api/ApiService.java` declares the HTTP endpoints for registration, account lookup/update, payment creation, transaction queries, budgets, reports, and QR retrieval.
- `Model/` contains the account, user, transaction, payment-request, and API-response data classes used by the activities and service.

The activities currently parse several backend responses using generic `Map` and `List` values as well as model objects.

### Resources and configuration

- `res/layout/` contains activity and transaction-row layouts.
- `res/drawable/`, `res/mipmap/`, and `res/values/` contain visual assets, the app icon, and theme/string resources.
- `assets/config.yml` is loaded by `Config`; it is separate from the Retrofit base URL, which is currently declared in `ApiClient`.
- `AndroidManifest.xml` declares the launcher activity, app screens, internet access, and notification permission.

## Data flow

1. The user registers or enters a mobile number to sign in.
2. The relevant activity sends an asynchronous request through `ApiService`.
3. `ApiClient` supplies the Retrofit configuration and JSON converter.
4. On a successful response, the activity maps the returned data to models or reads response fields, then updates the UI. Login also saves selected account context in `SharedPreferences`.
5. Dashboard, transaction, budget, and payment screens make further requests as required. Budget-use thresholds can trigger Android notifications.

`SharedPreferences` is used for a limited set of account/session fields; transaction and budget records are requested from the backend rather than being managed by a local database in this app.

## Build configuration

The Android application module is `app`. It uses Java 17, Android Gradle Plugin 8.2.0, compile/target SDK 34, and minimum SDK 24. Dependencies include AndroidX AppCompat, Material Components, ConstraintLayout, RecyclerView, Retrofit with Gson, MPAndroidChart, ZXing, and SnakeYAML.
