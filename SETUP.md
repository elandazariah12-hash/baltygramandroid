# Build setup

1. Keep this repository's existing GitHub Actions workflow.
2. Optional repository secrets: `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` (public client key only).
3. Open Actions → Build BaltyGram Android APK → Run workflow.
4. Download `baltygram-debug-apk` from the completed run.

Never store a Supabase service-role key in GitHub Actions or in the Android client.
