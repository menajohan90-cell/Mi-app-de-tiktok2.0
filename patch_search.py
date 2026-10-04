import os

with open('backend/server.js', 'r') as f:
    code = f.read()

code = code.replace("req.query.q || ''", "req.query.query || req.query.q || ''")

with open('backend/server.js', 'w') as f:
    f.write(code)

