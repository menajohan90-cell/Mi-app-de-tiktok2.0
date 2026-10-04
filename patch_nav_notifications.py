import re

with open('app/src/main/java/com/example/ui/navigation/MainNavigationScreen.kt', 'r') as f:
    code = f.read()

if "import com.example.ui.screens.NotificationsScreen" not in code:
    code = code.replace("import com.example.ui.screens.ChatsScreen", "import com.example.ui.screens.ChatsScreen\nimport com.example.ui.screens.NotificationsScreen")

route_code = """
                composable("chat_detail/{chatId}") { backStackEntry ->
"""
new_route_code = """
                composable("notifications/{type}") { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type") ?: "novedades"
                    NotificationsScreen(navController = mainNavController, filterType = type)
                }
                composable("chat_detail/{chatId}") { backStackEntry ->
"""

code = code.replace(route_code, new_route_code)

with open('app/src/main/java/com/example/ui/navigation/MainNavigationScreen.kt', 'w') as f:
    f.write(code)

