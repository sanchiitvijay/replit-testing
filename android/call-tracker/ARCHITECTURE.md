# Architecture

The app is a single native Android application written in Kotlin. UI uses Jetpack Compose and Material 3; local persistence uses Room over SQLite. There is no backend or network client.

```text
Compose screens
    ↓ actions / collected flows
CallTrackerViewModel
    ↓
CallRepository
    ├── CallLogDataSource → Android CallLog ContentProvider
    ├── ContactsDataSource → Android Contacts ContentProvider
    └── Room DAOs → app-private SQLite
```

## Responsibilities

- **Platform data sources** check the relevant runtime permission and read provider rows on the repository’s I/O dispatcher.
- **PhoneNumberMatcher** normalizes numbers, matches exact canonical numbers first, and validates likely variants with Android telephony utilities.
- **CallRepository** imports contacts and calls in a Room transaction, preserves unmatched and removed-contact history, upserts calls by provider ID, and owns app-local reset behavior.
- **Room DAOs** expose reactive person summaries, last outgoing calls, app settings, and newest-first person history.
- **CallTrackerViewModel** owns screen state, filtering inputs, refresh status, permission-related messages, and settings actions.
- **Compose screens** present onboarding, check-in list, person history, and settings. They do not access providers or databases directly.

## Threading and lifecycle

Content-provider reads and reset operations run off the main thread. Room-backed `Flow`s keep summaries and history current after imports. Foreground refresh is controlled by a local preference and can be disabled. There is no background polling or scheduled job.

## Failure behavior

The app does not invent provider results. Call-history permission denial blocks import. A failed call-log read reports that refresh did not complete. Contacts permission can be skipped, leaving caller matching incomplete while existing local snapshots remain. Missing call source or media metadata is stored and shown as Unknown.
