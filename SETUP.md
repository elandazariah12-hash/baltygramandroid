# Setup and current scope

- Native Kotlin / Jetpack Compose interface.
- Home, Explore, search/filter, listing detail, theme toggle.
- Supabase REST catalogue read scaffold.
- GitHub Actions workflow builds an APK; Android Studio is not required.
- No website code or database schema is changed by this project.

## Not implemented yet
Full sign-up/sign-in, uploads, downloads, offline media, saved-items sync, reviews, notifications, developer profiles, community/chat, push notifications and production release signing. These should be added in stages after verifying the live Supabase schema and RLS policies.

## Supabase schema note
The starter expects `software` to expose `id`, `name`, `description`, and `category`. If your project stores category IDs or uses different field names, update the REST select and JSON mapping accordingly. Keep the `software-packages` bucket private and do not expose service-role credentials.
