# BaltyGram Android

Native Android starter for BaltyGram using Kotlin + Jetpack Compose. It does not use a WebView.

## Build without Android Studio
1. Create a GitHub repository for this Android project.
2. Upload the contents of this ZIP (the files inside the folder, not the outer folder if you prefer a clean root).
3. In GitHub: Settings → Secrets and variables → Actions, add:
   - `SUPABASE_URL`
   - `SUPABASE_PUBLISHABLE_KEY`
4. Open Actions → Build BaltyGram Android APK → Run workflow.
5. Download the `baltygram-debug-apk` artifact from the completed workflow.

Use only your Supabase project URL and public publishable/anon key. Never put a service-role or secret key in an Android app.

## Important
This is a native starter foundation, not yet a full feature-for-feature replacement for BaltyGram's website. The current client attempts to read a basic software catalogue. Your live `software` table may use different column names or category relations; adjust the select and mapping in `SupabaseClient.kt` to match your actual schema. It does not change your website or Supabase database.
