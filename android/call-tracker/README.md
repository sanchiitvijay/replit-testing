# Last Called

Last Called is a native Android app that answers **“Who haven’t I called recently?”** It reads the call history and contacts Android exposes, keeps its working data in a local Room database, and runs without an account, backend, internet permission, analytics, or cloud sync.

## Open and run

1. Open `android/call-tracker` in Android Studio.
2. Install a JDK 17-compatible Android Studio runtime, Android SDK Platform 37, and Android Build Tools through SDK Manager.
3. Let Android Studio sync the Gradle project.
4. Run the `app` configuration on an Android 8.0 (API 26) or newer device or emulator.
5. Grant call-history access. Contacts access is optional; without it, callers that cannot be matched stay labeled as unknown.

From a machine with Java 17+, Android SDK Platform 37, and an emulator/device configured:

```sh
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## What it does

- Imports up to the 10,000 newest rows visible in Android’s call log on refresh. Existing imported rows are retained and upserted by the system call-log ID.
- Imports local contacts and phone numbers when `READ_CONTACTS` is granted; matches with normalized numbers and Android’s phone-number comparison APIs.
- Keeps unmatched/private numbers inspectable without inventing a person or call.
- Shows each person’s last outgoing call, relative and full date, duration, source, and call history.
- Filters by time since the last outgoing call, searches names/numbers, sorts the check-in list, and filters person history by direction, source, and call type.
- Supports manual refresh and refresh when the app returns to the foreground.
- Lets the user reset imported call rows or clear the app’s local data. Neither action changes Android’s call log or WhatsApp data.

For data ownership and storage details, see [PRIVACY.md](PRIVACY.md) and [DATA_MODEL.md](DATA_MODEL.md). For permission behavior, see [PERMISSIONS.md](PERMISSIONS.md). For calling-app and WhatsApp coverage, see [WHATSAPP_SUPPORT.md](WHATSAPP_SUPPORT.md). Build and device test steps are in [BUILD_AND_TESTING.md](BUILD_AND_TESTING.md).

## Project layout

- `app/src/main/java/app/lastcalled/data/platform/` — Android CallLog and Contacts provider readers.
- `app/src/main/java/app/lastcalled/data/local/` — Room entities, database, and queries.
- `app/src/main/java/app/lastcalled/data/CallRepository.kt` — import, matching, deduplication, and local reset.
- `app/src/main/java/app/lastcalled/domain/` — call classification and relative-time logic.
- `app/src/main/java/app/lastcalled/ui/` — Compose screens and lifecycle-aware state.
- `app/src/test/` — local JVM tests for pure classification and time formatting.
- `app/src/androidTest/` — device tests for Room, phone matching, and permission onboarding.
