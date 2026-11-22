# BaltyGram Native Android

Kotlin + Jetpack Compose client for BaltyGram, connected to the existing Supabase project. No WebView.

## Build
Open Actions → Build BaltyGram Android APK → Run workflow. The workflow uploads `app-debug.apk` as `baltygram-debug-apk`.

## Security
The app uses the public Supabase anon key and authenticated user JWTs. Never add a service-role key to this repository or Android app. RLS remains authoritative.

## Implemented in this pass
- Native Home and Explore catalogue with search and type filters
- Listing details and signed download link flow through `download-file`
- Supabase email/password sign-in, sign-up, password reset and sign-out
- Favorites, public community discovery and membership
- Direct-message inbox and sending, in-app notifications
- Creator Studio links to the website's existing upload workflow
- Light/dark theme toggle

## Notes
The upload workflow remains on the website because it includes file selection, media processing, CAPTCHA and review submission. Push notifications and full background transfer queue require additional platform configuration. The Android build must be verified by GitHub Actions before treating the APK as release-ready.
