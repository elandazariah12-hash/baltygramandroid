package site.baltygram.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONObject

private val Mint=Color(0xFF10B981)
data class Listing(val id:String,val name:String,val short:String,val description:String,val type:String,val icon:String,val cover:String,val website:String,val publisher:String,val versionId:String,val version:String,val external:String,val featured:Boolean,val tags:List<String>,val shots:List<String>)
data class Community(val id:String,val name:String,val description:String,val icon:String)
data class Notice(val id:String,val title:String,val body:String,val read:Boolean)
data class Conversation(val id:String,val other:String,val name:String,val avatar:String,val last:String)
data class DownloadRow(val id:String,val name:String,val version:String,val status:String)

class MainActivity:ComponentActivity(){
 private var authRevision by mutableIntStateOf(0)
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);SupabaseClient.init(applicationContext);handle(intent);setContent{BaltyGramApp(authRevision){url->startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}}}
 override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);handle(intent)}
 private fun handle(i:Intent?){val u=i?.data?:return;if(u.scheme=="site.baltygram.app"&&u.host=="auth"){val code=u.getQueryParameter("code");lifecycleScope.launch{try{if(!code.isNullOrBlank())SupabaseClient.exchangeOAuthCode(code)}catch(_:Exception){};authRevision++}}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BaltyGramApp(authRevision:Int,openUrl:(String)->Unit){
 var page by remember{mutableStateOf("Home")};var dark by remember{mutableStateOf(false)}
 var listings by remember{mutableStateOf(emptyList<Listing>())};var communities by remember{mutableStateOf(emptyList<Community>())};var notices by remember{mutableStateOf(emptyList<Notice>())}
 var saved by remember{mutableStateOf(emptySet<String>())};var downloads by remember{mutableStateOf(emptyList<DownloadRow>())};var inbox by remember{mutableStateOf(emptyList<Conversation>())};var messages by remember{mutableStateOf(emptyList<JSONObject>())}
 var selected by remember{mutableStateOf<Listing?>(null)};var chatId by remember{mutableStateOf("")};var chatName by remember{mutableStateOf("")};var chatText by remember{mutableStateOf("")}
 var query by remember{mutableStateOf("")};var filter by remember{mutableStateOf("All")};var email by remember{mutableStateOf(SupabaseClient.email())}
 var loginEmail by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var displayName by remember{mutableStateOf("")}
 var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var info by remember{mutableStateOf("")}
 val scope=rememberCoroutineScope();val drawer=rememberDrawerState(DrawerValue.Closed)
 fun task(block:suspend()->Unit){scope.launch{busy=true;error="";info="";try{block()}catch(e:Exception){error=e.message?:"Something went wrong."}finally{busy=false}}}
 fun reload(){task{listings=SupabaseClient.listings();if(SupabaseClient.token().isNotBlank()){email=SupabaseClient.email();saved=SupabaseClient.favorites();communities=SupabaseClient.communities();notices=SupabaseClient.notifications()}}}
 LaunchedEffect(Unit){reload()}
 LaunchedEffect(authRevision){if(authRevision>0)reload()}
 MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=Mint,secondary=Color(0xFF6EE7B7))else lightColorScheme(primary=Color(0xFF059669),secondary=Mint)){
  ModalNavigationDrawer(drawerState=drawer,drawerContent={ModalDrawerSheet(Modifier.widthIn(max=320.dp)){
   Column(Modifier.padding(20.dp)){Text("BaltyGram",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.primary);Text(if(email.isBlank())"Discover. Connect. Share." else email,style=MaterialTheme.typography.bodySmall)}
   HorizontalDivider()
   DrawerGroup("DISCOVER")
   DrawerLink("Home",Icons.Default.Home,page=="Home"){page="Home";selected=null}
   DrawerLink("Explore",Icons.Default.Explore,page=="Explore"){page="Explore";selected=null}
   DrawerLink("Categories",Icons.Default.Category,page=="Categories"){page="Categories"}
   DrawerLink("Find people",Icons.Default.People,page=="People"){page="People"}
   DrawerGroup("YOUR SPACE")
   DrawerLink("My uploads",Icons.Default.CloudUpload,page=="Uploads"){page="Uploads"}
   DrawerLink("Downloads",Icons.Default.Download,page=="Downloads"){page="Downloads";task{downloads=SupabaseClient.downloadHistory()}}
   DrawerLink("Saved",Icons.Default.Bookmarks,page=="Saved"){page="Saved"}
   DrawerLink("Creator Studio",Icons.Default.Dashboard,page=="Creator"){page="Creator"}
   DrawerGroup("COMMUNITY")
   DrawerLink("Communities",Icons.Default.Groups,page=="Community"){page="Community";task{communities=SupabaseClient.communities()}}
   DrawerLink("Messages",Icons.Default.Chat,page=="Messages"){page="Messages";task{inbox=SupabaseClient.inbox()}}
   DrawerLink("Notifications",Icons.Default.Notifications,page=="Notifications"){page="Notifications";task{notices=SupabaseClient.notifications()}}
   DrawerLink("Organizations",Icons.Default.Business,page=="Organizations"){page="Organizations"}
   DrawerGroup("MORE")
   DrawerLink("Account",Icons.Default.Person,page=="Account"){page="Account"}
   DrawerLink("About BaltyGram",Icons.Default.Info,page=="About"){page="About"}
   DrawerLink("Contact & help",Icons.Default.Help,page=="Contact"){page="Contact"}
   DrawerLink(if(dark)"Light appearance" else "Dark appearance",if(dark)Icons.Default.LightMode else Icons.Default.DarkMode,false){dark=!dark}
  }}){
   Scaffold(topBar={TopAppBar(title={Column{Text("BaltyGram",fontWeight=FontWeight.ExtraBold);Text("Discover what's next",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}},navigationIcon={IconButton(onClick={scope.launch{drawer.open()}}){Icon(Icons.Default.Menu,"Menu")}},actions={IconButton(onClick={page="Notifications";task{notices=SupabaseClient.notifications()}}){BadgedBox(badge={if(notices.any{!it.read})Badge{Text(notices.count{!it.read}.toString())}}){Icon(Icons.Default.NotificationsNone,"Notifications")}};IconButton(onClick={::reload}){Icon(Icons.Default.Refresh,"Refresh")}})},
    bottomBar={NavigationBar{listOf("Home","Explore","Upload","Account").forEach{p->NavigationBarItem(selected=page==p,onClick={page=p;selected=null;if(p=="Upload")openUrl("https://baltygram.site/#submit")},icon={Icon(when(p){"Home"->Icons.Default.Home;"Explore"->Icons.Default.Explore;"Upload"->Icons.Default.AddCircle;else->Icons.Default.Person},null)},label={Text(p)})}}}
   ){pad->Column(Modifier.fillMaxSize().padding(pad)){
    if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    if(error.isNotBlank())Text(error,Modifier.fillMaxWidth().padding(12.dp),color=MaterialTheme.colorScheme.error)
    if(info.isNotBlank())Text(info,Modifier.padding(12.dp),color=MaterialTheme.colorScheme.primary)
    if(selected!=null)Detail(selected!!,selected!!.id in saved,onBack={selected=null},onSave={task{if(email.isBlank())error("Sign in to save listings.")else{SupabaseClient.toggleFavorite(selected!!.id,selected!!.id in saved);saved=SupabaseClient.favorites()}}},onDownload={task{if(email.isBlank())throw Exception("Sign in to download.");openUrl(SupabaseClient.download(selected!!))}},openUrl)
    else when(page){
     "Home"->HomeScreen(listings,{page="Explore"},{selected=it})
     "Explore"->ExploreScreen(listings,query,{query=it},filter,{filter=it}){selected=it}
     "Categories"->CategoriesScreen(listings){filter=it;page="Explore"}
     "Saved"->ListScreen("Saved discoveries",listings.filter{it.id in saved},"Your saved listings appear here."){selected=it}
     "Account"->AccountScreen(email,loginEmail,password,displayName,{loginEmail=it},{password=it},{displayName=it},
       {task{SupabaseClient.signIn(loginEmail.trim(),password);email=SupabaseClient.email();saved=SupabaseClient.favorites();info="Welcome back."}},
       {task{SupabaseClient.signUp(displayName.trim(),loginEmail.trim(),password);info="Account created. Check your email if confirmation is enabled."}},
       {task{SupabaseClient.resetPassword(loginEmail.trim());info="Password reset email sent."}},
       {task{openUrl(SupabaseClient.googleAuthUrl())}},
       {task{SupabaseClient.signOut();email="";saved=emptySet();info="Signed out."}},
       {page="Creator"})
     "Community"->CommunityScreen(communities,email.isNotBlank()){id->task{SupabaseClient.joinCommunity(id);communities=SupabaseClient.communities()}}
     "Messages"->MessagesScreen(inbox,chatId,chatName,messages,chatText,{chatText=it},
       {c->chatId=c.id;chatName=c.name;task{messages=SupabaseClient.messages(c.id)}},
       {chatId="";task{inbox=SupabaseClient.inbox()}},
       {task{SupabaseClient.sendMessage(chatId,chatText);chatText="";messages=SupabaseClient.messages(chatId)}})
     "Notifications"->NotificationsScreen(notices){id->task{SupabaseClient.markNotificationRead(id);notices=SupabaseClient.notifications()}}
     "Downloads"->DownloadsScreen(downloads){task{downloads=SupabaseClient.downloadHistory()}}
     "Uploads"->ExternalScreen("My uploads","Manage submissions and review status using BaltyGram's existing creator workflow.","Open uploads"){openUrl("https://baltygram.site/#uploads")}
     "Creator"->ExternalScreen("Creator Studio","The complete upload wizard uses BaltyGram's existing validation, media processing and review workflow.","Open Creator Studio"){openUrl("https://baltygram.site/#submit")}
     "People"->ExternalScreen("Find people","Discover creators and developers on BaltyGram.","Find creators"){openUrl("https://baltygram.site/#discover?tab=people")}
     "Organizations"->ExternalScreen("Organizations","Explore developer organizations and teams.","Open organizations"){openUrl("https://baltygram.site/#organizations")}
     "About"->ExternalScreen("About BaltyGram","A digital discovery platform for games, apps, software, AI tools and developer resources.","Read more"){openUrl("https://baltygram.site/#about")}
     "Contact"->ExternalScreen("Contact & help","Get in touch with the BaltyGram team.","Contact BaltyGram"){openUrl("https://baltygram.site/#contact")}
     else->HomeScreen(listings,{page="Explore"},{selected=it})
    }
   }}
  }
 }
}

@Composable private fun DrawerGroup(t:String){Text(t,Modifier.padding(start=20.dp,top=14.dp,bottom=4.dp),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
@Composable private fun DrawerLink(t:String,i:androidx.compose.ui.graphics.vector.ImageVector,s:Boolean,click:()->Unit){NavigationDrawerItem(label={Text(t)},icon={Icon(i,null)},selected=s,onClick=click,modifier=Modifier.padding(horizontal=10.dp),shape=RoundedCornerShape(12.dp))}
@Composable private fun HomeScreen(data:List<Listing>,explore:()->Unit,open:(Listing)->Unit){LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Discover your next favourite.",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold);Text("Games, apps, software, AI tools and developer resources.");Button(onClick=explore){Text("Explore BaltyGram");Icon(Icons.Default.ArrowForward,null)}}}};item{Title("Featured discoveries","Fresh picks from independent creators")};if(data.isEmpty())item{Empty("No listings loaded","Check your connection and refresh.")};items(data){ListingCard(it,open)}}}
@Composable private fun Title(a:String,b:String){Column{Text(a,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(b,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable private fun ExploreScreen(data:List<Listing>,q:String,setQ:(String)->Unit,f:String,setF:(String)->Unit,open:(Listing)->Unit){Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){OutlinedTextField(q,setQ,Modifier.fillMaxWidth().padding(top=10.dp),placeholder={Text("Search games, apps, AI tools…")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,shape=RoundedCornerShape(14.dp));Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.padding(vertical=8.dp)){listOf("All","game","software","app","ai_tool","editing_tool","developer_tool").forEach{FilterChip(selected=f==it,onClick={setF(it)},label={Text(it.replace("_"," ").replaceFirstChar{c->c.uppercase()})})}};val filtered=data.filter{(f=="All"||it.type==f)&&(q.isBlank()||it.name.contains(q,true)||it.short.contains(q,true)||it.tags.any{t->t.contains(q,true)})};if(filtered.isEmpty())Empty("No matches","Try another search or category.")else LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){items(filtered){ListingCard(it,open)}}}}
@Composable private fun CategoriesScreen(data:List<Listing>,select:(String)->Unit){val cats=listOf("game" to "Games","software" to "Software","app" to "Apps","ai_tool" to "AI tools","editing_tool" to "Editing tools","developer_tool" to "Developer tools");LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){item{Title("Categories","Browse by what you need")};items(cats){(k,n)->Card(Modifier.fillMaxWidth().clickable{select(k)}){ListItem(headlineContent={Text(n,fontWeight=FontWeight.Bold)},supportingContent={Text("${data.count{it.type==k}} listings")},leadingContent={Icon(Icons.Default.GridView,null)})}}}}
@Composable private fun ListingCard(x:Listing,open:(Listing)->Unit){Card(Modifier.fillMaxWidth().clickable{open(x)},shape=RoundedCornerShape(17.dp)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){AsyncImage(model=x.icon.ifBlank{x.cover},contentDescription=null,modifier=Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(13.dp)));Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){if(x.featured)Text("FEATURED",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text(x.name,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(x.type.replace("_"," ").replaceFirstChar{it.uppercase()},style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(x.short,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Default.ChevronRight,null)}}}
@Composable private fun Detail(x:Listing,saved:Boolean,onBack:()->Unit,onSave:()->Unit,onDownload:()->Unit,openUrl:(String)->Unit){LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){item{TextButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null);Text("Back")}};item{AsyncImage(x.cover.ifBlank{x.icon},null,Modifier.fillMaxWidth().height(190.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(18.dp)))};item{Text(x.name,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold)};item{Text(x.publisher.ifBlank{x.type},color=MaterialTheme.colorScheme.primary)};item{Text(x.short,style=MaterialTheme.typography.titleMedium)};item{Text(x.description.ifBlank{x.short})};if(x.tags.isNotEmpty())item{Text(x.tags.joinToString(" • "),style=MaterialTheme.typography.bodySmall)};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=onSave){Icon(Icons.Default.BookmarkBorder,null);Text(if(saved)"Saved" else "Save")};Button(onClick=onDownload){Icon(Icons.Default.Download,null);Text("Download")}}};if(x.website.isNotBlank())item{TextButton(onClick={openUrl(x.website)}){Text("Publisher website")}};if(x.shots.isNotEmpty())item{Text("Screenshots",fontWeight=FontWeight.Bold)};items(x.shots){AsyncImage(it,null,Modifier.fillMaxWidth().height(190.dp))}}}
@Composable private fun AccountScreen(email:String,mail:String,pw:String,name:String,setMail:(String)->Unit,setPw:(String)->Unit,setName:(String)->Unit,login:()->Unit,signup:()->Unit,reset:()->Unit,google:()->Unit,logout:()->Unit,creator:()->Unit){LazyColumn(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){item{Text(if(email.isBlank())"Welcome to BaltyGram" else "Your account",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold)};if(email.isBlank()){item{Text("Sign in to save discoveries, join communities and connect with creators.")};item{OutlinedButton(google,Modifier.fillMaxWidth()){Icon(Icons.Default.AccountCircle,null);Spacer(Modifier.width(8.dp));Text("Continue with Google")}};item{OutlinedTextField(mail,setMail,Modifier.fillMaxWidth(),label={Text("Email address")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email))};item{OutlinedTextField(pw,setPw,Modifier.fillMaxWidth(),label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation())};item{Button(login,Modifier.fillMaxWidth(),enabled=mail.isNotBlank()&&pw.isNotBlank()){Text("Sign in")}};item{OutlinedTextField(name,setName,Modifier.fillMaxWidth(),label={Text("Display name for new accounts")},singleLine=true)};item{OutlinedButton(signup,Modifier.fillMaxWidth(),enabled=name.isNotBlank()&&mail.isNotBlank()&&pw.length>=8){Text("Create account")}};item{TextButton(onClick=reset){Text("Forgot password?")}}}else{item{Text("Signed in as $email")};item{Button(creator,Modifier.fillMaxWidth()){Text("Creator Studio")}};item{OutlinedButton(logout,Modifier.fillMaxWidth()){Text("Sign out")}}}}}
@Composable private fun CommunityScreen(data:List<Community>,logged:Boolean,join:(String)->Unit){LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){item{Title("Communities","Find people who share your interests")};if(data.isEmpty())item{Empty("No communities yet","Check back later.")};items(data){c->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(c.icon,null,Modifier.size(48.dp));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(c.name,fontWeight=FontWeight.Bold);Text(c.description,style=MaterialTheme.typography.bodySmall)};Button(onClick={join(c.id)},enabled=logged){Text("Join")}}}}}}
@Composable private fun MessagesScreen(data:List<Conversation>,chat:String,name:String,rows:List<JSONObject>,text:String,setText:(String)->Unit,open:(Conversation)->Unit,back:()->Unit,send:()->Unit){if(chat.isBlank())LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Title("Messages","Your direct conversations")};if(data.isEmpty())item{Empty("No conversations","Your chats will appear here.")};items(data){c->Card(Modifier.fillMaxWidth().clickable{open(c)}){ListItem(headlineContent={Text(c.name)},supportingContent={Text(c.last)},leadingContent={AsyncImage(c.avatar,null,Modifier.size(46.dp))})}}}else Column(Modifier.fillMaxSize().padding(12.dp)){TextButton(back){Icon(Icons.Default.ArrowBack,null);Text(name)};LazyColumn(Modifier.weight(1f)){items(rows){m->Text(m.optString("body"),Modifier.fillMaxWidth().padding(9.dp))}};Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(text,setText,Modifier.weight(1f),placeholder={Text("Message…")});IconButton(send,enabled=text.isNotBlank()){Icon(Icons.Default.Send,null)}}}}
@Composable private fun NotificationsScreen(data:List<Notice>,read:(String)->Unit){LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Title("Notifications","Updates from BaltyGram")};if(data.isEmpty())item{Empty("You're all caught up","New notifications appear here.")};items(data){n->Card(Modifier.fillMaxWidth().clickable{read(n.id)}){Column(Modifier.padding(14.dp)){Text(n.title,fontWeight=FontWeight.Bold);Text(n.body);if(!n.read)Text("UNREAD",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelSmall)}}}}}
@Composable private fun DownloadsScreen(data:List<DownloadRow>,refresh:()->Unit){Column(Modifier.padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("Downloads",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(refresh){Icon(Icons.Default.Refresh,null)}};if(data.isEmpty())Empty("No download history","Your downloads will appear here.")else LazyColumn{items(data){ListItem(headlineContent={Text(it.name)},supportingContent={Text("${it.version} • ${it.status}")},leadingContent={Icon(Icons.Default.Download,null)})}}}}
@Composable private fun ListScreen(title:String,data:List<Listing>,empty:String,open:(Listing)->Unit){LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)};if(data.isEmpty())item{Empty("Nothing here",empty)};items(data){ListingCard(it,open)}}}
@Composable private fun ExternalScreen(title:String,desc:String,button:String,click:()->Unit){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(desc);Button(click){Text(button)}}}
@Composable private fun Empty(title:String,sub:String){Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.SearchOff,null,tint=MaterialTheme.colorScheme.onSurfaceVariant);Text(title,fontWeight=FontWeight.Bold);Text(sub,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
