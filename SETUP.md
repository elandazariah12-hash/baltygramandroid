# BaltyGram Android setup

- Application ID: `site.baltygram.app`
- Minimum Android: 7.0 (API 24)
- Supabase project: Baltygram
- Google provider: shared with the website. Add `site.baltygram.app://auth/callback` to Supabase Authentication → URL Configuration → Redirect URLs before testing Google sign-in.
- Optional GitHub Actions secrets: `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` (public key only).
- Run Actions → Build BaltyGram Android APK and download `baltygram-debug-apk`.

Never add a Supabase service-role or secret key to Android or GitHub.