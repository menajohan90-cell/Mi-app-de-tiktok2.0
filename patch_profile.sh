sed -i '/@Composable\nfun LoggedInView/,$d' app/src/main/java/com/example/ui/screens/ProfileScreen.kt
cat << 'INNER_EOF' >> app/src/main/java/com/example/ui/screens/ProfileScreen.kt
@Composable
fun LoggedInView(user: FirebaseUser?, onLogout: () -> Unit) {
    var profileData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var userVideos by remember { mutableStateOf<List<com.example.data.VideoModel>>(emptyList()) }
        
    LaunchedEffect(user) {
        if (user != null) {
            val db = FirestoreManager.instance
            val snapshot = db.collection("users").document(user.uid).get().await()
            if (snapshot.exists()) {
                profileData = snapshot.data
            }
            
            // Load User Videos
            db.collection("videos")
                .whereEqualTo("uid", user.uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { vSnap, e ->
                    if (e == null && vSnap != null) {
                        userVideos = vSnap.documents.mapNotNull { it.toObject(com.example.data.VideoModel::class.java)?.copy(videoId = it.id) }
                    }
                }
        }
    }
        
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        AsyncImage(
            model = profileData?.get("photoUrl") as? String ?: user?.photoUrl,
            contentDescription = "Foto de perfil",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
        )
                
        Spacer(modifier = Modifier.height(16.dp))
                
        Text(
            text = "@${profileData?.get("username") as? String ?: "usuario"}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
                
        Spacer(modifier = Modifier.height(8.dp))
                
        Text(
            text = profileData?.get("displayName") as? String ?: user?.displayName ?: "",
            fontSize = 16.sp,
            color = Color.LightGray
        )
                
        Spacer(modifier = Modifier.height(24.dp))
                
        OutlinedButton(
            onClick = onLogout,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(40.dp)
        ) {
            Text("Cerrar Sesión")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        HorizontalDivider(thickness = 1.dp, color = Color.DarkGray)
        
        Text(
            "Mis Videos",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp).align(Alignment.Start)
        )
        
        // Video grid
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(1.dp)
        ) {
            androidx.compose.foundation.lazy.grid.items(userVideos) { video ->
                Box(
                    modifier = Modifier
                        .aspectRatio(9f / 16f)
                        .padding(1.dp)
                        .background(Color.DarkGray)
                        .clickable { /* Play video */ }
                ) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(video.videoUrl)
                            .decoderFactory(coil.decode.VideoFrameDecoder.Factory())
                            .videoFrameMillis(1000)
                            .build(),
                        contentDescription = "Thumbnail",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${video.views}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
INNER_EOF
