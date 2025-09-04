# Finance Tracker 📱💰

> **Smart Transaction Analyzer** - Intelligent SMS-based transaction tracking and categorization for Android

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Material Design 3](https://img.shields.io/badge/UI-Material%20Design%203-blue.svg)](https://m3.material.io)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 🚀 Features

### 📱 Core Functionality
- **Automatic SMS Transaction Detection** - Parses bank SMS messages to extract transaction details
- **Smart Categorization** - AI-powered transaction categorization with manual override
- **Multi-Bank Support** - Works with all major Indian banks (HDFC, ICICI, SBI, Axis, Kotak, etc.)
- **Real-time Processing** - Instant transaction detection and processing
- **OTP Filtering** - Intelligently ignores OTP and transaction initiation messages

### 💡 Smart Features
- **Auto Low-Value Categorization** - Automatically categorizes transactions below ₹100 as "Low Value"
- **Pending Transaction Management** - Review, tag, or ignore detected transactions
- **Manual Transaction Entry** - Add transactions manually via floating action button
- **Advanced Filtering** - Filter by date range, amount, category, and bank account
- **Intelligent Sorting** - Sort by date, amount, or recent activity
- **Cross-Tab Synchronization** - Changes reflect instantly across all tabs

### 🎨 User Experience
- **Material Design 3** - Modern, clean interface following latest design guidelines
- **Dark Theme Support** - Automatic dark/light theme switching
- **Pagination** - Efficient handling of large transaction datasets (10 items per page)
- **Loading States** - Professional loading indicators for all async operations
- **Gesture Navigation** - Swipe between tabs for seamless navigation
- **Responsive Design** - Optimized for all screen sizes

### 🏦 Account Management
- **Multiple Bank Accounts** - Manage transactions from multiple accounts
- **Account Profiles** - Store bank name, account number, and account type
- **Profile Section** - Clean menu-based navigation system

## 📸 Screenshots

*Coming soon - Screenshots of the app in action*

## 🛠️ Technical Stack

### Architecture
- **MVVM Pattern** - Clean separation of concerns with ViewModel
- **Repository Pattern** - Centralized data management
- **Dependency Injection** - Hilt for clean dependency management
- **Single Activity Architecture** - Navigation Component with fragments

### Technologies
- **Language**: Kotlin
- **UI Framework**: Android Views with Material Design 3
- **Database**: Room (SQLite)
- **Async Processing**: Kotlin Coroutines + Flow
- **Background Work**: WorkManager
- **Navigation**: Navigation Component
- **State Management**: LiveData + StateFlow
- **SMS Processing**: BroadcastReceiver + Custom Parser

### Libraries & Dependencies
```kotlin
// Core Android
implementation "androidx.core:core-ktx:1.12.0"
implementation "androidx.appcompat:appcompat:1.6.1"
implementation "androidx.activity:activity-ktx:1.8.2"
implementation "androidx.fragment:fragment-ktx:1.6.2"

// Material Design
implementation "com.google.android.material:material:1.11.0"

// Architecture Components
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.7.0"
implementation "androidx.navigation:navigation-fragment-ktx:2.7.6"
implementation "androidx.navigation:navigation-ui-ktx:2.7.6"

// Database
implementation "androidx.room:room-runtime:2.6.1"
implementation "androidx.room:room-ktx:2.6.1"
kapt "androidx.room:room-compiler:2.6.1"

// Dependency Injection
implementation "com.google.dagger:hilt-android:2.48.1"
kapt "com.google.dagger:hilt-compiler:2.48.1"

// Background Processing
implementation "androidx.work:work-runtime-ktx:2.9.0"
implementation "androidx.hilt:hilt-work:1.1.0"

// UI Components
implementation "androidx.recyclerview:recyclerview:1.3.2"
implementation "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0"
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24 (API level 24) or higher
- Kotlin 1.9.0 or later

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/vishnuteja98/FinanceTracker.git
   cd FinanceTracker
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory and select it

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run on device/emulator**
   ```bash
   ./gradlew installDebug
   ```

### Required Permissions
The app requires the following permissions:
- `READ_SMS` - To read bank transaction messages
- `RECEIVE_SMS` - To receive new SMS messages
- `WRITE_EXTERNAL_STORAGE` - For CSV export (Android 10 and below)

## 📱 Usage

### First Time Setup
1. **Grant Permissions** - Allow SMS permissions when prompted
2. **Add Bank Accounts** - Navigate to Profile → Bank Accounts to add your accounts
3. **Send Test SMS** - The app will automatically detect new transaction messages

### Managing Transactions
1. **Pending Tab** - Review newly detected transactions
2. **Tag Transactions** - Assign categories to pending transactions
3. **Transactions Tab** - View all categorized transactions
4. **Manual Entry** - Use the + button to add transactions manually

### Filtering & Search
- **Date Range** - Filter transactions by custom date ranges
- **Categories** - Filter by transaction categories
- **Amount Range** - Filter by transaction amounts
- **Bank Accounts** - Filter by specific bank accounts

## 🔧 Configuration

### SMS Parser Customization
The SMS parser can be customized to support additional banks by modifying:
```kotlin
// app/src/main/java/com/financetracker/services/SmsTransactionParser.kt
private val BANK_PATTERNS = mapOf(
    "YOUR_BANK" to BankPattern(
        amountPattern = "Rs\\.?([\\d,]+(?:\\.\\d{2})?)",
        transactionTypePattern = "(debited|credited)",
        // Add your bank's SMS patterns here
    )
)
```

### Categories Customization
Add or modify transaction categories in:
```kotlin
// app/src/main/java/com/financetracker/data/models/Transaction.kt
object TransactionCategories {
    fun getAllCategories(): List<String> {
        return listOf(
            "Food & Dining", "Shopping", "Transportation",
            "Bills & Utilities", "Healthcare", "Entertainment",
            // Add your custom categories here
        )
    }
}
```

## 🧪 Testing

### Running Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### SMS Testing
For testing SMS parsing without real messages:
```bash
# Send test SMS via ADB
adb emu sms send 12345 "Dear Customer, Rs.450.00 debited from A/c XX7890 on 16-Jan-25 for SWIGGY. Avl Bal: Rs.8,200.45. -HDFC Bank"
```

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Commit your changes**
   ```bash
   git commit -m 'Add some amazing feature'
   ```
4. **Push to the branch**
   ```bash
   git push origin feature/amazing-feature
   ```
5. **Open a Pull Request**

### Development Guidelines
- Follow Kotlin coding conventions
- Use meaningful commit messages
- Add unit tests for new features
- Update documentation as needed
- Ensure Material Design guidelines compliance

## 📋 Roadmap

### Upcoming Features
- [ ] **Export Functionality** - CSV/PDF export of transactions
- [ ] **Budget Tracking** - Set and monitor spending budgets
- [ ] **Analytics Dashboard** - Spending insights and trends
- [ ] **Backup & Sync** - Cloud backup with Google Drive
- [ ] **Multi-language Support** - Localization for Indian languages
- [ ] **Widgets** - Home screen widgets for quick transaction overview
- [ ] **Notifications** - Smart spending alerts and reminders
- [ ] **UPI Integration** - Direct UPI payment support

### Planned Improvements
- [ ] **Machine Learning** - Better transaction categorization
- [ ] **OCR Support** - Extract data from bill images
- [ ] **Voice Commands** - Add transactions via voice input
- [ ] **Wear OS Support** - Companion app for smartwatches

## 🐛 Known Issues

- SMS parsing may vary between different bank message formats
- Some edge cases in amount extraction for certain banks
- Dark theme may need refinement in some UI components

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support, email tejavishnu.p@gmail.com or create an issue on GitHub.

## 🙏 Acknowledgments

- **Material Design 3** - For the beautiful design system
- **Android Jetpack** - For the robust architecture components
- **Kotlin Team** - For the amazing programming language
- **Open Source Community** - For inspiration and resources

## 📊 Project Stats

- **Language**: Kotlin (100%)
- **Files**: 120+ source files
- **Lines of Code**: 7,200+
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)

---

<p align="center">
  Made with ❤️ for better financial management
</p>

<p align="center">
  <a href="https://github.com/vishnuteja98/FinanceTracker/issues">Report Bug</a>
  ·
  <a href="https://github.com/vishnuteja98/FinanceTracker/issues">Request Feature</a>
  ·
  <a href="https://github.com/vishnuteja98/FinanceTracker/discussions">Discussions</a>
</p>