package site.baltygram.app

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class Listing(val id: String, val name: String, val description: String, val category: String, val image: String = "")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BaltyGramApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaltyGramApp() {
    var page by remember { mutableStateOf("Home") }
    var selected by remember { mutableStateOf<Listing?>(null) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var dark by remember { mutableStateOf(false) }
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try {
                listings = SupabaseClient.fetchSoftware()
            } catch (e: Exception) {
                error = e.message ?: "Could not load listings. Check your Supabase configuration."
            } finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("BaltyGram", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { dark = !dark }) {
                            Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "Toggle theme")
                        }
                        IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "Refresh") }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf("Home", "Explore", "Saved", "Account").forEach { item ->
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item; selected = null },
                            icon = {
                                Icon(when(item) {
                                    "Home" -> Icons.Default.Home
                                    "Explore" -> Icons.Default.Explore
                                    "Saved" -> Icons.Default.Bookmark
                                    else -> Icons.Default.Person
                                }, item)
                            },
                            label = { Text(item) }
                        )
                    }
                }
            }
        ) { padding ->
            if (selected != null) {
                ListingDetail(selected!!, Modifier.padding(padding)) { selected = null }
            } else {
                when (page) {
                    "Home" -> HomeScreen(listings, loading, error, Modifier.padding(padding), onOpen = { selected = it }, onExplore = { page = "Explore" })
                    "Explore" -> ExploreScreen(listings, query, { query = it }, category, { category = it }, Modifier.padding(padding), onOpen = { selected = it })
                    "Saved" -> EmptyState("Saved items", "Sign in and save listings to build your collection.", Modifier.padding(padding))
                    else -> AccountScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
fun HomeScreen(items: List<Listing>, loading: Boolean, error: String?, modifier: Modifier, onOpen: (Listing)->Unit, onExplore: ()->Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Text("Discover digital tools.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Games, software, apps and resources from developers.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onExplore) { Text("Explore catalogue") }
            Spacer(Modifier.height(22.dp))
            Text("Featured discoveries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error != null) item { Text(error, color = MaterialTheme.colorScheme.error) }
        if (items.isEmpty() && !loading) item { Text("No listings loaded yet. Add Supabase configuration to connect your catalogue.") }
        items(items) { ListingCard(it, onOpen) }
    }
}

@Composable
fun ExploreScreen(items: List<Listing>, query: String, onQuery: (String)->Unit, category: String, onCategory: (String)->Unit, modifier: Modifier, onOpen: (Listing)->Unit) {
    val categories = listOf("All", "Games", "Software", "Apps", "AI Tools", "Editing", "Developer")
    val filtered = items.filter {
        (category == "All" || it.category.equals(category, true)) &&
        (it.name.contains(query, true) || it.description.contains(query, true))
    }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), label = { Text("Search BaltyGram") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.take(4).forEach { FilterChip(selected = category == it, onClick = { onCategory(it) }, label = { Text(it) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.drop(4).forEach { FilterChip(selected = category == it, onClick = { onCategory(it) }, label = { Text(it) }) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { ListingCard(it, onOpen) }
        }
    }
}

@Composable
fun ListingCard(item: Listing, onOpen: (Listing)->Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onOpen(item) }) {
        Column(Modifier.padding(16.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(item.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(5.dp))
            Text(item.description, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ListingDetail(item: Listing, modifier: Modifier, onBack: ()->Unit) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null); Text("Back") }
        Spacer(Modifier.height(20.dp))
        Text(item.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(item.category, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(item.description)
        Spacer(Modifier.height(24.dp))
        Text("More listing actions will be connected as the BaltyGram API integration is completed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.BookmarkBorder, null, Modifier.size(42.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AccountScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Your account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Authentication UI and profile syncing are planned for the next integration pass.")
    }
}
