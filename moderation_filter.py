#!/usr/bin/env python3
import sys
import re

PROFANITY_WORDS = [
    "puta", "puto", "mierda", "estupido", "estupida", "imbecil", "cabron", 
    "pendejo", "pendeja", "chinga", "verga", "cojones", "idiota", "marrano",
    "nsfw", "sexo", "desnudo", "desnuda", "porn", "xxx"
]

def check_content(text):
    if not text:
        return True, "OK"
    
    text_lower = text.lower()
    for word in PROFANITY_WORDS:
        if re.search(r'\b' + word + r'\b', text_lower) or word in text_lower:
            return False, f"Contenido inapropiado detectado: contiene lenguaje explícito o groserías ('{word}')."
            
    return True, "OK"

if __name__ == "__main__":
    if len(sys.argv) > 1:
        content = " ".join(sys.argv[1:])
        allowed, msg = check_content(content)
        print(f"Allowed: {allowed}")
        print(f"Reason: {msg}")
        sys.exit(0 if allowed else 1)
    else:
        print("Usage: python3 moderation_filter.py 'text to check'")
        sys.exit(0)
