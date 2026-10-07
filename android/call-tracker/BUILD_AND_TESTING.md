# Build and test

## Requirements

- Android Studio with a JDK 17-compatible runtime
- Android SDK Platform 37 and matching Build Tools
- Android 8.0/API 26 or newer device/emulator
- Gradle distribution 9.5.0 (the wrapper is checked into this project)

## Commands

From `android/call-tracker`:

```sh
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

Windows users can use `gradlew.bat` with the same task names.

## Automated coverage

- JVM tests: Android call-type classification (including unknown sources and video flags) and relative-time formatting.
- Device tests: Room upsert/deduplication, “never called” ordering, phone-number normalization and comparison, and permission-onboarding copy.

Device tests require an emulator/device connected through Android Studio or `adb`.

## Manual device checklist

1. Install on API 26 or newer and verify the first-run permission explanation.
2. Deny call-history permission, then verify access can be retried and can be opened in Android settings after repeated denial.
3. Grant call history and skip contacts; verify callers without a match remain inspectable as unknown.
4. Grant both permissions and refresh; verify names, multiple phone numbers, photos, and call summaries on real device data.
5. Make/receive calls and verify outgoing vs incoming/missed, durations, date/time, number matching, unknown numbers, and deduplication after repeated refresh.
6. Search by name and number; try all time thresholds and all three sort orders.
7. Open a person and exercise source, direction, and call-type history filters.
8. Revoke contacts permission and verify stored contact history remains usable and the app clearly shows that access is off.
9. Revoke call-history permission and verify the app returns to the permission flow.
10. Reset imported calls, then clear app data; confirm the actual Android call log remains unchanged.
11. On Android 16.1/API 36.1 and Android 17/API 37, compare imported rows with the system call history where calling-app integration is enabled. Verify an unidentified third-party call remains Unknown.
12. Inspect the merged manifest and confirm it contains no `INTERNET` permission.

Do not use fabricated records as evidence of device call history. Database and matcher test fixtures are isolated test data only.
