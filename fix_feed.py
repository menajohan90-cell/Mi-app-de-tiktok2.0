import re

with open('app/src/main/java/com/example/ui/screens/FeedScreen.kt', 'r') as f:
    code = f.read()

# Fix Box 1
code = code.replace("Box(modifier = Modifier.fillMaxSize(),\n                        update = { it.player = exoPlayer }, contentAlignment = Alignment.Center)",
                    "Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center)")
                    
# Fix Box 2 & 3
code = code.replace("modifier = Modifier.fillMaxSize(),\n                        update = { it.player = exoPlayer }.padding(32.dp)",
                    "modifier = Modifier.fillMaxSize().padding(32.dp)")

# Fix VerticalPager
code = code.replace("state = pagerState,\n            update = { it.player = exoPlayer },\n            modifier = Modifier\n                .fillMaxSize()",
                    "state = pagerState,\n            modifier = Modifier.fillMaxSize()")
                    
# Fix Box 4
code = code.replace("Box(modifier = Modifier.fillMaxSize(),\n                        update = { it.player = exoPlayer }) {",
                    "Box(modifier = Modifier.fillMaxSize()) {")

with open('app/src/main/java/com/example/ui/screens/FeedScreen.kt', 'w') as f:
    f.write(code)

