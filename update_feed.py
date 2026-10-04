import re

with open("app/src/main/java/com/example/ui/screens/FeedScreen.kt", "r") as f:
    content = f.read()

# Make sure FeedScreen can handle processing state
video_player_pattern = r'fun VideoPlayerItem\(videoModel: VideoModel, isPlaying: Boolean, navController: androidx.navigation.NavController, onVideoUpdated: \(VideoModel\) -> Unit = \{\}\) \{'
new_video_player = """fun VideoPlayerItem(videoModel: VideoModel, isPlaying: Boolean, navController: androidx.navigation.NavController, onVideoUpdated: (VideoModel) -> Unit = {}) {
    if (videoModel.status == "UPLOADING" || videoModel.status == "SECURITY_REVIEW" || videoModel.status == "PROCESSING") {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(if (videoModel.status == "SECURITY_REVIEW") "Revisando seguridad..." else "Procesando video...", color = Color.White)
            }
        }
        return
    }"""
content = content.replace(video_player_pattern, new_video_player)

with open("app/src/main/java/com/example/ui/screens/FeedScreen.kt", "w") as f:
    f.write(content)
