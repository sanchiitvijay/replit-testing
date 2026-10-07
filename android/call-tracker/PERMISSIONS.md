# Permissions

The manifest requests only:

| Android permission | Required? | Purpose |
|---|---|---|
| `READ_CALL_LOG` | Yes | Read call rows that Android exposes so the app can find the last outgoing call and display history. |
| `READ_CONTACTS` | Optional | Match local phone numbers to contact names and photos. Without it, unmatched callers remain unknown. |

The app has no `INTERNET` permission. It does not request write access to call logs or contacts, SMS access, microphone/camera access, notification-listener access, accessibility access, or the default phone role.

## User flow

- On first launch, the app explains local call-history use and requests `READ_CALL_LOG`.
- It then requests `READ_CONTACTS`, with a clear option to continue without contacts.
- If a permission is revoked, the app notices when it resumes. Call-history access is required to import; the contacts step can be skipped.
- After Android stops showing the runtime permission prompt, the app directs the user to its Android app settings.
- The settings screen shows current access and links to Android’s app-permission settings.

The runtime permission is enforced by Android. The explanation is not a substitute for Android’s permission dialog.
