# BaltyGram Native Android

Kotlin + Jetpack Compose native Android client (no WebView wrapper).

## Navigation
Four bottom destinations: Home, Explore, Upload and Account. The hamburger drawer contains Categories, Find people, My uploads, Downloads, Saved, Creator Studio, Communities, Messages, Notifications, Organizations, About, Contact and appearance settings.

## Connected features
- Approved software catalogue, images, categories, search and listing details.
- Supabase email/password authentication and Google OAuth using the existing Google provider.
- Saved listings, public communities and membership, direct-message inbox and messages, notifications and download history.
- Private downloads through the existing `download-file` Edge Function.
- Upload wizard and some long-form pages currently open the website's existing workflow; a fully native creator upload flow still needs the website's CAPTCHA, storage and review pipeline ported.

## Google OAuth
Add `site.baltygram.app://auth/callback` to Supabase Authentication → URL Configuration → Redirect URLs. The app uses a PKCE code exchange.

## Build
GitHub Actions uploads the debug APK as `baltygram-debug-apk`.