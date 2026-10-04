import os

with open('app/src/main/java/com/example/data/VideoModel.kt', 'r') as f:
    content = f.read()

if 'val isLiked: Boolean = false' not in content:
    content = content.replace(
        'val sharesCount: Int = 0',
        'val sharesCount: Int = 0,\n    val isLiked: Boolean = false'
    )
    with open('app/src/main/java/com/example/data/VideoModel.kt', 'w') as f:
        f.write(content)

