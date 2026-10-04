import re

with open("app/src/main/java/com/example/ui/screens/ChatDetailScreen.kt", "r") as f:
    content = f.read()
if "import androidx.compose.material.icons.filled.Star" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.CheckCircle", "import androidx.compose.material.icons.filled.CheckCircle\nimport androidx.compose.material.icons.filled.Star")
with open("app/src/main/java/com/example/ui/screens/ChatDetailScreen.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "r") as f:
    content = f.read()
if "import androidx.compose.material.icons.filled.Star" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.CheckCircle", "import androidx.compose.material.icons.filled.CheckCircle\nimport androidx.compose.material.icons.filled.Star")
with open("app/src/main/java/com/example/ui/screens/ChatsScreen.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    content = f.read()
content = content.replace("androidx.compose.foundation.lazy.grid.items(userVideos)", "items(userVideos)")
with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(content)
