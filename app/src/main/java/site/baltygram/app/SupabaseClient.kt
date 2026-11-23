package site.baltygram.app

import android.content.Context
import android.content.SharedPreferences
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object SupabaseClient {
 private const val BASE="https://atovuyugeqrbfhkpyhax.supabase.co"
 private const val ANON="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF0b3Z1eXVnZXFyYmZoa3B5aGF4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4MzYxNDAsImV4cCI6MjEwMjQxMjE0MH0.ZCCXf7do_qc0_5B1wvsZM_OPoP1Uo9v3lSyH-SXkDcc"
 private val http=OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(45,TimeUnit.SECONDS).build()
 private lateinit var prefs:SharedPreferences
 private var access=""; private var refresh=""
 fun init(ctx:Context){prefs=ctx.getSharedPreferences("bg_session",Context.MODE_PRIVATE);access=prefs.getString("access","")?:"";refresh=prefs.getString("refresh","")?:""}
 fun token()=access
 fun email()=prefs.getString("email","")?:""
 private fun request(path:String,method:String="GET",body:JSONObject?=null,auth:Boolean=true,api:Boolean=true):String{
  val b=Request.Builder().url(BASE+path).header("apikey",ANON)
  if(auth && access.isNotBlank()) b.header("Authorization","Bearer $access") else b.header("Authorization","Bearer $ANON")
  if(api)b.header("Accept","application/json")
  if(body!=null)b.header("Content-Type","application/json").method(method,body.toString().toRequestBody("application/json".toMediaType()))
  else if(method!="GET")b.method(method,ByteArray(0).toRequestBody(null))
  http.newCall(b.build()).execute().use{r->val s=r.body?.string().orEmpty();if(!r.isSuccessful)throw IOException(try{JSONObject(s).optString("message",JSONObject(s).optString("msg",JSONObject(s).optString("error", "HTTP "+r.code)))}catch(_:Exception){"HTTP "+r.code});return s}
 }
 private fun enc(s:String)=URLEncoder.encode(s,"UTF-8")
 private fun saveSession(o:JSONObject){access=o.optString("access_token");refresh=o.optString("refresh_token");val u=o.optJSONObject("user");prefs.edit().putString("access",access).putString("refresh",refresh).putString("email",u?.optString("email")?:"").apply()}
 fun signIn(email:String,password:String){val o=JSONObject(request("/auth/v1/token?grant_type=password","POST",JSONObject().put("email",email).put("password",password),false));saveSession(o)}
 fun signUp(email:String,password:String){request("/auth/v1/signup","POST",JSONObject().put("email",email).put("password",password),false)}
 fun resetPassword(email:String){request("/auth/v1/recover","POST",JSONObject().put("email",email),false)}
 fun signOut(){try{request("/auth/v1/logout","POST",JSONObject())}finally{access="";refresh="";prefs.edit().clear().apply()}}
 private fun arr(path:String)=JSONArray(request(path))
 private fun obj(path:String)=JSONObject(request(path))
 private fun parseListings(a:JSONArray):List<Listing> = (0 until a.length()).map{i->
  val x=a.getJSONObject(i)
  val vs=x.optJSONArray("software_versions")
  val v=if(vs!=null&&vs.length()>0)vs.getJSONObject(0) else JSONObject()
  Listing(x.optString("id"),x.optString("name"),x.optString("description",x.optString("short_description")),x.optString("software_type","Other"),x.optString("icon_url"),v.optString("id"),v.optString("external_url"),x.optInt("views"),x.optInt("download_count"),x.optString("created_at"),x.optJSONObject("category")?.optString("name").orEmpty())
 }
 fun listings():List<Listing>{
  return try {
   val a=arr("/rest/v1/software?select=id,name,short_description,description,software_type,icon_url,external_url:website_url,views,download_count,created_at,category:categories(name),software_versions(id,version,external_url)&status=eq.approved&deleted_at=is.null&order=is_featured.desc,created_at.desc&limit=100")
   prefs.edit().putString("catalogue_cache",a.toString()).apply()
   parseListings(a)
  } catch(e:Exception) {
   val cached=prefs.getString("catalogue_cache",null) ?: throw e
   parseListings(JSONArray(cached))
  }
 }
 fun categories():List<Category>{return try{val a=arr("/rest/v1/categories?select=id,name,software_type&order=name");(0 until a.length()).map{val x=a.getJSONObject(it);Category(x.optString("id"),x.optString("name"),x.optString("software_type"))}}catch(_:Exception){emptyList()}}
 fun myListings():List<CreatorListing>{if(access.isBlank())return emptyList();val uid=userId();val a=arr("/rest/v1/software?select=id,name,short_description,description,software_type,icon_url,views,download_count,created_at,status,category:categories(name),software_versions(id,version,external_url)&uploader_id=eq.$uid&deleted_at=is.null&order=created_at.desc&limit=100");return (0 until a.length()).map{i->val x=a.getJSONObject(i);val item=parseListings(JSONArray().put(x)).first();CreatorListing(item,x.optString("status","unknown"))}}
 fun recordRecent(id:String){val ids=try{JSONArray(prefs.getString("recent_ids","[]"))}catch(_:Exception){JSONArray()};val next=JSONArray().put(id);for(i in 0 until ids.length()){val old=ids.optString(i);if(old!=id&&next.length()<12)next.put(old)};prefs.edit().putString("recent_ids",next.toString()).apply()}
 fun recentListings(items:List<Listing>):List<Listing>{val ids=try{JSONArray(prefs.getString("recent_ids","[]"))}catch(_:Exception){JSONArray()};val byId=items.associateBy{it.id};return (0 until ids.length()).mapNotNull{byId[ids.optString(it)]}}
 fun favorites():Set<String>{if(access.isBlank())return emptySet();val a=arr("/rest/v1/favorites?select=software_id");return (0 until a.length()).map{a.getJSONObject(it).optString("software_id")}.toSet()}
 fun toggleFavorite(id:String,exists:Boolean){val uid=userId();if(exists)request("/rest/v1/favorites?user_id=eq.$uid&software_id=eq.$id","DELETE") else request("/rest/v1/favorites","POST",JSONObject().put("user_id",uid).put("software_id",id))}
 fun communities():List<Community>{val a=arr("/rest/v1/communities?select=id,name,description&is_active=eq.true&is_public=eq.true&order=name&limit=50");return (0 until a.length()).map{val x=a.getJSONObject(it);Community(x.optString("id"),x.optString("name"),x.optString("description"))}}
 fun joinCommunity(id:String){request("/rest/v1/community_members","POST",JSONObject().put("community_id",id).put("user_id",userId()).put("role","member"))}
 private fun userId():String=obj("/auth/v1/user").optString("id")
 fun notifications():List<Notice>{if(access.isBlank())return emptyList();val a=arr("/rest/v1/notifications?select=id,title,body&order=created_at.desc&limit=30");return (0 until a.length()).map{val x=a.getJSONObject(it);Notice(x.optString("id"),x.optString("title"),x.optString("body"))}}
 fun markAllNotificationsRead(){if(access.isBlank())throw IOException("Sign in to update notifications.");request("/rest/v1/notifications?read_at=is.null","PATCH",JSONObject().put("read_at",java.time.Instant.now().toString()))}
 fun inbox():List<JSONObject>{val a=arr("/rest/v1/dm_threads?select=id,user_a,user_b,last_message_at&order=last_message_at.desc&limit=50");return (0 until a.length()).map{a.getJSONObject(it)}}
 fun messages(id:String):List<JSONObject>{val a=arr("/rest/v1/messages?select=id,body,sender_id,created_at&dm_id=eq.$id&deleted_at=is.null&order=created_at.asc&limit=100");return (0 until a.length()).map{a.getJSONObject(it)}}
 fun sendMessage(id:String,body:String){request("/rest/v1/messages","POST",JSONObject().put("dm_id",id).put("sender_id",userId()).put("body",body))}
 fun download(item:Listing):String{if(access.isBlank())throw IOException("Sign in before downloading.")
  val response=JSONObject(request("/functions/v1/download-file","POST",JSONObject().put("version_id",item.versionId)))
  if(!response.optBoolean("success"))throw IOException(response.optString("error","Download unavailable"))
  return response.optString("url").ifBlank{throw IOException("No download URL returned")}
 }
}
