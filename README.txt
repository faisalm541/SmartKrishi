╔══════════════════════════════════════════════════╗
║                  SMART KRISHI                    ║
║     AI-Based Crop Recommendation Android App     ║
╚══════════════════════════════════════════════════╝

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
FOLDER STRUCTURE (copy exactly as shown)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

SmartKrishi/
├── build.gradle.kts                   ← Project level
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradle/
│   ├── libs.versions.toml             ← Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
└── app/
    ├── build.gradle.kts               ← App level
    ├── proguard-rules.pro
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── java/
            │   └── com/example/smartkrishi/
            │       ├── Theme.kt
            │       ├── DatabaseHelper.kt
            │       ├── SplashActivity.kt
            │       ├── LoginActivity.kt
            │       ├── HomeActivity.kt
            │       ├── CropInputActivity.kt
            │       ├── ResultActivity.kt
            │       ├── HistoryActivity.kt
            │       ├── MarketActivity.kt
            │       └── ProfileActivity.kt
            └── res/
                └── values/
                    ├── strings.xml
                    └── themes.xml

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
HOW TO SET UP IN ANDROID STUDIO
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

OPTION A — Use files from this ZIP (Recommended):

Step 1: Open Android Studio
Step 2: Create New Project
         → Empty Activity
         → Name: SmartKrishi
         → Package: com.example.smartkrishi
         → Language: Kotlin
         → Min SDK: 24

Step 3: Replace these files from the ZIP:
         ✅ gradle/libs.versions.toml
         ✅ app/build.gradle.kts
         ✅ build.gradle.kts
         ✅ settings.gradle.kts
         ✅ app/src/main/AndroidManifest.xml
         ✅ app/src/main/res/values/themes.xml
         ✅ app/src/main/res/values/strings.xml

Step 4: DELETE the default MainActivity.kt

Step 5: Copy all .kt files into:
         app/src/main/java/com/example/smartkrishi/

Step 6: Click "Sync Now" in the yellow bar

Step 7: Press ▶️ Run

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
APP SCREENS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. SplashActivity    → Animated splash (2.8 sec auto-navigate)
2. LoginActivity     → Login + Register tabs with Firebase Auth
3. HomeActivity      → Dashboard: weather, stats, 4 feature cards
4. CropInputActivity → Soil & climate input form with chips
5. ResultActivity    → AI crop result with progress bars + share
6. HistoryActivity   → Saved recommendations from SQLite
7. MarketActivity    → Market prices with search + trend arrows
8. ProfileActivity   → Farmer profile + SIH badge + logout

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TECH STACK
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

- Kotlin + Jetpack Compose (Material 3)
- SQLite via SQLiteOpenHelper (NO Room, NO KSP)
- No external dependencies — works 100% offline
- Package: com.example.smartkrishi
- Min SDK: 24 (Android 7.0+)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
COMMON ERRORS & FIXES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Error: "Unresolved reference"
Fix:   Make sure libs.versions.toml is replaced correctly
       and Gradle sync is done

Error: "Theme.SmartKrishi not found"
Fix:   Replace res/values/themes.xml with the one in zip

Error: App crashes on launch
Fix:   Delete old MainActivity.kt if it still exists
       Make sure SplashActivity is the launcher in Manifest

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Smart India Hackathon 2025
Package: com.example.smartkrishi
Version: 1.0
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
