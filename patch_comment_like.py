import re

with open('app/src/main/java/com/example/ui/screens/FeedScreen.kt', 'r') as f:
    code = f.read()

# Fix CommentItem signature and add state
new_comment_item_sig = """fun CommentItem(
    comment: CommentResponse, 
    videoOwnerId: String, 
    repository: PostRepository,
    onReply: (CommentResponse) -> Unit
) {
    var isLiked by remember(comment.id) { mutableStateOf(comment.isLiked) }
    var likesCount by remember(comment.id) { mutableStateOf(comment.likesCount) }
    val scope = rememberCoroutineScope()
    
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {"""
code = re.sub(r"fun CommentItem\(comment: CommentResponse, videoOwnerId: String, onReply: \(CommentResponse\) -> Unit\) \{\n    Column\(modifier = Modifier\.padding\(horizontal = 16\.dp, vertical = 8\.dp\)\.fillMaxWidth\(\)\) \{", new_comment_item_sig, code)

# Fix the Icon click and tint
old_icon_block = """            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = AppIcons.Heart, 
                    contentDescription = null, 
                    tint = if (comment.isLiked) Color.Red else Color.DarkGray,
                    modifier = Modifier.size(16.dp)
                )
                Text(formatCount(comment.likesCount), color = Color.DarkGray, fontSize = 10.sp)
            }"""
new_icon_block = """            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                scope.launch {
                    val result = if (isLiked) repository.apiInstance.unlikeComment(comment.id) else repository.apiInstance.likeComment(comment.id)
                    if (result.isSuccessful && result.body() != null) {
                        isLiked = result.body()!!.isLiked
                        likesCount = result.body()!!.likesCount
                    }
                }
            }) {
                Icon(
                    imageVector = AppIcons.Heart, 
                    contentDescription = null, 
                    tint = if (isLiked) Color.Red else Color.DarkGray,
                    modifier = Modifier.size(16.dp)
                )
                Text(formatCount(likesCount), color = Color.DarkGray, fontSize = 10.sp)
            }"""
code = code.replace(old_icon_block, new_icon_block)

# Fix recursive calls to CommentItem
code = code.replace("CommentItem(reply, videoOwnerId, onReply)", "CommentItem(reply, videoOwnerId, repository, onReply)")

# Fix the call in CommentsSection
code = code.replace("CommentItem(comment, videoOwnerId, onReply = { replyingTo = it })", "CommentItem(comment, videoOwnerId, repository, onReply = { replyingTo = it })")

# Wait, we also need to change the image in CommentItem to load `photoUrl`!
# The current code uses a letter in a Box!
# Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.DarkGray), contentAlignment = Alignment.Center) {
#    Text(comment.username.take(1).uppercase(), color = Color.White, fontSize = 14.sp)
# }
old_avatar = """Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.DarkGray), contentAlignment = Alignment.Center) {
                Text(comment.username.take(1).uppercase(), color = Color.White, fontSize = 14.sp)
            }"""
new_avatar = """AsyncImage(
                model = if (comment.userProfilePic.isNotEmpty()) comment.userProfilePic else "https://ui-avatars.com/api/?name=${comment.username}",
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.DarkGray)
            )"""
code = code.replace(old_avatar, new_avatar)

with open('app/src/main/java/com/example/ui/screens/FeedScreen.kt', 'w') as f:
    f.write(code)

