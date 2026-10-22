# Daily Expense Tracker

Daily Expense Tracker is an Android app for managing income and expenses. It provides account registration and login, a transaction dashboard, transaction history, budgets, and payment QR/VPA screens.

## Features

- Register an account and sign in with a mobile number.
- View the account balance and a credit/debit chart on the dashboard.
- Add transactions and browse transaction history.
- Create or update a budget and receive budget-usage notifications.
- View and update payment VPA details and display a QR code.

The app retrieves and updates account, transaction, and budget data through the configured backend service. An internet connection is required for these operations.

## Requirements

- Android Studio or a compatible Android build environment.
- JDK 17.
- Android SDK Platform 34.
- Android Gradle Plugin 8.2.0 (resolved by Gradle).

The Gradle wrapper is included, so a separate Gradle installation is not needed.

## Build

From the repository root:

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

Run local unit tests with:

```bash
./gradlew test
```

Instrumented tests require a connected Android device or emulator:

```bash
./gradlew connectedAndroidTest
```

## Backend

The Retrofit base URL is currently defined in `app/src/main/java/com/example/generalExpanseTracker/api/ApiClient.java`. The app requires that service to be reachable for account, transaction, budget, and payment operations. Update the base URL there if building against a different backend environment.

## Project structure

```text
app/src/main/
├── AndroidManifest.xml
├── assets/
├── java/com/example/generalExpanseTracker/
│   ├── api/       # Retrofit client and endpoint declarations
│   ├── Model/     # Request and response data models
│   └── ...        # Activities and UI helpers
└── res/
    ├── drawable/
    ├── layout/
    └── values/
```

See [artitature.md](artitature.md) for the application architecture and screen/data flow.
