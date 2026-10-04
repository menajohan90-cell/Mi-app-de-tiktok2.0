import os

with open('backend/server.js', 'r') as f:
    code = f.read()

code = code.replace(
    "const mockName = \"Usuario \" + req.params.uid.substring(0, 4);",
    """
    if (req.params.uid === 'system') {
        return res.json({
            uid: 'system',
            username: 'Sistema',
            displayName: 'Notificaciones del Sistema',
            photoUrl: 'https://ui-avatars.com/api/?name=S&background=EF4444&color=fff',
            bio: 'Avisos oficiales.',
            followersCount: 0,
            followingCount: 0,
            isProfileComplete: true,
            suspended: false
        });
    }
    const mockName = "Usuario " + req.params.uid.substring(0, 4);
    """
)

with open('backend/server.js', 'w') as f:
    f.write(code)

