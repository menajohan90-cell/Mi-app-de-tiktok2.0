import re

with open('app/src/main/java/com/example/ui/screens/FeedScreen.kt', 'r') as f:
    code = f.read()

code = code.replace("import androidx.compose.ui.text.SpanStyle", "import androidx.compose.ui.text.SpanStyle\nimport coil.compose.AsyncImage\nimport coil.request.ImageRequest")

with open('app/src/main/java/com/example/ui/screens/FeedScreen.kt', 'w') as f:
    f.write(code)

