package site.baltygram.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

data class Listing(val id:String,val name:String,val description:String,val type:String,val icon:String="",val versionId:String="",val externalUrl:String="")
data class Community(val id:String,val name:String,val description:String)
data class Notice(val id:String,val title:String,val body:String)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); SupabaseClient.init(applicationContext); setContent { BaltyGramApp { url -> startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) } }}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BaltyGramApp(openUrl:(String)->Unit){
 var page by remember { mutableStateOf("Home") }; var dark by remember { mutableStateOf(false) }
 var listings by remember { mutableStateOf(listOf<Listing>()) }; var categories by remember { mutableStateOf(listOf<String>()) }
 var communities by remember { mutableStateOf(listOf<Community>()) }; var notices by remember { mutableStateOf(listOf<Notice>()) }
 var saved by remember { mutableStateOf(setOf<String>()) }; var selected by remember { mutableStateOf<Listing?>(null) }
 var query by remember { mutableStateOf("") }; var filter by remember { mutableStateOf("All") }
 var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var fullName by remember { mutableStateOf("") }
 var userEmail by remember { mutableStateOf(SupabaseClient.email()) }; var message by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
 var inbox by remember { mutableStateOf(listOf<JSONObject>()) }; var chatRows by remember { mutableStateOf(listOf<JSONObject>()) }; var chatId by remember { mutableStateOf("") }; var chatText by remember { mutableStateOf("") }
 val scope= rememberCoroutineScope()
 fun runTask(block:suspend()->Unit){ scope.launch { busy=true; message=""; try{block()}catch(e:Exception){message=e.message ?: "Something went wrong"}finally{busy=false} } }
 fun refresh(){ runTask { listings=SupabaseClient.listings(); categories=SupabaseClient.categories(); if(SupabaseClient.token().isNotBlank()){ saved=SupabaseClient.favorites(); communities=SupabaseClient.communities(); notices=SupabaseClient.notifications() } } }
 LaunchedEffect(Unit){ runTask { listings=SupabaseClient.listings(); categories=SupabaseClient.categories(); if(userEmail.isNotBlank()){saved=SupabaseClient.favorites(); communities=SupabaseClient.communities(); notices=SupabaseClient.notifications()} } }
 MaterialTheme(colorScheme=if(dark) darkColorScheme(primary=androidx.compose.ui.graphics.Color(0xFF10B981),secondary=androidx.compose.ui.graphics.Color(0xFF6EE7B7)) else lightColorScheme(primary=androidx.compose.ui.graphics.Color(0xFF059669),secondary=androidx.compose.ui.graphics.Color(0xFF10B981))){
 Scaffold(topBar={TopAppBar(title={Text("BaltyGram",fontWeight=FontWeight.Bold)},actions={
 IconButton(onClick={dark=!dark}){Icon(if(dark) Icons.Default.LightMode else Icons.Default.DarkMode,"Theme")}
 IconButton(onClick={ {page="Notifications"; runTask{notices=SupabaseClient.notifications()}} }){BadgedBox(badge={if(notices.isNotEmpty()) Badge { Text(notices.size.toString()) }}){Icon(Icons.Default.Notifications,"Notifications")}}
 IconButton(onClick={::refresh}){Icon(Icons.Default.Refresh,"Refresh")}
 })},bottomBar={NavigationBar{listOf("Home","Explore","Community","Inbox","Saved","Account").forEach{p->NavigationBarItem(selected=page==p,onClick={page=p;selected=null},icon={Icon(when(p){"Home"->Icons.Default.Home;"Explore"->Icons.Default.Explore;"Community"->Icons.Default.Groups;"Inbox"->Icons.Default.Chat;"Saved"->Icons.Default.Bookmark;else->Icons.Default.Person},p)},label={Text(p)})}}}){pad->
 Column(Modifier.fillMaxSize().padding(pad)){
 if(message.isNotBlank()) Text(message,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(horizontal=16.dp,vertical=6.dp))
 if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
 if(selected!=null) ListingDetail(selected!!,saved.contains(selected!!.id),onBack={selected=null},onSave={runTask{if(SupabaseClient.token().isBlank()) throw Exception("Sign in to save listings."); SupabaseClient.toggleFavorite(selected!!.id,saved.contains(selected!!.id)); saved=SupabaseClient.favorites()}},onDownload={item->runTask{val url=SupabaseClient.download(item);openUrl(url)}},openUrl=openUrl)
 else when(page){
 "Home"->LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
 item{Text("Discover digital tools.",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Games, apps, software and resources from developers.",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(8.dp));Button(onClick={page="Explore"}){Text("Explore catalogue")}}
 item{Text("Featured discoveries",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
 if(listings.isEmpty()) item{Text("No approved listings yet. Pull to refresh when your catalogue is populated.")}
 items(listings.take(12)){ListingCard(it){selected=it}}
 }
 "Explore"->Column(Modifier.fillMaxSize().padding(16.dp)){
 OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Search BaltyGram")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
 Spacer(Modifier.height(8.dp)); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){(listOf("All","game","software","app","ai","editing","developer")).forEach{FilterChip(selected=filter.equals(it,true),onClick={filter=it},label={Text(it.replaceFirstChar { c->c.uppercase() })})}}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(listings.filter{(filter=="All"||it.type.contains(filter,true))&&(it.name.contains(query,true)||it.description.contains(query,true))}){ListingCard(it){selected=it}}}
 }
 "Community"->LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
 item{Text("Communities",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Find your people and join the conversation.")}
 if(SupabaseClient.token().isBlank()) item{Text("Sign in to join communities.")}
 items(communities){c->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(c.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(c.description);Button(onClick={runTask{SupabaseClient.joinCommunity(c.id);communities=SupabaseClient.communities()}},modifier=Modifier.padding(top=8.dp)){Text("Join community")}}}}
 }
 "Saved"->LazyColumn(Modifier.fillMaxSize().padding(16.dp)){item{Text("Saved listings",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)};items(listings.filter{saved.contains(it.id)}){ListingCard(it){selected=it}};if(saved.isEmpty())item{Text("Your saved discoveries will appear here after you sign in.")}}
 "Notifications"->LazyColumn(Modifier.fillMaxSize().padding(16.dp)){item{Text("Notifications",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)};items(notices){n->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(14.dp)){Text(n.title,fontWeight=FontWeight.Bold);Text(n.body)}}}}
 "Inbox"->Column(Modifier.fillMaxSize().padding(16.dp)){
 Text("Messages",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
 if(SupabaseClient.token().isBlank()) Text("Sign in to access messages.")
 else if(chatId.isBlank()){Button(onClick={runTask{inbox=SupabaseClient.inbox()}}){Text("Load conversations")};LazyColumn{items(inbox){t->val id=t.optString("id");Text("Conversation",Modifier.fillMaxWidth().clickable{chatId=id;runTask{chatRows=SupabaseClient.messages(id)} }.padding(16.dp))}}}
 else {TextButton(onClick={chatId=""}){Text("← Inbox")};LazyColumn(Modifier.weight(1f)){items(chatRows){m->Text(m.optString("body"),Modifier.fillMaxWidth().padding(8.dp))}};OutlinedTextField(chatText,{chatText=it},Modifier.fillMaxWidth(),label={Text("Message")});Button(onClick={runTask{SupabaseClient.sendMessage(chatId,chatText);chatText="";chatRows=SupabaseClient.messages(chatId)}},enabled=chatText.isNotBlank()){Text("Send")}}
 }
 "Account"->LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
 Text("Your account",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
 if(userEmail.isBlank()){
 Text("Sign in or create your BaltyGram account.")
 OutlinedTextField(fullName,{fullName=it},Modifier.fillMaxWidth(),label={Text("Display name (for sign up)")})
 OutlinedTextField(email,{email=it},Modifier.fillMaxWidth(),label={Text("Email")})
 OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text("Password")})
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={runTask{SupabaseClient.signIn(email,password);userEmail=SupabaseClient.email();saved=SupabaseClient.favorites();communities=SupabaseClient.communities()}},enabled=email.isNotBlank()&&password.isNotBlank()){Text("Sign in")};OutlinedButton(onClick={runTask{SupabaseClient.signUp(fullName,email,password);message="Account created. Check your email to confirm, then sign in."}},enabled=fullName.isNotBlank()&&email.isNotBlank()&&password.length>=8){Text("Create account")}}
 TextButton(onClick={runTask{SupabaseClient.resetPassword(email);message="Password reset email sent."}}){Text("Forgot password?")}
 }else{
 Text("Signed in as $userEmail");Button(onClick={page="Creator"}){Text("Creator dashboard")};Button(onClick={runTask{SupabaseClient.signOut();userEmail="";saved=emptySet();communities=emptyList()}}){Text("Sign out")}
 }
 }
 "Creator"->Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text("Creator Studio",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
 Text("Manage your developer presence and submit new listings through BaltyGram's review workflow.")
 Button(onClick={ {openUrl("https://baltygram.site/#submit")} }){Text("Open upload wizard")}
 Button(onClick={ {openUrl("https://baltygram.site/#uploads")} }){Text("Manage my uploads")}
 Text("Uploads are reviewed by the BaltyGram moderation workflow. The app does not bypass approval or expose private package storage.")
 }
 }
 }
 }}
}

@Composable fun ListingCard(item:Listing,onClick:()->Unit){Card(Modifier.fillMaxWidth().clickable{onClick()}){Column(Modifier.padding(16.dp)){Text(item.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text(item.type,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(4.dp));Text(item.description,maxLines=3,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable fun ListingDetail(item:Listing,saved:Boolean,onBack:()->Unit,onSave:()->Unit,onDownload:(Listing)->Unit,openUrl:(String)->Unit){
 Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
 TextButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null);Text("Back")}
 Text(item.name,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text(item.type,color=MaterialTheme.colorScheme.primary);Text(item.description)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=onSave){Icon(if(saved)Icons.Default.Bookmark else Icons.Default.BookmarkBorder,null);Text(if(saved)"Saved" else "Save")};Button(onClick={onDownload(item)}){Icon(Icons.Default.Download,null);Text("Download")}}
 if(item.externalUrl.isNotBlank()) TextButton(onClick={ {openUrl(item.externalUrl)} }){Text("Visit publisher website")}
 }
}
