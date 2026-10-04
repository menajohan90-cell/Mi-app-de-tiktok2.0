import sys

with open('backend/server.js', 'r') as f:
    code = f.read()

# Add welcome message on register
if "await db.run('INSERT INTO users" in code and "sys" not in code.split("app.post('/api/users/register'")[1][:500]:
    code = code.replace(
        "res.status(201).json(user);",
        """
        // Send welcome system message
        const sysMsg = {
            id: 'sys' + Date.now(),
            senderId: 'system',
            receiverId: uid,
            text: '¡Bienvenido a Momentos! Disfruta explorando y compartiendo.',
            timestamp: Date.now()
        };
        await db.run('INSERT INTO messages (id, senderId, receiverId, text, timestamp) VALUES (?, ?, ?, ?, ?)',
            [sysMsg.id, sysMsg.senderId, sysMsg.receiverId, sysMsg.text, sysMsg.timestamp]);
        
        res.status(201).json(user);
        """
    )

# Fix reports to send system message
if "app.post('/api/videos/:videoId/report'" not in code:
    code = code.replace(
        "app.post('/api/videos/shared', async (req, res) => {",
        """
app.post('/api/videos/:videoId/report', async (req, res) => {
    try {
        const db = await dbPromise;
        const { reason, description } = req.body;
        
        const video = await db.get('SELECT uid FROM videos WHERE videoId = ?', req.params.videoId);
        if (video) {
            // Log the report (optional, assuming we just send a system message to the author)
            
            // Send warning system message to video author
            const sysMsg = {
                id: 'sys_warn' + Date.now(),
                senderId: 'system',
                receiverId: video.uid,
                text: `Advertencia: Tu publicación ha sido reportada por "${reason}". Por favor respeta las normas de la comunidad o tu cuenta podría ser suspendida.`,
                timestamp: Date.now()
            };
            await db.run('INSERT INTO messages (id, senderId, receiverId, text, timestamp) VALUES (?, ?, ?, ?, ?)',
                [sysMsg.id, sysMsg.senderId, sysMsg.receiverId, sysMsg.text, sysMsg.timestamp]);
        }
        
        res.json({ message: "Reportado" });
    } catch (error) { handleError(res, error); }
});

app.post('/api/videos/shared', async (req, res) => {"""
    )

# Fix shares to log in 'shares' table
if "app.post('/api/videos/:videoId/share'" not in code:
    code = code.replace(
        "app.post('/api/videos/shared', async (req, res) => {",
        """
app.post('/api/videos/:videoId/share', async (req, res) => {
    try {
        const db = await dbPromise;
        const { type, targetUserIds } = req.body;
        
        await db.run('INSERT INTO shares (id, userId, videoId, platform, timestamp) VALUES (?, ?, ?, ?, ?)',
            ['share' + Date.now(), req.userId, req.params.videoId, type || 'internal', Date.now()]);
            
        res.json({ message: "Compartido" });
    } catch (error) { handleError(res, error); }
});

app.post('/api/videos/shared', async (req, res) => {"""
    )

# Ensure 'shares' table exists
if "CREATE TABLE IF NOT EXISTS shares" not in code:
    code = code.replace(
        "CREATE TABLE IF NOT EXISTS messages",
        """CREATE TABLE IF NOT EXISTS shares (
                id TEXT PRIMARY KEY,
                userId TEXT,
                videoId TEXT,
                platform TEXT,
                timestamp INTEGER
            );
            CREATE TABLE IF NOT EXISTS messages"""
    )
    
with open('backend/server.js', 'w') as f:
    f.write(code)
