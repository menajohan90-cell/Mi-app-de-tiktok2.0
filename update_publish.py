import re

with open("app/src/main/java/com/example/ui/screens/PublishScreen.kt", "r") as f:
    content = f.read()

old_call = """                                        val result = postRepository.publishVideo(
                                            videoId = videoId,
                                             //  videoFile,
                                            videoUrl = "", // Empty fallback since we upload
                                            description = description,
                                            hashtags = hashtagsList,
                                            username = currentUserProfile?.username ?: user?.displayName?.lowercase()?.replace(" " , "") ?: "invitado",
                                            displayName = currentUserProfile?.displayName ?: user?.displayName ?: "Invitado"
                                        )"""

new_call = """                                        val result = postRepository.publishVideo(
                                            videoId = videoId,
                                            localUriString = selectedMediaUri.toString(),
                                            description = description,
                                            hashtags = hashtagsList,
                                            username = currentUserProfile?.username ?: user?.displayName?.lowercase()?.replace(" " , "") ?: "invitado",
                                            displayName = currentUserProfile?.displayName ?: user?.displayName ?: "Invitado",
                                            visibility = "PUBLIC" // TODO: Add visibility state
                                        )
                                        
                                        // Show private security review animation
                                        navController.navigate("security_review")"""

content = content.replace(old_call, new_call)

with open("app/src/main/java/com/example/ui/screens/PublishScreen.kt", "w") as f:
    f.write(content)
