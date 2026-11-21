package site.baltygram.app

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException

object SupabaseClient {
    // Set these in GitHub Actions repository secrets before building.
    private const val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private const val SUPABASE_KEY = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val http = OkHttpClient()

    fun isConfigured() = SUPABASE_URL.isNotBlank() && SUPABASE_KEY.isNotBlank()

    fun fetchSoftware(): List<Listing> {
        if (!isConfigured()) return emptyList()
        val url = SUPABASE_URL.trimEnd('/') +
            "/rest/v1/software?select=id,name,description,category&limit=50"
        val request = Request.Builder().url(url)
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer $SUPABASE_KEY")
            .header("Accept", "application/json")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Supabase returned HTTP ${response.code}. Check table fields and Row Level Security.")
            val array = JSONArray(response.body?.string() ?: "[]")
            return (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Listing(
                    id = obj.optString("id"),
                    name = obj.optString("name", "Untitled"),
                    description = obj.optString("description", ""),
                    category = obj.optString("category", "Other")
                )
            }
        }
    }
}
