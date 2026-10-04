import re

with open("app/src/main/java/com/example/ui/screens/FeedScreen.kt", "r") as f:
    content = f.read()

# The incorrect insertion looks like:
#         Column(
#         modifier = Modifier.padding(bottom = 16.dp).clickable { onClick() },
#             modifier = Modifier

bad_str = """        Column(
        modifier = Modifier.padding(bottom = 16.dp).clickable { onClick() },
            modifier = Modifier"""
good_str = """        Column(
            modifier = Modifier"""
content = content.replace(bad_str, good_str)

with open("app/src/main/java/com/example/ui/screens/FeedScreen.kt", "w") as f:
    f.write(content)
