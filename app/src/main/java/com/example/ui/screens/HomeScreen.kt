package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.example.data.StoryResponse
import com.example.data.PostRepository
import com.example.data.UserRepository

@Composable
fun StoryBar(navController: androidx.navigation.NavController) {
    val postRepository = remember { PostRepository() }
    var stories by remember { mutableStateOf<List<StoryResponse>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        try {
            val result = postRepository.getStories()
            if (result.isSuccess) {
                stories = result.getOrNull() ?: emptyList()
            }
        } catch (e: Exception) {
            // Ignore for story bar so it fails gracefully (shows no stories instead of crashing)
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clickable { navController.navigate("chats") },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    com.example.ui.components.UserAvatar(
                        photoUrl = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString(),
                        size = 58.dp,
                        borderWidth = 2.dp,
                        borderColor = Color(0xFF3B82F6)
                    )
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6))
                            .border(1.5.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Subir historia", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tu historia", color = Color.White, fontSize = 11.sp)
            }
        }
        
        items(stories) { story ->
            val isCustom = story.soundName == "Personalizado" 
            val ringColor = when {
                story.isViewed -> Color.Gray.copy(alpha = 0.5f) // Viewed
                isCustom -> Color(0xFFF59E0B) // Custom ring (yellow/gold)
                else -> Color(0xFF3B82F6) // Standard ring (blue)
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { navController.navigate("story_view/${story.storyId}") }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, ringColor, CircleShape)
                        .padding(3.dp)
                ) {
                    AsyncImage(
                        model = if (story.thumbnailUrl.isNotBlank()) story.thumbnailUrl else story.mediaUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(story.username.take(8), color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: androidx.navigation.NavController) {
    val tabs = listOf("Amigos", "Para Ti")
    val pagerState = rememberPagerState(initialPage = 1) { tabs.size } // Start on Para Ti
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> FeedScreen(navController = navController, feedType = "Amigos")
                1 -> FeedScreen(navController = navController, feedType = "FYP")
            }
        }

        // Overlay Content
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            // Top Tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Amigos Tab
                    val isAmigos = pagerState.currentPage == 0
                    Text(
                        text = "Amigos",
                        color = if (isAmigos) Color.White else Color.White.copy(alpha = 0.6f),
                        fontWeight = if (isAmigos) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )
                    
                    // Para Ti Tab
                    val isParaTi = pagerState.currentPage == 1
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    ) {
                        Text(
                            text = "Para Ti",
                            color = if (isParaTi) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (isParaTi) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                        if (isParaTi) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .width(20.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                }
            }
            
            // Story Bar
            StoryBar(navController)
        }
    }
}
