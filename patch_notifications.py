import re

with open('backend/server.js', 'r') as f:
    code = f.read()

# Add table creation
if "CREATE TABLE IF NOT EXISTS notifications" not in code:
    code = code.replace(
        "CREATE TABLE IF NOT EXISTS messages",
        """CREATE TABLE IF NOT EXISTS notifications (
                id TEXT PRIMARY KEY,
                userId TEXT,
                actorId TEXT,
                type TEXT,
                message TEXT,
                targetId TEXT,
                timestamp INTEGER
            );
            CREATE TABLE IF NOT EXISTS messages"""
    )

# Endpoints for notifications
if "app.get('/api/notifications'" not in code:
    code += """
app.get('/api/notifications', async (req, res) => {
    try {
        const db = await dbPromise;
        const notifications = await db.all(`
            SELECT n.*, u.username, u.photoUrl
            FROM notifications n
            JOIN users u ON n.actorId = u.uid
            WHERE n.userId = ?
            ORDER BY n.timestamp DESC
        `, [req.userId]);
        res.json(notifications);
    } catch (error) { handleError(res, error); }
});
"""

# Modify comment like to send notification
if "nId: 'notif'" not in code:
    # Need to replace the body of like comment
    like_comment_logic = """app.post('/api/comments/:commentId/like', async (req, res) => {
    try {
        const db = await dbPromise;
        await db.run('INSERT OR IGNORE INTO likes (uid, targetId, type) VALUES (?, ?, ?)', [req.userId, req.params.commentId, 'comment']);
        const count = await db.get('SELECT COUNT(*) as c FROM likes WHERE targetId = ? AND type = ?', [req.params.commentId, 'comment']);
        
        // Notify comment author
        const comment = await db.get('SELECT uid FROM comments WHERE id = ?', req.params.commentId);
        if (comment && comment.uid !== req.userId) {
            const actor = await db.get('SELECT username FROM users WHERE uid = ?', req.userId);
            const msg = `${actor.username} le ha gustado tu comentario`;
            await db.run('INSERT INTO notifications (id, userId, actorId, type, message, targetId, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)',
                ['notif' + Date.now(), comment.uid, req.userId, 'LIKE_COMMENT', msg, req.params.commentId, Date.now()]);
        }
        
        res.json({ likesCount: count.c, isLiked: true });
    } catch (error) { handleError(res, error); }
});"""
    code = re.sub(r"app\.post\('/api/comments/:commentId/like', async \(req, res\) => \{.*?(?=\napp\.delete\('/api/comments/:commentId/like')", like_comment_logic, code, flags=re.DOTALL)

    # Modify reply comment to send notification
    reply_comment_logic = """
        const user = await db.get('SELECT username, photoUrl FROM users WHERE uid = ?', req.userId);
        
        if (parentCommentId) {
            const parent = await db.get('SELECT uid FROM comments WHERE id = ?', parentCommentId);
            if (parent && parent.uid !== req.userId) {
                const msg = `${user.username} ha respondido a tu comentario`;
                await db.run('INSERT INTO notifications (id, userId, actorId, type, message, targetId, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)',
                    ['notif' + Date.now(), parent.uid, req.userId, 'REPLY_COMMENT', msg, parentCommentId, Date.now()]);
            }
        }
        """
    code = code.replace("const user = await db.get('SELECT username, photoUrl FROM users WHERE uid = ?', req.userId);", reply_comment_logic)

    # Modify video like to send notification
    like_video_logic = """app.post('/api/videos/:videoId/like', async (req, res) => {
    try {
        const db = await dbPromise;
        await db.run('INSERT OR IGNORE INTO likes (uid, targetId, type) VALUES (?, ?, ?)', [req.userId, req.params.videoId, 'video']);
        const count = await db.get('SELECT COUNT(*) as c FROM likes WHERE targetId = ? AND type = ?', [req.params.videoId, 'video']);
        
        // Notify video author
        const video = await db.get('SELECT uid FROM videos WHERE videoId = ?', req.params.videoId);
        if (video && video.uid !== req.userId) {
            const actor = await db.get('SELECT username FROM users WHERE uid = ?', req.userId);
            const msg = `${actor.username} le ha gustado tu publicación`;
            await db.run('INSERT INTO notifications (id, userId, actorId, type, message, targetId, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)',
                ['notif' + Date.now(), video.uid, req.userId, 'LIKE_VIDEO', msg, req.params.videoId, Date.now()]);
        }
        
        res.json({ likesCount: count.c, isLiked: true });
    } catch (error) { handleError(res, error); }
});"""
    code = re.sub(r"app\.post\('/api/videos/:videoId/like', async \(req, res\) => \{.*?(?=\napp\.delete\('/api/videos/:videoId/like')", like_video_logic, code, flags=re.DOTALL)


with open('backend/server.js', 'w') as f:
    f.write(code)

