package com.example.utils

object UsernameValidator {

    private val RESERVED_USERNAMES = setOf(
        "admin", "administrator", "moderator", "mod", "support", "soporte",
        "system", "sistema", "official", "oficial", "staff", "owner"
    )

    private val BAD_WORDS = listOf(
        "puta", "puto", "mierda", "pendej", "cabron", "verga", "chinga", "joder", "idiota", "estupid",
        "zorra", "perra", "imbecil", "maricon", "nazi", "hitler", "porno", "violacion", "asesin", "suicid",
        "cojud", "gilipoll", "mamon", "capull", "tarad", "bastard", "malparid", "hijueput", "hijodeput",
        "coño", "culo", "marica", "pene", "p3n3"
    )
    
    private val SAFE_SUBSTRINGS = listOf("diputad", "computa", "reputa", "disputa")

    private val LEET_MAP = mapOf(
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's',
        '7' to 't', '8' to 'b', '@' to 'a', '$' to 's', '!' to 'i',
        '.' to ' ', '_' to ' ', '-' to ' ', '*' to ' '
    )

    enum class ValidationResult(val message: String?) {
        SUCCESS(null),
        INVALID_FORMAT("Usa únicamente letras, números y guiones bajos (_). La longitud debe ser entre 3 a 20 caracteres."),
        RESERVED("Este nombre de usuario no está permitido. Elige otro."),
        PROFANITY("Este nombre no cumple con nuestras normas comunitarias. Elige otro."),
        TAKEN("Este nombre de usuario ya está ocupado.")
    }

    fun normalize(input: String): String {
        val lower = input.lowercase()
        val normalized = StringBuilder()
        var prevChar: Char? = null
        
        for (char in lower) {
            val mappedChar = LEET_MAP[char] ?: char
            // Skip spaces and separator chars to collapse p.u.t.o to puto
            if (mappedChar == ' ' || char == '_' || char == '-' || char == '.' || char == '*' || char == '/' || char == '|') continue
            
            // Remove exact consecutive duplicates to prevent bypassing (e.g. "aaadddmiiinnn")
            if (mappedChar != prevChar) {
                normalized.append(mappedChar)
                prevChar = mappedChar
            }
        }
        return normalized.toString()
    }

    fun validateUsernameFormat(username: String): ValidationResult {
        if (!username.matches(Regex("^[a-zA-Z0-9_]{3,20}$"))) {
            return ValidationResult.INVALID_FORMAT
        }
        return ValidationResult.SUCCESS
    }

    fun validateContent(text: String): ValidationResult {
        val normalized = normalize(text)
        
        // Exact match for reserved
        if (RESERVED_USERNAMES.contains(normalized)) {
            return ValidationResult.RESERVED
        }
        
        // Substring check for reserved 
        if (RESERVED_USERNAMES.any { normalized.contains(it) && it.length > 4 }) {
             // Only check substrings for longer reserved words like "administrator", "official"
             // if they are embedded. E.g. "my_official_account". 
        }

        // Substring check for profanity
        for (badWord in BAD_WORDS) {
            if (normalized.contains(badWord)) {
                // Check if it's part of a safe word
                val isSafe = SAFE_SUBSTRINGS.any { normalized.contains(it) }
                if (!isSafe) {
                    return ValidationResult.PROFANITY
                }
            }
        }
        
        // Also check reserved words if they try to embed them to impersonate
        val criticalReserved = listOf("admin", "soporte", "sistema", "official")
        if (criticalReserved.any { normalized.contains(it) }) {
             return ValidationResult.RESERVED
        }
        
        return ValidationResult.SUCCESS
    }
}
