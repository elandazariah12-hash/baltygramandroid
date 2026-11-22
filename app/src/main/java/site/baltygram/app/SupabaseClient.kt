package site.baltygram.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object SupabaseClient {
 private const val BASE="https://atovuyugeqrbfhkpyhax.supabase.co"
 private const val LEGACY_ANON="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF0b3Z1eXVnZXFyYmZoa3B5aGF4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4MzYxNDAsImV4cCI6MjEwMjQxMjE0MH0.ZCCXf7do_qc0_5B1wvsZM_OPoP1Uo9v3lSyH-SXkDcc"
 private val http=OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(45,TimeUnit.SECONDS).build()
 private lateinit var prefs:SharedPreferences
 private var access="";private var refresh=""
 private val apiKey get()=BuildConfig.SUPABASE_PUBLISHABLE_KEY.ifBlank{LEGACY_ANON}
 fun init(ctx:Context){prefs=ctx.getSharedPreferences("bg_session",Context.MODE_PRIVATE);access=prefs.getString("access","")?:"";refresh=prefs.getString("refresh","")?:""}
 fun token()=access
 fun email()=prefs.getString("email","")?:""
 private fun enc(s:String)=URLEncoder.encode(s,"UTF-8")
 private fun save(o:JSONObject){access=o.optString("access_token");refresh=o.optString("refresh_token");val user=o.optJSONObject("user");prefs.edit().putString("access",access).putString("refresh",refresh).putString("email",user?.optString("email")?:"").apply()}
 private fun call(path:String,method:String="GET",body:JSONObject?=null,auth:Boolean=true,retry:Boolean=true):String{
  val b=Request.Builder().url(BASE+path).header("apikey",apiKey).header("Accept","application/json")
  b.header("Authorization","Bearer "+if(auth&&access.isNotBlank())access else apiKey)
  if(body!=null)b.header("Content-Type","application/json").method(method,body.toString().toRequestBody("application/json".toMediaType()))
  else if(method!="GET")b.method(method,ByteArray(0).toRequestBody(null))
  http.newCall(b.build()).execute().use{r->
   val text=r.body?.string().orEmpty()
   if(!r.isSuccessful){
    if(r.code==401&&retry&&refresh.isNotBlank()&&path!="/auth/v1/token?grant_type=refresh_token"){
     val payload=JSONObject().put("refresh_token",refresh)
     val req=Request.Builder().url("$BASE/auth/v1/token?grant_type=refresh_token").header("apikey",apiKey).header("Authorization","Bearer $apiKey").header("Content-Type","application/json").post(payload.toString().toRequestBody("application/json".toMediaType())).build()
     http.newCall(req).execute().use{rr->val rs=rr.body?.string().orEmpty();if(rr.isSuccessful){save(JSONObject(rs));return call(path,method,body,auth,false)}}
    }
    val msg=try{val j=JSONObject(text);j.optString("message",j.optString("msg",j.optString("error_description",j.optString("error","HTTP "+r.code))))}catch(_:Exception){"HTTP "+r.code}
    throw IOException(msg)
   }
   return text
  }
 }
 private suspend fun req(path:String,method:String="GET",body:JSONObject?=null,auth:Boolean=true)=withContext(Dispatchers.IO){call(path,method,body,auth)}
 suspend fun signIn(email:String,password:String){req("/auth/v1/token?grant_type=password","POST",JSONObject().put("email",email).put("password",password),false).let{save(JSONObject(it))}}
 suspend fun signUp(name:String,email:String,password:String){req("/auth/v1/signup","POST",JSONObject().put("email",email).put("password",password).put("data",JSONObject().put("full_name",name)),false)}
 suspend fun resetPassword(email:String){req("/auth/v1/recover","POST",JSONObject().put("email",email),false)}
 suspend fun signOut(){try{req("/auth/v1/logout","POST")}finally{access="";refresh="";prefs.edit().clear().apply()}}
 suspend fun googleAuthUrl():String{
  val bytes=ByteArray(48);SecureRandom().nextBytes(bytes)
  val verifier=Base64.encodeToString(bytes,Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  val challenge=Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()),Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  prefs.edit().putString("oauth_verifier",verifier).apply()
  val redirect="site.baltygram.app://auth/callback"
  return "$BASE/auth/v1/authorize?provider=google&redirect_to="+enc(redirect)+"&code_challenge=$challenge&code_challenge_method=s256"
 }
 suspend fun exchangeOAuthCode(code:String){val verifier=prefs.getString("oauth_verifier","")?:throw IOException("Sign-in expired. Try again.");val raw=req("/auth/v1/token?grant_type=pkce","POST",JSONObject().put("auth_code",code).put("code_verifier",verifier),false);save(JSONObject(raw));prefs.edit().remove("oauth_verifier").apply()}
 private suspend fun arr(path:String)=JSONArray(req(path))
 private suspend fun userId()=JSONObject(req("/auth/v1/user")).optString("id")
 suspend fun listings():List<Listing>{
  val a=arr("/rest/v1/software?select=id,name,short_description,description,software_type,icon_url,cover_url,website_url,publisher_name,tags,screenshots,is_featured,software_versions(id,version,external_url)&status=eq.approved&deleted_at=is.null&order=is_featured.desc,created_at.desc&limit=100")
  return (0 until a.length()).map{i->val x=a.getJSONObject(i);val vs=x.optJSONArray("software_versions");val v=if(vs!=null&&vs.length()>0)vs.getJSONObject(0)else JSONObject();Listing(x.optString("id"),x.optString("name"),x.optString("short_description"),x.optString("description"),x.optString("software_type","other"),x.optString("icon_url"),x.optString("cover_url"),x.optString("website_url"),x.optString("publisher_name"),v.optString("id"),v.optString("version"),v.optString("external_url"),x.optBoolean("is_featured"),strings(x.optJSONArray("tags")),strings(x.optJSONArray("screenshots")))}
 }
 private fun strings(a:JSONArray?):List<String>{if(a==null)return emptyList();return (0 until a.length()).mapNotNull{a.optString(it).takeIf{s->s.isNotBlank()&&s!="null"}}}
 suspend fun favorites():Set<String>{if(access.isBlank())return emptySet();val a=arr("/rest/v1/favorites?select=software_id");return (0 until a.length()).map{a.getJSONObject(it).optString("software_id")}.toSet()}
 suspend fun toggleFavorite(id:String,exists:Boolean){val uid=userId();if(exists)req("/rest/v1/favorites?software_id=eq.$id&user_id=eq.$uid","DELETE")else req("/rest/v1/favorites","POST",JSONObject().put("user_id",uid).put("software_id",id))}
 suspend fun communities():List<Community>{val a=arr("/rest/v1/communities?select=id,name,description,icon_url&is_active=eq.true&is_public=eq.true&order=name&limit=60");return(0 until a.length()).map{val x=a.getJSONObject(it);Community(x.optString("id"),x.optString("name"),x.optString("description"),x.optString("icon_url"))}}
 suspend fun joinCommunity(id:String){req("/rest/v1/community_members","POST",JSONObject().put("community_id",id).put("user_id",userId()).put("role","member"))}
 suspend fun notifications():List<Notice>{if(access.isBlank())return emptyList();val a=arr("/rest/v1/notifications?select=id,title,body,read_at&order=created_at.desc&limit=50");return(0 until a.length()).map{val x=a.getJSONObject(it);Notice(x.optString("id"),x.optString("title"),x.optString("body"),!x.isNull("read_at"))}}
 suspend fun markNotificationRead(id:String){req("/rest/v1/notifications?id=eq.$id","PATCH",JSONObject().put("read_at",java.time.Instant.now().toString()))}
 suspend fun inbox():List<Conversation>{if(access.isBlank())return emptyList();val uid=userId();val a=arr("/rest/v1/dm_threads?select=id,user_a,user_b,last_message_at&order=last_message_at.desc&limit=60");val rows=(0 until a.length()).map{a.getJSONObject(it)};val ids=rows.map{if(it.optString("user_a")==uid)it.optString("user_b")else it.optString("user_a")}.distinct();val profiles=if(ids.isEmpty())JSONArray()else arr("/rest/v1/profiles?select=id,display_name,username,avatar_url&id=in.("+ids.joinToString(",")+")");val map=(0 until profiles.length()).associateBy({profiles.getJSONObject(it).optString("id")},{profiles.getJSONObject(it)});return rows.map{r->val other=if(r.optString("user_a")==uid)r.optString("user_b")else r.optString("user_a");val p=map[other]?:JSONObject();Conversation(r.optString("id"),other,p.optString("display_name",p.optString("username","Conversation")),p.optString("avatar_url"),"Open conversation")}}
 suspend fun messages(id:String):List<JSONObject>{val a=arr("/rest/v1/messages?select=id,body,sender_id,created_at&dm_id=eq.$id&deleted_at=is.null&order=created_at.asc&limit=150");return(0 until a.length()).map{a.getJSONObject(it)}}
 suspend fun sendMessage(id:String,body:String){req("/rest/v1/messages","POST",JSONObject().put("dm_id",id).put("sender_id",userId()).put("body",body))}
 suspend fun download(item:Listing):String{if(access.isBlank())throw IOException("Sign in before downloading.");val o=JSONObject(req("/functions/v1/download-file","POST",JSONObject().put("version_id",item.versionId)));if(!o.optBoolean("success"))throw IOException(o.optString("error","Download unavailable"));return o.optString("url").ifBlank{throw IOException("No download URL returned.")}}
 suspend fun downloadHistory():List<DownloadRow>{val a=arr("/rest/v1/downloads?select=id,status,software(name),software_versions(version)&deleted_from_history=eq.false&order=downloaded_at.desc&limit=60");return(0 until a.length()).map{val x=a.getJSONObject(it);DownloadRow(x.optString("id"),x.optJSONObject("software")?.optString("name")?:"Download",x.optJSONObject("software_versions")?.optString("version")?:"",x.optString("status"))}}
}
