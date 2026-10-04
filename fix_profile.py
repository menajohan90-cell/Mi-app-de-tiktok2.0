import re
with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    content = f.read()

# Strip everything from the FIRST occurrence of @Composable fun LoggedInView
index = content.find("@Composable\nfun LoggedInView")
if index == -1:
    index = content.find("@Composable\nfun LoggedInView")
if index == -1:
    index = content.find("@Composable\r\nfun LoggedInView")
if index != -1:
    content = content[:index]

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(content)
