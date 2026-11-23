package site.baltygram.app

import android.content.Intent
import android.net.Uri
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Listing(val id:String,val name:String,val description:String,val type:String,val icon:String="",val versionId:String="",val externalUrl:String="",val views:Int=0,val downloads:Int=0,val createdAt:String="",val category:String="")
data class Category(val id:String,val name:String,val type:String)
data class CreatorListing(val item:Listing,val status:String)
data class Community(val id:String,val name:String,val description:String)
data class Notice(val id:String,val title:String,val body:String)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  SupabaseClient.init(applicationContext)
  setContent { BaltyGramApp(openUrl={ url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }, enqueueDownload={ item ->
    val url=SupabaseClient.download(item)
    val request=DownloadManager.Request(Uri.parse(url)).setTitle(item.name).setDescription("Downloading from BaltyGram").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, item.name.replace(Regex("[^A-Za-z0-9._-]"),"_")+".download")
    (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
   }) }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaltyGramApp(openUrl:(String)->Unit,enqueueDownload:(Listing)->Unit){
 var page by remember { mutableStateOf("Home") }
 var dark by remember { mutableStateOf(false) }
 var listings by remember { mutableStateOf(listOf<Listing>()) }
  var categories by remember { mutableStateOf(listOf<Category>()) }
  var myUploads by remember { mutableStateOf(listOf<CreatorListing>()) }
  var recent by remember { mutableStateOf(listOf<Listing>()) }
 var communities by remember { mutableStateOf(listOf<Community>()) }
 var notices by remember { mutableStateOf(listOf<Notice>()) }
 var saved by remember { mutableStateOf(setOf<String>()) }
 var selected by remember { mutableStateOf<Listing?>(null) }
 var query by remember { mutableStateOf("") }
 var filter by remember { mutableStateOf("All") }
 var email by remember { mutableStateOf(SupabaseClient.email()) }
 var loginEmail by remember { mutableStateOf("") }
 var password by remember { mutableStateOf("") }
 var showPassword by remember { mutableStateOf(false) }
 var notice by remember { mutableStateOf("") }
 var busy by remember { mutableStateOf(false) }
 var inbox by remember { mutableStateOf(listOf<JSONObject>()) }
 var chatRows by remember { mutableStateOf(listOf<JSONObject>()) }
 var chatId by remember { mutableStateOf("") }
 var chatText by remember { mutableStateOf("") }
 val scope= rememberCoroutineScope()
 val drawerState = rememberDrawerState(DrawerValue.Closed)

 fun task(block:suspend()->Unit){
  scope.launch {
   busy=true; notice=""
   try { withContext(Dispatchers.IO) { block() } } catch(e:Exception) { notice=e.message?.takeIf { it.isNotBlank() } ?: "Request failed (${e.javaClass.simpleName}). Check your connection and try again." }
   finally { busy=false }
  }
 }
 fun load(){
  task {
   listings=SupabaseClient.listings()
    categories=SupabaseClient.categories()
    recent=SupabaseClient.recentListings(listings)
    if(SupabaseClient.token().isNotBlank()) myUploads=SupabaseClient.myListings()
   if(SupabaseClient.token().isNotBlank()){
    saved=SupabaseClient.favorites()
    communities=SupabaseClient.communities()
    notices=SupabaseClient.notifications()
   }
  }
 }
 LaunchedEffect(Unit){ load() }

 MaterialTheme(
  colorScheme=if(dark) darkColorScheme(primary=Color(0xFF8AB4FF),secondary=Color(0xFF5EEAD4),background=Color(0xFF0B1220),surface=Color(0xFF111B2E))
  else lightColorScheme(primary=Color(0xFF2563EB),secondary=Color(0xFF0F9D8A),background=Color(0xFFF5F7FB),surface=Color(0xFFFFFFFF),surfaceVariant=Color(0xFFEAF0F8))
 ){
  ModalNavigationDrawer(
   drawerState=drawerState,
   drawerContent={
    ModalDrawerSheet {
     Column(Modifier.fillMaxWidth().padding(20.dp)) {
      Text("BaltyGram",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
      Text("Discover. Download. Connect.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
     HorizontalDivider()
     listOf("Home","Explore","Categories","Trending","New releases","Community","Inbox","Notifications","Saved","Downloads","Creator Studio","My uploads","Developer profile","Analytics","Account","Settings","Help","About").forEach { destination ->
      NavigationDrawerItem(
       label={Text(destination)},
       selected=page==destination || (destination=="Creator Studio" && page=="Creator"),
       icon={Icon(when(destination){"Home"->Icons.Default.Home;"Explore"->Icons.Default.Explore;"Categories"->Icons.Default.Category;"Trending"->Icons.Default.TrendingUp;"New releases"->Icons.Default.NewReleases;"Community"->Icons.Default.Groups;"Inbox"->Icons.Default.Chat;"Notifications"->Icons.Default.Notifications;"Saved"->Icons.Default.Bookmark;"Downloads"->Icons.Default.Download;"Creator Studio"->Icons.Default.Upload;"My uploads"->Icons.Default.Folder;"Developer profile"->Icons.Default.Code;"Analytics"->Icons.Default.Analytics;"Account"->Icons.Default.Person;"Settings"->Icons.Default.Settings;"Help"->Icons.Default.HelpOutline;else->Icons.Default.Info},null)},
       onClick={scope.launch { drawerState.close(); page=when(destination){"Creator Studio"->"Creator";"Categories"->"Categories";"Trending"->{filter="Trending";"Explore"};"New releases"->{filter="New";"Explore"};"My uploads"->"My uploads";"Developer profile","Analytics"->"Creator";else->destination};selected=null }}
      )
     }
    }
   }
  ) {
  Scaffold(
   topBar={
    TopAppBar(
     colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background),
     title={Text("BaltyGram",fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleLarge)},
     navigationIcon={IconButton(onClick={scope.launch { drawerState.open() }}){Icon(Icons.Default.Menu,"Open navigation menu")}},
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
    NavigationBar(containerColor=MaterialTheme.colorScheme.surface, tonalElevation=6.dp) {
     listOf("Home","Explore","Community","Account").forEach { p ->
      NavigationBarItem(
       selected=page==p,
       onClick={page=p;selected=null},
       icon={Icon(when(p){"Home"->Icons.Default.Home;"Explore"->Icons.Default.Explore;"Community"->Icons.Default.Groups;else->Icons.Default.Person},p)},
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
      onDownload={item->task{enqueueDownload(item)}},
      openUrl=openUrl
     )
    } else {
     when(page){
      "Home" -> HomeScreen(listings,recent,onOpen={selected=it;SupabaseClient.recordRecent(it.id)},onExplore={page="Explore"})
      "Explore" -> ExploreScreen(listings,query,{query=it},filter,{filter=it},onOpen={selected=it;SupabaseClient.recordRecent(it.id)})
      "Categories" -> CategoriesScreen(categories,listings,onChoose={c->filter=c.name;page="Explore"})
       "My uploads" -> MyUploadsScreen(myUploads,onOpen={item->selected=item;SupabaseClient.recordRecent(item.id)})
       "Community" -> CommunityScreen(communities,loggedIn=SupabaseClient.token().isNotBlank(),onJoin={id->task{SupabaseClient.joinCommunity(id);communities=SupabaseClient.communities()}})
      "Saved" -> SavedScreen(listings,saved,onOpen={selected=it})
      "Notifications" -> NotificationScreen(notices,onMarkRead={task{SupabaseClient.markAllNotificationsRead();notices=SupabaseClient.notifications()}})
      "Inbox" -> InboxScreen(
       loggedIn=SupabaseClient.token().isNotBlank(),inbox=inbox,chatId=chatId,chatRows=chatRows,chatText=chatText,
       onLoad={task{inbox=SupabaseClient.inbox()}},
       onOpen={id->chatId=id;task{chatRows=SupabaseClient.messages(id)}},
       onBack={chatId=""},
       onText={chatText=it},
       onSend={task{SupabaseClient.sendMessage(chatId,chatText);chatText="";chatRows=SupabaseClient.messages(chatId)}}
      )
      "Account" -> AccountScreen(
       email=email,loginEmail=loginEmail,password=password,showPassword=showPassword,onShowPassword={showPassword=it},
       onLoginEmail={loginEmail=it},onPassword={password=it},
       onSignIn={task{SupabaseClient.signIn(loginEmail,password);email=SupabaseClient.email();saved=SupabaseClient.favorites();communities=SupabaseClient.communities();myUploads=SupabaseClient.myListings()}},
       onSignUp={task{SupabaseClient.signUp(loginEmail,password);notice="Account created. Check your email to confirm, then sign in."}},
       onReset={task{SupabaseClient.resetPassword(loginEmail);notice="Password reset email sent."}},
       onSignOut={task{SupabaseClient.signOut();email="";saved=emptySet();communities=emptyList()}},
       onCreator={page="Creator"}
      )
      "Creator" -> CreatorScreen(openUrl)
      "Downloads" -> InfoScreen("Downloads","Files downloaded through BaltyGram are saved in your device Downloads folder. Android shows progress and completion notifications.")
      "Settings" -> InfoScreen("Settings","Theme controls are available from the top bar. Account and privacy settings remain tied to your BaltyGram account.")
      "Help" -> InfoScreen("Help","Visit baltygram.site for support and platform information.")
      "About" -> InfoScreen("About BaltyGram","A digital discovery platform for games, apps, software and developer tools.")
      else -> HomeScreen(listings,recent,onOpen={selected=it;SupabaseClient.recordRecent(it.id)},onExplore={page="Explore"})
     }
    }
   }
  }
  }
 }
}

@Composable
fun InfoScreen(title:String,description:String){
 Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
  Text(description,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}

@Composable
fun HomeScreen(listings:List<Listing>,recent:List<Listing>,onOpen:(Listing)->Unit,onExplore:()->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{
   Text("Discover digital tools.",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
   Text("Games, apps, software and resources from developers.",color=MaterialTheme.colorScheme.onSurfaceVariant)
   Button(onClick=onExplore){Text("Explore catalogue")}
   if(recent.isNotEmpty()) Text("Continue exploring",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
  }
  if(recent.isNotEmpty()) items(recent.take(6)){ListingCard(it){onOpen(it)}}
  item{Text("Featured discoveries",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
  if(listings.isEmpty()) item{Text("No approved listings yet. Refresh when your catalogue is populated.")}
  items(listings.take(30)){ListingCard(it){onOpen(it)}}
 }
}

@Composable
fun ExploreScreen(listings:List<Listing>,query:String,onQuery:(String)->Unit,filter:String,onFilter:(String)->Unit,onOpen:(Listing)->Unit){
 Column(Modifier.fillMaxSize().padding(16.dp)){
  OutlinedTextField(query,onQuery,Modifier.fillMaxWidth(),label={Text("Search BaltyGram")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
  Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
   listOf("All","game","software","app","ai","editing","developer","Trending","New").forEach{f->FilterChip(selected=filter.equals(f,true),onClick={onFilter(f)},label={Text(f.replaceFirstChar{it.uppercase()})})}
  }
  val filtered=listings.filter{(filter=="All"||filter=="Trending"||filter=="New"||it.type.contains(filter,true)||it.category.contains(filter,true))&&(it.name.contains(query,true)||it.description.contains(query,true))}
  val ordered=when(filter){"Trending"->filtered.sortedByDescending{it.downloads};"New"->filtered.sortedByDescending{it.createdAt};else->filtered}
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
   items(ordered){ListingCard(it){onOpen(it)}}
   if(ordered.isEmpty()) item{Text("No matching listings. Try another category or search term.")}
  }
 }
}

@Composable
@Composable
fun CategoriesScreen(categories:List<Category>,listings:List<Listing>,onChoose:(Category)->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Text("Categories",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Browse BaltyGram by category.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
  items(categories){c->val count=listings.count{it.category.equals(c.name,true)};Card(Modifier.fillMaxWidth().clickable{onChoose(c)}){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(c.name,fontWeight=FontWeight.Bold);Text(c.type,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("$count listings",color=MaterialTheme.colorScheme.primary)}}}
  if(categories.isEmpty()) item{Text("Categories will appear when the catalogue is available.")}
 }
}

@Composable
fun MyUploadsScreen(items:List<CreatorListing>,onOpen:(Listing)->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Text("My uploads",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Manage the listings published from your developer account.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
  if(items.isEmpty()) item{Text("No uploads found. Sign in with your creator account or publish your first listing.")}
  items(items){row->Column(verticalArrangement=Arrangement.spacedBy(4.dp)){ListingCard(row.item){onOpen(row.item)};Text("Status: ${row.status.replaceFirstChar{it.uppercase()}} · ${row.item.downloads} downloads · ${row.item.views} views",Modifier.padding(start=8.dp),color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelMedium)}}
 }
}

@Composable
fun ListingCard(item:Listing,onClick:()->Unit){
 Card(Modifier.fillMaxWidth().clickable{onClick()}){
  Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   if(item.icon.isNotBlank()) coil.compose.AsyncImage(model=item.icon,contentDescription=item.name,modifier=Modifier.size(68.dp))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
    Text(item.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    Text(item.type,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
    Text(item.description,maxLines=3,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
 }
}

@Composable
fun ListingDetail(item:Listing,saved:Boolean,onBack:()->Unit,onSave:()->Unit,onDownload:(Listing)->Unit,openUrl:(String)->Unit){
 Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  TextButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null);Text("Back")}
  Text(item.name,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
  Text(item.type,color=MaterialTheme.colorScheme.primary)
  if(item.icon.isNotBlank()) coil.compose.AsyncImage(model=item.icon,contentDescription=item.name,modifier=Modifier.fillMaxWidth().height(180.dp))
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
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=18.dp)){
  item{
   Text("Saved",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold)
   Text("Your bookmarked discoveries.",color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  val savedItems=listings.filter{saved.contains(it.id)}
  if(savedItems.isEmpty()) item{
   Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
    Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
     Icon(Icons.Default.BookmarkBorder,null,tint=MaterialTheme.colorScheme.primary)
     Text("Nothing saved yet",fontWeight=FontWeight.Bold)
     Text("Save listings to keep them close at hand.",color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
  items(savedItems){ListingCard(it){onOpen(it)}}
 }
}

@Composable
fun NotificationScreen(items:List<Notice>,onMarkRead:()->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    Text("Notifications",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
    TextButton(onClick=onMarkRead){Text("Mark all read")}
   }
  }
  if(items.isEmpty()) item{Text("You're all caught up.")}
  items(items){n->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(n.title,fontWeight=FontWeight.Bold);Text(n.body)}}}
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
fun AccountScreen(email:String,loginEmail:String,password:String,showPassword:Boolean,onShowPassword:(Boolean)->Unit,onLoginEmail:(String)->Unit,onPassword:(String)->Unit,onSignIn:()->Unit,onSignUp:()->Unit,onReset:()->Unit,onSignOut:()->Unit,onCreator:()->Unit){
 LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Text("Your account",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}
  if(email.isBlank()){
   item{Text("Sign in or create your BaltyGram account.")}
   item{OutlinedTextField(loginEmail,onLoginEmail,Modifier.fillMaxWidth(),label={Text("Email")})}
   item{OutlinedTextField(password,onPassword,Modifier.fillMaxWidth(),label={Text("Password")},visualTransformation=if(showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),trailingIcon={TextButton(onClick={onShowPassword(!showPassword)}){Text(if(showPassword)"Hide" else "Show")}})}
   item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onSignIn,enabled=loginEmail.isNotBlank()&&password.isNotBlank()){Text("Sign in")};OutlinedButton(onClick=onSignUp,enabled=loginEmail.contains("@")&&password.length>=8){Text("Sign up")}}}
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
