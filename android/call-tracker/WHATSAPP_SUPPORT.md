# WhatsApp and other calling apps

Last Called does **not** read WhatsApp’s private storage, database, backups, or notification contents. It does not reconstruct or fabricate call events that Android does not expose.

## What can appear

On Android versions and device configurations that provide unified call history, a WhatsApp call may be present in Android’s system call log. This app imports only rows returned by Android. It labels a row WhatsApp only when Android provides account-component metadata identifying `com.whatsapp` or `com.whatsapp.w4b`.

The Android 17/API 37 call-log URI includes VoIP rows. Android 16.1 also supports the provider’s `include_voip_calls=true` query parameter. Which app entries are available still depends on the Android release, device implementation, calling app, user settings, and whether that app integrates with the system call log. The query parameter can be ignored on Android 16 builds that do not implement it.

If Android returns a row without reliable source metadata, Last Called labels its source Unknown. Older Android releases may expose only ordinary phone calls.

## What cannot appear

- WhatsApp calls that are absent from Android’s exposed call log.
- Private WhatsApp history, media, or messages.
- A reliable source label when Android does not provide identifying metadata.
- A reliable voice/video classification when the platform row has no video feature flag.

No notification listener is required or implemented. Turning on Android’s per-app call-history sharing, when that control exists, is a system/app setting outside this app; this app cannot override it.
