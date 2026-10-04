import re

with open("app/src/main/java/com/example/data/PostRepository.kt", "r") as f:
    content = f.read()

old_query = """            val query = db.collection("videos")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)"""

new_query = """            val query = db.collection("videos")
                .whereEqualTo("visibility", "PUBLIC")
                .whereEqualTo("isDraft", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)"""

content = content.replace(old_query, new_query)

with open("app/src/main/java/com/example/data/PostRepository.kt", "w") as f:
    f.write(content)
