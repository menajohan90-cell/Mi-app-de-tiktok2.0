package com.example.utils

object ContentModerator {
    private val profanityList = listOf(
        "puta", "puto", "mierda", "estupido", "estupida", "imbecil", "cabron",
        "pendejo", "pendeja", "chinga", "verga", "cojones", "idiota", "marrano",
        "nsfw", "sexo", "desnudo", "desnuda", "porn", "xxx"
    )

    fun isContentAllowed(text: String): Pair<Boolean, String> {
        if (text.isBlank()) return Pair(true, "")
        val lower = text.lowercase()
        for (word in profanityList) {
            if (lower.contains(word)) {
                return Pair(false, "Mensaje bloqueado: contiene lenguaje explícito o groserías no permitidas.")
            }
        }
        return Pair(true, "")
    }

    fun isMediaAllowed(uriString: String): Pair<Boolean, String> {
        val lower = uriString.lowercase()
        if (lower.contains("nsfw") || lower.contains("explicit") || lower.contains("porn")) {
            return Pair(false, "Imagen o video bloqueado: contenido explícito detectado por el sistema de seguridad.")
        }
        return Pair(true, "")
    }
}
