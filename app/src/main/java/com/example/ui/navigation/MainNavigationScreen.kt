package com.example.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.ModerationWrapper
import com.example.ui.screens.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

import android.net.Uri
import com.example.ui.screens.StoryEditorScreen
import com.example.ui.screens.StoryViewScreen
import com.example.data.UpdateRepository
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext

sealed class BottomNavItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    object Inicio : BottomNavItem("inicio", Icons.Default.Home, "Feed")
    object Navegar : BottomNavItem("navegar", Icons.Default.Explore, "Explorar")
    object Publish : BottomNavItem("publish", Icons.Default.Add, "Publicar")
    object Chats : BottomNavItem("chats", Icons.Default.Forum, "Chats")
    object Perfil : BottomNavItem("perfil", Icons.Default.Person, "Perfil")
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoSplashScreen(onTimeout: (Boolean) -> Unit) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            // Intenta cargar el video desde res/raw/splash_video.mp4
            val resourceId = context.resources.getIdentifier("splash_video", "raw", context.packageName)
            if (resourceId != 0) {
                val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/$resourceId")
                setMediaItem(mediaItem)
                prepare()
            } else {
                hasError = true
            }
        }
    }

    LaunchedEffect(hasError) {
        if (!hasError) {
            exoPlayer.play()
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        val auth = FirebaseAuth.getInstance()
                        onTimeout(auth.currentUser != null)
                    }
                }
            })
            // Fallback
            delay(5000)
            val auth = FirebaseAuth.getInstance()
            onTimeout(auth.currentUser != null)
        } else {
            delay(2000)
            val auth = FirebaseAuth.getInstance()
            onTimeout(auth.currentUser != null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!hasError) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                        update = { it.player = exoPlayer }
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MOMENTOS",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun MainNavigationScreen() {
    var showSplash by remember { mutableStateOf(true) }
    var startRoute by remember { mutableStateOf(BottomNavItem.Inicio.route) }
    val updateRepository = remember { UpdateRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var githubUpdateUrl by remember { mutableStateOf<String?>(null) }

    if (showSplash) {
        VideoSplashScreen(onTimeout = { isLoggedIn ->
            startRoute = if (isLoggedIn) BottomNavItem.Inicio.route else BottomNavItem.Perfil.route
            showSplash = false
        })
        return
    }

    val navController = rememberNavController()
    val context = LocalContext.current

    // Auto-check for GitHub updates
    LaunchedEffect(Unit) {
        scope.launch {
            delay(8000) // Esperar a que la app cargue
            updateRepository.getGitHubLatestRelease("menajohan90-cell/Mi-app-de-tiktok2.0").onSuccess { url ->
                githubUpdateUrl = url
                val result = snackbarHostState.showSnackbar(
                    message = "¡Nueva actualización disponible en GitHub!",
                    actionLabel = "Ver",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    navController.navigate("update_screen")
                }
            }
        }
    }
    
    val items = listOf(
        BottomNavItem.Inicio,
        BottomNavItem.Navegar,
        BottomNavItem.Publish,
        BottomNavItem.Chats,
        BottomNavItem.Perfil
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute in items.map { it.route }
    
    ModerationWrapper(
        onForceProfileUpdate = {
            if (currentRoute != "perfil") {
                navController.navigate("perfil") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomNav) {
                    Column {
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFF171717))
                        NavigationBar(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ) {
                            items.forEach { item ->
                                NavigationBarItem(
                                    icon = { 
                                        if (item.route == "publish") {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(Color.White, RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(item.icon, contentDescription = item.label, tint = Color.Black, modifier = Modifier.size(28.dp))
                                            }
                                        } else {
                                            Box {
                                                Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(28.dp))
                                            }
                                        }
                                    },
                                    label = {
                                        if (item.route != "publish") {
                                            Text(item.label, fontSize = 10.sp)
                                        }
                                    },
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        unselectedIconColor = Color(0xFF737373),
                                        selectedTextColor = Color.White,
                                        unselectedTextColor = Color(0xFF737373),
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Inicio.route) { HomeScreen(navController) }
                composable(BottomNavItem.Navegar.route) { DiscoverScreen(navController) }
                composable(BottomNavItem.Chats.route) { ChatsScreen(navController) }
                composable(BottomNavItem.Perfil.route) { ProfileScreen(navController) }
                composable("login") {
                    LoginScreen(
                        navController = navController,
                        onAuthSuccess = {
                            if (!navController.popBackStack("login", inclusive = true)) {
                                navController.popBackStack()
                            }
                        }
                    )
                }
                composable("register") {
                    RegisterScreen(
                        navController = navController,
                        onAuthSuccess = {
                            if (!navController.popBackStack("login", inclusive = true)) {
                                navController.popBackStack()
                            }
                        }
                    )
                }
                composable("profile/{uid}") { backStackEntry ->
                    val uid = backStackEntry.arguments?.getString("uid")
                    ProfileScreen(navController, uid)
                }
                composable(
                    route = "shared_chat_offline/{payload}",
                    deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "https://ais-pre-jo2awl56tqipvrtb37xrx2-270659541135.us-west2.run.app/shared_chat_offline/{payload}" })
                ) { backStackEntry ->
                    val payload = backStackEntry.arguments?.getString("payload") ?: ""
                    SharedChatScreen(navController, payload)
                }
                composable(BottomNavItem.Publish.route) { PublishScreen(navController) }
                composable("story_view/{storyId}") { backStackEntry ->
                    val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
                    StoryViewScreen(navController, storyId)
                }
                composable("story_editor/{uri}/{type}") { backStackEntry ->
                    val uriString = backStackEntry.arguments?.getString("uri") ?: ""
                    val uri = Uri.parse(Uri.decode(uriString))
                    val type = backStackEntry.arguments?.getString("type") ?: "video"
                    StoryEditorScreen(navController, uri, type)
                }
                composable("notifications/{type}") { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type") ?: "novedades"
                    NotificationsScreen(navController = navController, filterType = type)
                }
                composable("reports") {
                    ReportsScreen(navController = navController)
                }
                composable("admin_badges") {
                    AdminBadgePanelScreen(navController = navController)
                }
                composable("admin_panel") {
                    AdminPanelScreen(navController = navController)
                }
                composable("update_screen") {
                    UpdateScreen(navController = navController)
                }
                composable("admin_update_management") {
                    AdminUpdateManagementScreen(navController = navController)
                }
                composable("admin_verification") {
                    AdminVerificationScreen(navController = navController)
                }
                composable("report_detail/{reportId}") { backStackEntry ->
                    val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
                    ReportDetailScreen(navController = navController, reportId = reportId)
                }
                composable("chat_detail/{chatId}") { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                    ChatDetailScreen(navController, chatId)
                }
                composable("chat/{chatId}") { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                    ChatDetailScreen(navController, chatId)
                }
            }
        }
    }
}
