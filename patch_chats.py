import re

with open('app/src/main/java/com/example/ui/screens/ChatsScreen.kt', 'r') as f:
    code = f.read()

# Add necessary icons
if "import androidx.compose.material.icons.filled.Group" not in code:
    code = code.replace("import androidx.compose.material.icons.filled.Warning", 
    "import androidx.compose.material.icons.filled.Warning\nimport androidx.compose.material.icons.filled.Group\nimport androidx.compose.material.icons.filled.Favorite")

# Add the 3 items
items_code = """
                // --- 3 Fixed Sections ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController?.navigate("chat_detail/system") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF3B82F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Sistema", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Sistema", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Notificaciones importantes", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController?.navigate("notifications/connections") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = "Conexiones", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Nuevas conexiones", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Nuevos seguidores", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController?.navigate("notifications/novedades") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Novedades", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Novedades", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Likes, comentarios y más", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
                // -------------------------
                
                item {
"""

code = code.replace("LazyColumn(modifier = Modifier.fillMaxSize()) {\n                item {", "LazyColumn(modifier = Modifier.fillMaxSize()) {\n" + items_code)

with open('app/src/main/java/com/example/ui/screens/ChatsScreen.kt', 'w') as f:
    f.write(code)

