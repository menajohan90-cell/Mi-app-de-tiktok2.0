import re

with open('app/src/main/java/com/example/ui/navigation/MainNavigationScreen.kt', 'r') as f:
    code = f.read()

code = code.replace("navController = mainNavController", "navController = navController")

with open('app/src/main/java/com/example/ui/navigation/MainNavigationScreen.kt', 'w') as f:
    f.write(code)

