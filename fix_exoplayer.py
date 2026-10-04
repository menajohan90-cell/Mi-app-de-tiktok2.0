import os

def fix_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Find AndroidView(
    #        factory = { ctx -> ... }
    # and add update = { it.player = exoPlayer }
    if 'update = {' not in content and 'AndroidView(' in content and 'exoPlayer' in content:
        content = content.replace(
            'modifier = Modifier.fillMaxSize()',
            'modifier = Modifier.fillMaxSize(),\n                        update = { it.player = exoPlayer }'
        )
        content = content.replace(
            'modifier = Modifier\n                .fillMaxSize()',
            'update = { it.player = exoPlayer },\n            modifier = Modifier\n                .fillMaxSize()'
        )
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Fixed {filepath}")

fix_file('app/src/main/java/com/example/ui/screens/PublishScreen.kt')
fix_file('app/src/main/java/com/example/ui/screens/FeedScreen.kt')
fix_file('app/src/main/java/com/example/ui/navigation/MainNavigationScreen.kt')
