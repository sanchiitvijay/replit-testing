# Local data model

All application data is stored in the app-private Room database `last-called.db`. The system CallLog and Contacts providers remain the source of new imports.

## Tables

### `contacts`

One row per imported contact or stable unknown-number group. It stores the provider-derived ID, display name, primary display number, photo URI, unknown/available flags, and local create/update times. A known contact removed from Android Contacts is marked unavailable instead of deleted while call history still refers to it.

Unknown callers use a stable key based on a normalized number. Calls with withheld numbers use a key based on the system call-row ID because there is no safe basis for grouping them.

### `contact_phones`

Maps each current contact to each phone number from the Contacts provider. This table is refreshed only when contacts access is available. It has a composite contact-and-number key and a number index.

### `call_records`

Stores the Android call-log row ID as the primary key, linked contact, contact-name snapshot, number snapshot, timestamp in milliseconds, duration in seconds, direction, source, call type, account component metadata, and local create/update times. The system ID is the import deduplication key.

Direction values include outgoing, incoming, missed, rejected/blocked, voicemail, answered externally, screened, and unknown. Source is Phone, WhatsApp, or Unknown. Media type is Voice, Video, or Unknown.

### `app_settings`

A single row stores the default “not called for” threshold, refresh-on-foreground preference, and last successful import time.

## Import and matching rules

- A refresh reads at most the latest 10,000 provider rows. It upserts by system ID and does not delete previously imported calls absent from a later provider result.
- On Android releases that expose integrated calling-app history, the app uses the platform’s VoIP-inclusive call-log query. Source is called WhatsApp only when the provider’s account-component metadata identifies the WhatsApp or WhatsApp Business package. Unknown third-party components remain Unknown.
- Video is identified only from Android’s video feature flag. A missing feature field yields Unknown rather than a guess.
- Contact matching first compares normalized phone numbers, then uses Android’s phone comparison APIs for formatting/country-code variants. A suffix alone is not accepted as a match.
- Each person’s “last called” time is the maximum timestamp of their outgoing rows. Incoming, missed, voicemail, and screened rows do not count as the user having called that person.
- The filter threshold is based on the most recent outgoing call. People with no outgoing call are included in every threshold view.
- Person history is newest-first and currently displays up to 250 most recent imported rows for that person.

## Schema evolution

Room schema export is enabled. When changing entities, add an explicit Room migration and its exported schema rather than silently dropping local data.
