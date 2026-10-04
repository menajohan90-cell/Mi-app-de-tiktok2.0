import re

with open('backend/server.js', 'r') as f:
    code = f.read()

def replace_get_videos(match):
    return """app.get('/api/videos', async (req, res) => {
    try {
        const db = await dbPromise;
        const feedType = req.query.type || 'FYP';
        const userId = req.userId || 'anonymous';
        
        let query = `
            SELECT v.*, u.username, u.displayName, u.photoUrl,
            (SELECT COUNT(*) FROM likes WHERE targetId = v.videoId AND type = 'video') as likesCount,
            (SELECT COUNT(*) FROM comments WHERE videoId = v.videoId) as commentsCount,
            (SELECT COUNT(*) FROM shares WHERE videoId = v.videoId) as sharesCount,
            CASE WHEN (SELECT COUNT(*) FROM likes WHERE targetId = v.videoId AND type = 'video' AND uid = ?) > 0 THEN 1 ELSE 0 END as isLiked
            FROM videos v
            JOIN users u ON v.uid = u.uid
        `;
        let params = [userId];
        
        if (feedType.startsWith('SEARCH_')) {
            const searchTerm = feedType.replace('SEARCH_', '');
            query += ` WHERE v.description LIKE ? ORDER BY v.createdAt DESC`;
            params.push(`%${searchTerm}%`);
        } else if (feedType === 'Amigos') {
            query += ` JOIN connections c ON v.uid = c.receiverId WHERE c.requesterId = ? AND c.status = 'CONNECTED' ORDER BY v.createdAt DESC`;
            params.push(userId);
        } else if (feedType === 'PROFILE') {
            const targetUid = req.query.uid;
            if (targetUid) {
                query += ` WHERE v.uid = ? ORDER BY v.createdAt DESC`;
                params.push(targetUid);
            } else {
                query += ` ORDER BY v.createdAt DESC`;
            }
        } else {
            query += ` ORDER BY v.createdAt DESC`;
        }
        
        const videos = await db.all(query, params);
        // Convert isLiked 0/1 to boolean
        const formatted = videos.map(v => ({ ...v, isLiked: v.isLiked === 1 }));
        res.json(formatted);
    } catch (error) { handleError(res, error); }
});"""

code = re.sub(r"app\.get\('/api/videos', async \(req, res\) => \{.*?(?=\napp\.get\('/api/videos/:videoId/comments')", replace_get_videos, code, flags=re.DOTALL)

with open('backend/server.js', 'w') as f:
    f.write(code)

