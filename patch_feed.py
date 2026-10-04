import re

with open("app/src/main/java/com/example/ui/screens/FeedScreen.kt", "r") as f:
    content = f.read()

# Add necessary imports
imports = """
import androidx.compose.material.icons.filled.Warning
import com.example.ui.screens.SystemChatHelper
import kotlinx.coroutines.launch
import android.widget.Toast
"""
content = content.replace("package com.example.ui.screens\n", "package com.example.ui.screens\n" + imports)

# We need a scope in FeedScreen
if "val scope = rememberCoroutineScope()" not in content:
    content = content.replace("val context = LocalContext.current", "val context = LocalContext.current\n    val scope = rememberCoroutineScope()")

# Replace interaction buttons to include Report
old_buttons = """            InteractionButton(icon = Icons.Default.FavoriteBorder, text = "142.3K") // Styled as in mockup
            InteractionButton(icon = Icons.Default.ChatBubbleOutline, text = "1.2K")
            InteractionButton(icon = Icons.Default.Share, text = "Compartir")"""

new_buttons = """            InteractionButton(icon = Icons.Default.FavoriteBorder, text = "142.3K", onClick = {}) 
            InteractionButton(icon = Icons.Default.ChatBubbleOutline, text = "1.2K", onClick = {})
            InteractionButton(icon = Icons.Default.Share, text = "Compartir", onClick = {})
            InteractionButton(icon = Icons.Default.Warning, text = "Reportar", onClick = {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    scope.launch {
                        try {
                            val report = hashMapOf(
                                "reporterId" to uid,
                                "reportedVideoId" to videoModel.videoId,
                                "reportedUserId" to videoModel.uid,
                                "timestamp" to System.currentTimeMillis(),
                                "status" to "pending"
                            )
                            FirebaseFirestore.getInstance().collection("reports").add(report).await()
                            SystemChatHelper.sendSystemMessage(uid, "Recibimos tu reporte. Nuestro sistema de seguridad revisará la información enviada.")
                            Toast.makeText(context, "Reporte enviado. Gracias por ayudarnos a mantener la comunidad segura.", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al enviar reporte", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Debes iniciar sesión para reportar", Toast.LENGTH_SHORT).show()
                }
            })"""

content = content.replace(old_buttons, new_buttons)

# Update InteractionButton signature
old_ib = """@Composable
fun InteractionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {"""
new_ib = """@Composable
fun InteractionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit = {}) {"""
content = content.replace(old_ib, new_ib)

old_ib_click = """    Column(
        modifier = Modifier.padding(bottom = 16.dp),"""
new_ib_click = """    Column(
        modifier = Modifier.padding(bottom = 16.dp).clickable { onClick() },"""
content = content.replace(old_ib_click, new_ib_click)

with open("app/src/main/java/com/example/ui/screens/FeedScreen.kt", "w") as f:
    f.write(content)
