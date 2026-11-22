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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

data class Listing(val id:String,val name:String,val description:String,val type:String,val icon:String="",val versionId:String="",val externalUrl:String="")
data class Community(val id:String,val name:String,val description:String)
data class Notice(val id:String,val title:String,val body:String)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  SupabaseClient.init(applicationContext)
  setContent { BaltyGramApp { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaltyGramApp(openUrl:(String)->Unit){
 var page by remember { mutableStateOf("Home") }
 var dark by remember { mutableStateOf(false) }
 var listings by remember { mutableStateOf(listOf<Listing>()) }
 var communities by remember { mutableStateOf(listOf<Community>()) }
 var notices by remember { mutableStateOf(listOf<Notice>()) }
 var saved by remember { mutableStateOf(setOf<String>()) }
 var selected by remember { mutableStateOf<Listing?>(null) }
 var query by remember { mutableStateOf("") }
 var filter by remember { mutableStateOf("All") }
 var email by remember { mutableStateOf(SupabaseClient.email()) }
 var loginEmail by remember { mutableStateOf("") }
 var password by remember { mutableStateOf("") }
 var displayName by remember { mutableStateOf("") }
 var notice by remember { mutableStateOf("") }
 var busy by remember { mutableStateOf(false) }
 var inbox by remember { mutableStateOf(listOf<JSONObject>()) }
 var chatRows by remember { mutableStateOf(listOf<JSONObject>()) }
 var chatId by remember { mutableStateOf("") }
 var chatText by remember { mutableStateOf("") }
 val scope= rememberCoroutineScope()

 fun task(block:suspend()->Unit){
  scope.launch {
   busy=true; notice=""
   try { block() } catch(e:Exception) { notice=e.message ?: "Something went wrong." }
   finally { busy=false }
  }
 }
 fun load(){
  task {
   listings=SupabaseClient.listings()
   if(SupabaseClient.token().isNotBlank()){
    saved=SupabaseClient.favorites()
    communities=SupabaseClient.communities()
    notices=SupabaseClient.notifications()
   }
  }
 }
 LaunchedEffect(Unit){ load() }

 MaterialTheme(
  colorScheme=if(dark) darkColorScheme(primary=androidx.compose.ui.graphics.Color(0xFF10B981),secondary=androidx.compose.ui.graphics.Color(0xFF6EE7B7))
  else lightColorScheme(primary=androidx.compose.ui.graphics.Color(0xFF059669),secondary=androidx.compose.ui.graphics.Color(0xFF10B981))
 ){
  Scaffold(
   topBar={
    TopAppBar(
     title={Text("BaltyGram",fontWeight=FontWeight.Bold)},
     actions={
      IconButton(onClick={dark=!dark}){Icon(if(dark) Icons.Default.LightMode else Icons.Default.DarkMode,"Theme")}
      IconButton(onClick={page="Notifications";task{notices=SupabaseClient.notifications()}}){
       BadgedBox(badge={if(notices.isNotEmpty()) Badge{Text(notices.size.toString())}}){Icon(Icons.Default.Notifications,"Notifications")}
      }
      IconButton(onClick={::load}){Icon(Icons.Default.Refresh,"Refresh")}
     }
    )
   },
   bottomBar={
    NavigationBar {
     listOf("Home","Explore","Community","Inbox","Saved","Account").forEach { p ->
      NavigationBarItem(
       selected=page==p,
       onClick={page=p;selected=null},
       icon={Icon(when(p){"Home"->Icons.Default.Home;"Explore"->Icons.Default.Explore;"Community"->Icons.Default.Groups;"Inbox"->Icons.Default.Chat;"Saved"->Icons.Default.Bookmark;else->Icons.Default.Person},p)},
       label={Text(p)}
      )
     }
    }
   }
  ){ padding ->
   Column(Modifier.fillMaxSize().padding(padding)){
    if(notice.isNotBlank()) Text(notice,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(12.dp))
    if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if(selected!=null){
     ListingDetail(
      item=selected!!,
      saved=saved.contains(selected!!.id),
      onBack={selected=null},
      onSave={
       task {
        if(SupabaseClient.token().isBlank()) error("Sign in to save listings.")
        SupabaseClient.toggleFavorite(selected!!.id,saved.contains(selected!!.id))
        saved=SupabaseClient.favorites()
       }
      },
      onDownload={item->task{openUrl(SupabaseClient.download(item))}},
      openUrl=openUrl
     )
    } else {
     when(page){
      "Home" -> HomeScreen(listings,onOpen={selected=it},onExplore={page="Explore"})
      "Explore" -> ExploreScreen(listings,query,{query=it},filter,{filter=it},onOpen={selected=it})
      "Community" -> CommunityScreen(communities,loggedIn=SupabaseClient.token().isNotBlank(),onJoin={id->task{SupabaseClient.joinCommunity(id);communities=SupabaseClient.communities()}})
      "Saved" -> SavedScreen(listings,saved,onOpen={selected=it})
      "Notifications" -> NotificationScreen(notices)
      "Inbox" -> InboxScreen(
       loggedIn=SupabaseClient.token().isNotBlank(),inbox=inbox,chatId=chatId,chatRows=chatRows,chatText=chatText,
       onLoad={task{inbox=SupabaseClient.inbox()}},
       onOpen={id->chatId=id;task{chatRows=SupabaseClient.messages(id)}},
       onBack={chatId=""},
       onText={chatText=it},
       onSend={task{SupabaseClient.sendMessage(chatId,chatText);chatText="";chatRows=SupabaseClient.messages(chatId)}}
      )
      "Account" -> AccountScreen(
       email=email,loginEmail=loginEmail,password=password,displayName=displayName,
       onLoginEmail={loginEmail=it},onPassword={password=it},onDisplayName={displayName=it},
       onSignIn={task{SupabaseClient.signIn(loginEmail,password);email=SupabaseClient.email();saved=SupabaseClient.favorites();communities=SupabaseClient.communities()}},
       onSignUp={task{SupabaseClient.signUp(displayName,loginEmail,password);notice="Account created. Check your email to confirm, then sign in."}},
       onReset={task{SupabaseClient.resetPassword(loginEmail);notice="Password reset email sent."}},
       onSignOut={task{SupabaseClient.signOut();email="";saved=emptySet();communities=emptyList()}},
       onCreator={page="Creator"}
      )
      "Creator" -> CreatorScreen(openUrl)
      else -> HomeScreen(listings,onOpen={selected=it},onExplore={page="Explore"})
     }
    }
   }
  }
 }
}

@Composable
fun HomeScreen(listings:List<Listing>,onOpen:(Listing)->Unit,onExplore:()->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{
   Text("Discover digital tools.",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
   Text("Games, apps, software and resources from developers.",color=MaterialTheme.colorScheme.onSurfaceVariant)
   Button(onClick=onExplore){Text("Explore catalogue")}
   Text("Featured discoveries",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
  }
  if(listings.isEmpty()) item{Text("No approved listings yet. Refresh when your catalogue is populated.")}
  items(listings.take(30)){ListingCard(it){onOpen(it)}}
 }
}

@Composable
fun ExploreScreen(listings:List<Listing>,query:String,onQuery:(String)->Unit,filter:String,onFilter:(String)->Unit,onOpen:(Listing)->Unit){
 Column(Modifier.fillMaxSize().padding(16.dp)){
  OutlinedTextField(query,onQuery,Modifier.fillMaxWidth(),label={Text("Search BaltyGram")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
  Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
   listOf("All","game","software","app","ai","editing","developer").forEach{f->FilterChip(selected=filter.equals(f,true),onClick={onFilter(f)},label={Text(f.replaceFirstChar{it.uppercase()})})}
  }
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
   items(listings.filter{(filter=="All"||it.type.contains(filter,true))&&(it.name.contains(query,true)||it.description.contains(query,true))}){ListingCard(it){onOpen(it)}}
  }
 }
}

@Composable
fun ListingCard(item:Listing,onClick:()->Unit){
 Card(Modifier.fillMaxWidth().clickable{onClick()}){
  Column(Modifier.padding(16.dp)){
   Text(item.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
   Text(item.type,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
   Text(item.description,maxLines=3,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

@Composable
fun ListingDetail(item:Listing,saved:Boolean,onBack:()->Unit,onSave:()->Unit,onDownload:(Listing)->Unit,openUrl:(String)->Unit){
 Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  TextButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null);Text("Back")}
  Text(item.name,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
  Text(item.type,color=MaterialTheme.colorScheme.primary)
  Text(item.description)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedButton(onClick=onSave){Icon(if(saved)Icons.Default.Bookmark else Icons.Default.BookmarkBorder,null);Text(if(saved)"Saved" else "Save")}
   Button(onClick={onDownload(item)}){Icon(Icons.Default.Download,null);Text("Download")}
  }
  if(item.externalUrl.isNotBlank()) { TextButton(onClick={openUrl(item.externalUrl)}) { Text("Publisher website") } }
 }
}

@Composable
fun CommunityScreen(items:List<Community>,loggedIn:Boolean,onJoin:(String)->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Text("Communities",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text(if(loggedIn)"Find your people and join the conversation." else "Sign in to join communities.")}
  items(items){c->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(c.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(c.description);Button(onClick={onJoin(c.id)},enabled=loggedIn){Text("Join community")}}}}
 }
}

@Composable
fun SavedScreen(listings:List<Listing>,saved:Set<String>,onOpen:(Listing)->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(16.dp)){
  item{Text("Saved listings",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}
  items(listings.filter{saved.contains(it.id)}){ListingCard(it){onOpen(it)}}
  if(saved.isEmpty())item{Text("Your saved discoveries will appear here after you sign in.")}
 }
}

@Composable
fun NotificationScreen(items:List<Notice>){
 LazyColumn(Modifier.fillMaxSize().padding(16.dp)){
  item{Text("Notifications",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}
  items(items){n->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(14.dp)){Text(n.title,fontWeight=FontWeight.Bold);Text(n.body)}}}
 }
}

@Composable
fun InboxScreen(loggedIn:Boolean,inbox:List<JSONObject>,chatId:String,chatRows:List<JSONObject>,chatText:String,onLoad:()->Unit,onOpen:(String)->Unit,onBack:()->Unit,onText:(String)->Unit,onSend:()->Unit){
 Column(Modifier.fillMaxSize().padding(16.dp)){
  Text("Messages",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
  if(!loggedIn) Text("Sign in to access messages.")
  else if(chatId.isBlank()){
   Button(onClick=onLoad){Text("Load conversations")}
   LazyColumn{items(inbox){t->Text("Conversation",Modifier.fillMaxWidth().clickable{onOpen(t.optString("id"))}.padding(16.dp))}}
  }else{
   TextButton(onClick=onBack){Text("← Inbox")}
   LazyColumn(Modifier.weight(1f)){items(chatRows){m->Text(m.optString("body"),Modifier.fillMaxWidth().padding(8.dp))}}
   OutlinedTextField(chatText,onText,Modifier.fillMaxWidth(),label={Text("Message")})
   Button(onClick=onSend,enabled=chatText.isNotBlank()){Text("Send")}
  }
 }
}

@Composable
fun AccountScreen(email:String,loginEmail:String,password:String,displayName:String,onLoginEmail:(String)->Unit,onPassword:(String)->Unit,onDisplayName:(String)->Unit,onSignIn:()->Unit,onSignUp:()->Unit,onReset:()->Unit,onSignOut:()->Unit,onCreator:()->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Text("Your account",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}
  if(email.isBlank()){
   item{Text("Sign in or create your BaltyGram account.")}
   item{OutlinedTextField(displayName,onDisplayName,Modifier.fillMaxWidth(),label={Text("Display name")})}
   item{OutlinedTextField(loginEmail,onLoginEmail,Modifier.fillMaxWidth(),label={Text("Email")})}
   item{OutlinedTextField(password,onPassword,Modifier.fillMaxWidth(),label={Text("Password")})}
   item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onSignIn,enabled=loginEmail.isNotBlank()&&password.isNotBlank()){Text("Sign in")};OutlinedButton(onClick=onSignUp,enabled=displayName.isNotBlank()&&loginEmail.isNotBlank()&&password.length>=8){Text("Sign up")}}}
   item{TextButton(onClick=onReset){Text("Forgot password?")}}
  }else{
   item{Text("Signed in as $email")}
   item{Button(onClick=onCreator){Text("Creator dashboard")}}
   item{Button(onClick=onSignOut){Text("Sign out")}}
  }
 }
}

@Composable
fun CreatorScreen(openUrl:(String)->Unit){
 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Creator Studio",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
  Text("Manage your developer presence and submit new listings through BaltyGram's review workflow.")
  Button(onClick={openUrl("https://baltygram.site/#submit")}){Text("Open upload wizard")}
  Button(onClick={openUrl("https://baltygram.site/#uploads")}){Text("Manage my uploads")}
  Text("Uploads follow the website's moderation and approval workflow. The app does not bypass approval or expose private package storage.")
 }
}
