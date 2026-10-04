import re

with open('app/src/main/java/com/example/ui/screens/FeedScreen.kt', 'r') as f:
    code = f.read()

# Replace:
# var isLiked by remember(safeVideoId) { mutableStateOf(false) }
# var likesCount by remember(safeVideoId) { mutableStateOf(videoModel.likesCount) }
# with initializing from videoModel

code = code.replace(
    'var isLiked by remember(safeVideoId) { mutableStateOf(false) }',
    'var isLiked by remember(safeVideoId) { mutableStateOf(videoModel.isLiked) }'
)

with open('app/src/main/java/com/example/ui/screens/FeedScreen.kt', 'w') as f:
    f.write(code)

