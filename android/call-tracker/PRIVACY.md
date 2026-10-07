# Privacy and offline behavior

Last Called is designed to keep call and contact information on the Android device.

## Data flow

1. With permission, the app reads entries from Android’s `CallLog` content provider and names, photos, and phone numbers from Android’s local `Contacts` provider.
2. It matches call numbers to contacts locally.
3. It stores imported snapshots and call records in the app-private Room database, `last-called.db`.
4. Compose screens read those local records to display the check-in list and person history.

The app does not send this information to a server. It has no `INTERNET` permission, account flow, analytics SDK, telemetry, upload, or cloud-sync code. Android’s app-private storage and OS permission controls protect the local database. Android backup is disabled for this application.

## Data held locally

- Contact identifier, display name, primary phone number, photo URI, and whether the contact is still available in the address book.
- Imported phone-number mappings used for matching.
- System call-log row ID, contact link, name/number snapshot, timestamp, duration, direction, source metadata, and call type.
- App preferences: default day threshold, refresh-on-foreground setting, and last successful import time.

Permission request history is held in the app’s ordinary private preferences so the app can detect when Android settings should be opened after a repeated denial.

## Retention and deletion

Refreshes update imported call rows by Android’s call-log row ID; they do not erase older rows already imported into Last Called. This preserves history if Android later prunes the device call log. Contact removals are marked unavailable so existing calls can retain their historical association.

**Reset imported call records** deletes this app’s saved call rows and unknown-caller groups. **Clear all app data** deletes the app’s local database and resets its preferences. Neither action deletes or changes the device call log, the address book, or WhatsApp’s private data. Android’s system “Clear storage” also removes this app’s local state.

## No notification monitoring

The app does not ask for notification access, accessibility access, call-screening roles, or default-dialer status. It does not inspect notifications or other apps’ private storage.
