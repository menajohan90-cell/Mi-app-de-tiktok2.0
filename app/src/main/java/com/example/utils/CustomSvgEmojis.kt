package com.example.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class HeadGradient {
    LAVENDER_BLUE, // User's reference image!
    SUNSET_ORANGE,
    SUNNY_YELLOW,
    COOL_CYAN,
    MINT_GREEN,
    HOT_BERRY,
    CORAL_PEACH,
    CHERRY_RED,
    ELECTRIC_PURPLE,
    GOLDEN_GLOW,
    MIDNIGHT_DARK,
    ALIEN_LIME
}

enum class EyeStyle {
    BIG_CARTOON,   // User's reference image
    HEART_EYES,
    STAR_EYES,
    WINK_LEFT,
    SUNGLASSES,
    SQUINT_LAUGH,
    TEAR_EYES,
    SPIRAL_DIZZY,
    X_DEAD,
    ANGRY_SLANT,
    SLEEPY_CLOSED,
    PUPPY_CUTE
}

enum class MouthStyle {
    SHOCKED_O,      // User's reference image
    BIG_SMILE,
    WIDE_LAUGH,
    KISS_PUCKER,
    TONGUE_OUT,
    SCREAM_OVAL,
    SMIRK,
    WAVY_NERVOUS,
    SAD_FROWN,
    CAT_3
}

enum class ExtraFeature {
    HANDS_ON_CHEEKS, // User's reference image!
    BLUSH_CHEEKS,
    TEAR_DROPS,
    PARTY_HAT,
    ANGEL_HALO,
    DEVIL_HORNS,
    SWEAT_DROP,
    HEART_FLOAT,
    CAT_EARS,
    NONE
}

data class CustomEmojiDef(
    val id: String,
    val name: String,
    val category: String,
    val headGradient: HeadGradient,
    val eyeStyle: EyeStyle,
    val mouthStyle: MouthStyle,
    val extraFeature: ExtraFeature
)

object CustomSvgEmojis {

    // EXACTLY 100 COLORFUL CARTOON EMOJIS!
    val list: List<CustomEmojiDef> = listOf(
        // 1 - The exact user reference!
        CustomEmojiDef("shocked_hands", "Asombrado con Manos", "Caras", HeadGradient.LAVENDER_BLUE, EyeStyle.BIG_CARTOON, MouthStyle.SHOCKED_O, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("happy_classic", "Feliz Clásico", "Caras", HeadGradient.SUNNY_YELLOW, EyeStyle.BIG_CARTOON, MouthStyle.BIG_SMILE, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("super_blush", "Sonrisa Tímida", "Caras", HeadGradient.CORAL_PEACH, EyeStyle.BIG_CARTOON, MouthStyle.BIG_SMILE, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("heart_eyes_love", "Ojos de Corazón", "Amor", HeadGradient.HOT_BERRY, EyeStyle.HEART_EYES, MouthStyle.BIG_SMILE, ExtraFeature.HEART_FLOAT),
        CustomEmojiDef("heart_kiss", "Beso con Amor", "Amor", HeadGradient.CORAL_PEACH, EyeStyle.WINK_LEFT, MouthStyle.KISS_PUCKER, ExtraFeature.HEART_FLOAT),
        CustomEmojiDef("laughing_joy", "Muerto de Risa", "Caras", HeadGradient.SUNNY_YELLOW, EyeStyle.SQUINT_LAUGH, MouthStyle.WIDE_LAUGH, ExtraFeature.TEAR_DROPS),
        CustomEmojiDef("wink_cool", "Guiño Pícaro", "Caras", HeadGradient.SUNSET_ORANGE, EyeStyle.WINK_LEFT, MouthStyle.SMIRK, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("sunglasses_boss", "Modo Pro Gafas", "Cool", HeadGradient.COOL_CYAN, EyeStyle.SUNGLASSES, MouthStyle.SMIRK, ExtraFeature.NONE),
        CustomEmojiDef("crying_river", "Llorando a Mares", "Drama", HeadGradient.COOL_CYAN, EyeStyle.TEAR_EYES, MouthStyle.SAD_FROWN, ExtraFeature.TEAR_DROPS),
        CustomEmojiDef("angel_blessed", "Angelito Santo", "Fantasía", HeadGradient.GOLDEN_GLOW, EyeStyle.SLEEPY_CLOSED, MouthStyle.BIG_SMILE, ExtraFeature.ANGEL_HALO),
        CustomEmojiDef("devil_mischief", "Diablillo Pillo", "Fantasía", HeadGradient.CHERRY_RED, EyeStyle.ANGRY_SLANT, MouthStyle.SMIRK, ExtraFeature.DEVIL_HORNS),
        CustomEmojiDef("party_time", "Modo Fiesta", "Fiesta", HeadGradient.SUNSET_ORANGE, EyeStyle.BIG_CARTOON, MouthStyle.WIDE_LAUGH, ExtraFeature.PARTY_HAT),
        CustomEmojiDef("screaming_fear", "Grito de Pánico", "Drama", HeadGradient.COOL_CYAN, EyeStyle.BIG_CARTOON, MouthStyle.SCREAM_OVAL, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("mind_blown", "Cerebro Explotado", "Sorpresa", HeadGradient.SUNSET_ORANGE, EyeStyle.BIG_CARTOON, MouthStyle.SCREAM_OVAL, ExtraFeature.SWEAT_DROP),
        CustomEmojiDef("sleepy_night", "Zzz Con Sueño", "Caras", HeadGradient.LAVENDER_BLUE, EyeStyle.SLEEPY_CLOSED, MouthStyle.SHOCKED_O, ExtraFeature.NONE),
        CustomEmojiDef("thinking_hmm", "Pensativo Mmm", "Caras", HeadGradient.MINT_GREEN, EyeStyle.WINK_LEFT, MouthStyle.WAVY_NERVOUS, ExtraFeature.NONE),
        CustomEmojiDef("angry_furious", "Super Furia", "Enojo", HeadGradient.CHERRY_RED, EyeStyle.ANGRY_SLANT, MouthStyle.SAD_FROWN, ExtraFeature.NONE),
        CustomEmojiDef("sweat_relief", "Uff Qué Alivio", "Caras", HeadGradient.COOL_CYAN, EyeStyle.BIG_CARTOON, MouthStyle.BIG_SMILE, ExtraFeature.SWEAT_DROP),
        CustomEmojiDef("dizzy_confused", "Mareado y Loco", "Sorpresa", HeadGradient.MINT_GREEN, EyeStyle.SPIRAL_DIZZY, MouthStyle.WAVY_NERVOUS, ExtraFeature.NONE),
        CustomEmojiDef("star_superstar", "Estrella Brillante", "Cool", HeadGradient.GOLDEN_GLOW, EyeStyle.STAR_EYES, MouthStyle.WIDE_LAUGH, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("tongue_playful", "Sacando la Lengua", "Caras", HeadGradient.SUNNY_YELLOW, EyeStyle.WINK_LEFT, MouthStyle.TONGUE_OUT, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("alien_buddy", "Alien Amigable", "Fantasía", HeadGradient.ALIEN_LIME, EyeStyle.BIG_CARTOON, MouthStyle.BIG_SMILE, ExtraFeature.NONE),
        CustomEmojiDef("cat_kawaii", "Gatito Kawaii", "Fantasía", HeadGradient.HOT_BERRY, EyeStyle.PUPPY_CUTE, MouthStyle.CAT_3, ExtraFeature.CAT_EARS),
        CustomEmojiDef("shocked_yellow", "Sorpresa Total", "Sorpresa", HeadGradient.SUNNY_YELLOW, EyeStyle.BIG_CARTOON, MouthStyle.SHOCKED_O, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("surprised_pink", "Oh Rosa Pastel", "Sorpresa", HeadGradient.HOT_BERRY, EyeStyle.BIG_CARTOON, MouthStyle.SHOCKED_O, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("cool_mint", "Fresco Menta", "Cool", HeadGradient.MINT_GREEN, EyeStyle.SUNGLASSES, MouthStyle.SMIRK, ExtraFeature.NONE),
        CustomEmojiDef("sleepy_drool", "Babeando Sueño", "Caras", HeadGradient.LAVENDER_BLUE, EyeStyle.SLEEPY_CLOSED, MouthStyle.TONGUE_OUT, ExtraFeature.NONE),
        CustomEmojiDef("kiss_blush", "Beso Sonrojado", "Amor", HeadGradient.CORAL_PEACH, EyeStyle.SLEEPY_CLOSED, MouthStyle.KISS_PUCKER, ExtraFeature.HEART_FLOAT),
        CustomEmojiDef("angel_mint", "Ángel Celestial", "Fantasía", HeadGradient.MINT_GREEN, EyeStyle.SLEEPY_CLOSED, MouthStyle.BIG_SMILE, ExtraFeature.ANGEL_HALO),
        CustomEmojiDef("devil_violet", "Diablillo Morado", "Fantasía", HeadGradient.ELECTRIC_PURPLE, EyeStyle.ANGRY_SLANT, MouthStyle.SMIRK, ExtraFeature.DEVIL_HORNS),
        CustomEmojiDef("party_pop", "Celebración Loca", "Fiesta", HeadGradient.HOT_BERRY, EyeStyle.WINK_LEFT, MouthStyle.WIDE_LAUGH, ExtraFeature.PARTY_HAT),
        CustomEmojiDef("heart_sparkle", "Enamorado Dulce", "Amor", HeadGradient.LAVENDER_BLUE, EyeStyle.HEART_EYES, MouthStyle.BIG_SMILE, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("cool_lavender", "Lavanda Chill", "Cool", HeadGradient.LAVENDER_BLUE, EyeStyle.SUNGLASSES, MouthStyle.BIG_SMILE, ExtraFeature.NONE),
        CustomEmojiDef("screaming_yellow", "Alarido Sorpresa", "Drama", HeadGradient.SUNNY_YELLOW, EyeStyle.BIG_CARTOON, MouthStyle.SCREAM_OVAL, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("confused_wavy", "¿Qué Pasó Aquí?", "Sorpresa", HeadGradient.SUNNY_YELLOW, EyeStyle.BIG_CARTOON, MouthStyle.WAVY_NERVOUS, ExtraFeature.SWEAT_DROP),
        CustomEmojiDef("cat_wink", "Gatito Pícaro", "Fantasía", HeadGradient.SUNSET_ORANGE, EyeStyle.WINK_LEFT, MouthStyle.CAT_3, ExtraFeature.CAT_EARS),
        CustomEmojiDef("alien_shocked", "Alien Atónito", "Fantasía", HeadGradient.ALIEN_LIME, EyeStyle.BIG_CARTOON, MouthStyle.SHOCKED_O, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("star_pink", "Estrella Glamour", "Cool", HeadGradient.HOT_BERRY, EyeStyle.STAR_EYES, MouthStyle.BIG_SMILE, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("angry_pout", "Berrinche Furioso", "Enojo", HeadGradient.SUNSET_ORANGE, EyeStyle.ANGRY_SLANT, MouthStyle.SAD_FROWN, ExtraFeature.NONE),
        CustomEmojiDef("hands_peach", "Asombro Durazno", "Sorpresa", HeadGradient.CORAL_PEACH, EyeStyle.BIG_CARTOON, MouthStyle.SHOCKED_O, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("hands_mint", "Asombro Menta", "Sorpresa", HeadGradient.MINT_GREEN, EyeStyle.BIG_CARTOON, MouthStyle.SHOCKED_O, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("dizzy_pink", "Vueltas y Vueltas", "Sorpresa", HeadGradient.HOT_BERRY, EyeStyle.SPIRAL_DIZZY, MouthStyle.WAVY_NERVOUS, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("party_neon", "Rave Nocturno", "Fiesta", HeadGradient.ALIEN_LIME, EyeStyle.STAR_EYES, MouthStyle.WIDE_LAUGH, ExtraFeature.PARTY_HAT),
        CustomEmojiDef("angel_yellow", "Ángel Luminoso", "Fantasía", HeadGradient.SUNNY_YELLOW, EyeStyle.BIG_CARTOON, MouthStyle.BIG_SMILE, ExtraFeature.ANGEL_HALO),
        CustomEmojiDef("devil_red", "Maldad Roja", "Fantasía", HeadGradient.CHERRY_RED, EyeStyle.ANGRY_SLANT, MouthStyle.WIDE_LAUGH, ExtraFeature.DEVIL_HORNS),
        CustomEmojiDef("crying_lavender", "Puchero Dulce", "Drama", HeadGradient.LAVENDER_BLUE, EyeStyle.TEAR_EYES, MouthStyle.SAD_FROWN, ExtraFeature.TEAR_DROPS),
        CustomEmojiDef("sunglasses_cyan", "Surfista Relajado", "Cool", HeadGradient.COOL_CYAN, EyeStyle.SUNGLASSES, MouthStyle.BIG_SMILE, ExtraFeature.NONE),
        CustomEmojiDef("dead_tired", "Muerto de Cansancio", "Caras", HeadGradient.MIDNIGHT_DARK, EyeStyle.X_DEAD, MouthStyle.WAVY_NERVOUS, ExtraFeature.SWEAT_DROP),
        CustomEmojiDef("sleepy_cloud", "En las Nubes", "Caras", HeadGradient.LAVENDER_BLUE, EyeStyle.SLEEPY_CLOSED, MouthStyle.BIG_SMILE, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("thinking_yellow", "Filosofando", "Caras", HeadGradient.SUNNY_YELLOW, EyeStyle.PUPPY_CUTE, MouthStyle.WAVY_NERVOUS, ExtraFeature.NONE),
        CustomEmojiDef("cat_hearts", "Gatito Enamorado", "Amor", HeadGradient.CORAL_PEACH, EyeStyle.HEART_EYES, MouthStyle.CAT_3, ExtraFeature.CAT_EARS),
        CustomEmojiDef("alien_cool", "Marciano con Estilo", "Cool", HeadGradient.ALIEN_LIME, EyeStyle.SUNGLASSES, MouthStyle.SMIRK, ExtraFeature.NONE),
        CustomEmojiDef("happy_peach", "Dulce Felicidad", "Caras", HeadGradient.CORAL_PEACH, EyeStyle.BIG_CARTOON, MouthStyle.BIG_SMILE, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("shocked_coral", "Quedé Helado", "Sorpresa", HeadGradient.CORAL_PEACH, EyeStyle.BIG_CARTOON, MouthStyle.SCREAM_OVAL, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("star_cyan", "Cosmos y Magia", "Cool", HeadGradient.COOL_CYAN, EyeStyle.STAR_EYES, MouthStyle.BIG_SMILE, ExtraFeature.NONE),
        CustomEmojiDef("angry_lavender", "Enojito Adorable", "Enojo", HeadGradient.LAVENDER_BLUE, EyeStyle.ANGRY_SLANT, MouthStyle.SAD_FROWN, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("tongue_mint", "Lengua y Menta", "Caras", HeadGradient.MINT_GREEN, EyeStyle.WINK_LEFT, MouthStyle.TONGUE_OUT, ExtraFeature.NONE),
        CustomEmojiDef("kiss_love_red", "Beso Apasionado", "Amor", HeadGradient.CHERRY_RED, EyeStyle.SLEEPY_CLOSED, MouthStyle.KISS_PUCKER, ExtraFeature.HEART_FLOAT),
        CustomEmojiDef("screaming_cyan", "Grito Frío", "Drama", HeadGradient.COOL_CYAN, EyeStyle.BIG_CARTOON, MouthStyle.SCREAM_OVAL, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("confused_yellow", "Duda Existencial", "Caras", HeadGradient.SUNNY_YELLOW, EyeStyle.PUPPY_CUTE, MouthStyle.WAVY_NERVOUS, ExtraFeature.SWEAT_DROP),
        CustomEmojiDef("party_coral", "Fiesta Tropical", "Fiesta", HeadGradient.CORAL_PEACH, EyeStyle.BIG_CARTOON, MouthStyle.WIDE_LAUGH, ExtraFeature.PARTY_HAT),
        CustomEmojiDef("angel_cyan", "Sereno y Puro", "Fantasía", HeadGradient.COOL_CYAN, EyeStyle.SLEEPY_CLOSED, MouthStyle.BIG_SMILE, ExtraFeature.ANGEL_HALO),
        CustomEmojiDef("devil_cyan", "Duendecillo Frío", "Fantasía", HeadGradient.COOL_CYAN, EyeStyle.ANGRY_SLANT, MouthStyle.SMIRK, ExtraFeature.DEVIL_HORNS),
        CustomEmojiDef("hands_violet", "Manos en Mejillas", "Sorpresa", HeadGradient.ELECTRIC_PURPLE, EyeStyle.BIG_CARTOON, MouthStyle.SHOCKED_O, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("crying_puddle", "Inundación", "Drama", HeadGradient.COOL_CYAN, EyeStyle.TEAR_EYES, MouthStyle.WIDE_LAUGH, ExtraFeature.TEAR_DROPS),
        CustomEmojiDef("sunglasses_gold", "Magnate Dorado", "Cool", HeadGradient.GOLDEN_GLOW, EyeStyle.SUNGLASSES, MouthStyle.SMIRK, ExtraFeature.NONE),
        CustomEmojiDef("laughing_pink", "Risa Incontrolable", "Caras", HeadGradient.HOT_BERRY, EyeStyle.SQUINT_LAUGH, MouthStyle.WIDE_LAUGH, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("sleepy_lavender", "Noche Estrellada", "Caras", HeadGradient.LAVENDER_BLUE, EyeStyle.SLEEPY_CLOSED, MouthStyle.SHOCKED_O, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("thinking_mint", "Pensando Ideas", "Caras", HeadGradient.MINT_GREEN, EyeStyle.BIG_CARTOON, MouthStyle.WAVY_NERVOUS, ExtraFeature.SWEAT_DROP),
        CustomEmojiDef("cat_surprised", "Michi Wow", "Fantasía", HeadGradient.SUNNY_YELLOW, EyeStyle.BIG_CARTOON, MouthStyle.CAT_3, ExtraFeature.CAT_EARS),
        CustomEmojiDef("alien_party", "Fiesta Galáctica", "Fiesta", HeadGradient.ALIEN_LIME, EyeStyle.WINK_LEFT, MouthStyle.WIDE_LAUGH, ExtraFeature.PARTY_HAT),
        CustomEmojiDef("star_gold", "Premio Mayor", "Cool", HeadGradient.GOLDEN_GLOW, EyeStyle.STAR_EYES, MouthStyle.WIDE_LAUGH, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("angry_dark", "Sombra Enojada", "Enojo", HeadGradient.MIDNIGHT_DARK, EyeStyle.ANGRY_SLANT, MouthStyle.SAD_FROWN, ExtraFeature.NONE),
        CustomEmojiDef("super_happy_cyan", "Energía Máxima", "Caras", HeadGradient.COOL_CYAN, EyeStyle.BIG_CARTOON, MouthStyle.WIDE_LAUGH, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("sweat_embarrassed", "Tierra Trágame", "Drama", HeadGradient.CORAL_PEACH, EyeStyle.SQUINT_LAUGH, MouthStyle.WAVY_NERVOUS, ExtraFeature.SWEAT_DROP),
        CustomEmojiDef("smirk_lavender", "Mirada Cómplice", "Cool", HeadGradient.LAVENDER_BLUE, EyeStyle.WINK_LEFT, MouthStyle.SMIRK, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("heart_eyes_mint", "Flecha de Cupido", "Amor", HeadGradient.MINT_GREEN, EyeStyle.HEART_EYES, MouthStyle.BIG_SMILE, ExtraFeature.HEART_FLOAT),
        CustomEmojiDef("hands_gold", "Qué Hice Dios Mío", "Sorpresa", HeadGradient.GOLDEN_GLOW, EyeStyle.BIG_CARTOON, MouthStyle.SHOCKED_O, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("dizzy_cyan", "Remolino Mental", "Sorpresa", HeadGradient.COOL_CYAN, EyeStyle.SPIRAL_DIZZY, MouthStyle.WAVY_NERVOUS, ExtraFeature.NONE),
        CustomEmojiDef("party_lavender", "Brindis con Amigos", "Fiesta", HeadGradient.LAVENDER_BLUE, EyeStyle.BIG_CARTOON, MouthStyle.WIDE_LAUGH, ExtraFeature.PARTY_HAT),
        CustomEmojiDef("angel_pink", "Alas de Seda", "Fantasía", HeadGradient.HOT_BERRY, EyeStyle.SLEEPY_CLOSED, MouthStyle.BIG_SMILE, ExtraFeature.ANGEL_HALO),
        CustomEmojiDef("devil_gold", "Tentación Dorada", "Fantasía", HeadGradient.GOLDEN_GLOW, EyeStyle.ANGRY_SLANT, MouthStyle.SMIRK, ExtraFeature.DEVIL_HORNS),
        CustomEmojiDef("crying_coral", "Llanto Sentido", "Drama", HeadGradient.CORAL_PEACH, EyeStyle.TEAR_EYES, MouthStyle.SAD_FROWN, ExtraFeature.TEAR_DROPS),
        CustomEmojiDef("sunglasses_peach", "Atardecer Playero", "Cool", HeadGradient.CORAL_PEACH, EyeStyle.SUNGLASSES, MouthStyle.SMIRK, ExtraFeature.NONE),
        CustomEmojiDef("laughing_mint", "Carcajada Fresca", "Caras", HeadGradient.MINT_GREEN, EyeStyle.SQUINT_LAUGH, MouthStyle.WIDE_LAUGH, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("mind_blown_gold", "Impacto Supremo", "Sorpresa", HeadGradient.GOLDEN_GLOW, EyeStyle.BIG_CARTOON, MouthStyle.SCREAM_OVAL, ExtraFeature.SWEAT_DROP),
        CustomEmojiDef("cat_sleepy", "Michi Durmiente", "Fantasía", HeadGradient.LAVENDER_BLUE, EyeStyle.SLEEPY_CLOSED, MouthStyle.CAT_3, ExtraFeature.CAT_EARS),
        CustomEmojiDef("cosmic_legend", "Emoji Legendario", "Fantasía", HeadGradient.ELECTRIC_PURPLE, EyeStyle.STAR_EYES, MouthStyle.WIDE_LAUGH, ExtraFeature.ANGEL_HALO),
        CustomEmojiDef("wink_lavender", "Guiño Mágico", "Caras", HeadGradient.LAVENDER_BLUE, EyeStyle.WINK_LEFT, MouthStyle.BIG_SMILE, ExtraFeature.BLUSH_CHEEKS),
        CustomEmojiDef("shocked_dark", "Pánico Oscuro", "Drama", HeadGradient.MIDNIGHT_DARK, EyeStyle.BIG_CARTOON, MouthStyle.SHOCKED_O, ExtraFeature.HANDS_ON_CHEEKS),
        CustomEmojiDef("kiss_mint", "Beso Refrescante", "Amor", HeadGradient.MINT_GREEN, EyeStyle.WINK_LEFT, MouthStyle.KISS_PUCKER, ExtraFeature.HEART_FLOAT),
        CustomEmojiDef("party_gold", "Fiesta de Gala", "Fiesta", HeadGradient.GOLDEN_GLOW, EyeStyle.STAR_EYES, MouthStyle.BIG_SMILE, ExtraFeature.PARTY_HAT),
        CustomEmojiDef("devil_mint", "Diablillo Menta", "Fantasía", HeadGradient.MINT_GREEN, EyeStyle.ANGRY_SLANT, MouthStyle.SMIRK, ExtraFeature.DEVIL_HORNS),
        CustomEmojiDef("cat_playful", "Michi Juguetón", "Fantasía", HeadGradient.CORAL_PEACH, EyeStyle.WINK_LEFT, MouthStyle.CAT_3, ExtraFeature.CAT_EARS),
        CustomEmojiDef("sunglasses_red", "Rockstar Total", "Cool", HeadGradient.CHERRY_RED, EyeStyle.SUNGLASSES, MouthStyle.SMIRK, ExtraFeature.NONE),
        CustomEmojiDef("sweat_purple", "Nervios Morados", "Drama", HeadGradient.ELECTRIC_PURPLE, EyeStyle.BIG_CARTOON, MouthStyle.WAVY_NERVOUS, ExtraFeature.SWEAT_DROP),
        CustomEmojiDef("angel_coral", "Ángel Cálido", "Fantasía", HeadGradient.CORAL_PEACH, EyeStyle.BIG_CARTOON, MouthStyle.BIG_SMILE, ExtraFeature.ANGEL_HALO),
        CustomEmojiDef("alien_wink", "Extraterrestre Guiño", "Fantasía", HeadGradient.ALIEN_LIME, EyeStyle.WINK_LEFT, MouthStyle.BIG_SMILE, ExtraFeature.NONE),
        CustomEmojiDef("dead_pink", "K.O. Rosa", "Drama", HeadGradient.HOT_BERRY, EyeStyle.X_DEAD, MouthStyle.WAVY_NERVOUS, ExtraFeature.NONE),
        CustomEmojiDef("master_star", "Gran Maestro", "Cool", HeadGradient.SUNSET_ORANGE, EyeStyle.STAR_EYES, MouthStyle.WIDE_LAUGH, ExtraFeature.ANGEL_HALO)
    )

    private val map: Map<String, CustomEmojiDef> = list.associateBy { it.id }

    fun getEmoji(id: String): CustomEmojiDef? = map[id]

    fun isSvgEmoji(text: String): Boolean {
        return text.startsWith("[custom_svg_emoji:") && text.endsWith("]")
    }

    fun parseEmojiId(text: String): String {
        return text.removePrefix("[custom_svg_emoji:").removeSuffix("]")
    }

    fun formatMessage(id: String): String {
        return "[custom_svg_emoji:$id]"
    }
}

@Composable
fun CustomSvgEmojiGraphic(
    emoji: CustomEmojiDef,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // 1. Extras behind head
        if (emoji.extraFeature == ExtraFeature.DEVIL_HORNS) {
            drawDevilHorns(w, h)
        }
        if (emoji.extraFeature == ExtraFeature.ANGEL_HALO) {
            drawAngelHalo(w, h)
        }
        if (emoji.extraFeature == ExtraFeature.PARTY_HAT) {
            drawPartyHat(w, h)
        }
        if (emoji.extraFeature == ExtraFeature.CAT_EARS) {
            drawCatEars(w, h)
        }

        // 2. Head Shape & Gradient
        val headColors = when (emoji.headGradient) {
            HeadGradient.LAVENDER_BLUE -> listOf(Color(0xFF8EC5FC), Color(0xFFA1C4FD), Color(0xFF9575CD))
            HeadGradient.SUNSET_ORANGE -> listOf(Color(0xFFFFD166), Color(0xFFFF9E00), Color(0xFFFF5400))
            HeadGradient.SUNNY_YELLOW -> listOf(Color(0xFFFFF176), Color(0xFFFFD54F), Color(0xFFFFB300))
            HeadGradient.COOL_CYAN -> listOf(Color(0xFF80DEEA), Color(0xFF4DD0E1), Color(0xFF00ACC1))
            HeadGradient.MINT_GREEN -> listOf(Color(0xFFA7F3D0), Color(0xFF34D399), Color(0xFF059669))
            HeadGradient.HOT_BERRY -> listOf(Color(0xFFFBCFE8), Color(0xFFF472B6), Color(0xFFDB2777))
            HeadGradient.CORAL_PEACH -> listOf(Color(0xFFFFE0B2), Color(0xFFFFAB91), Color(0xFFFF7043))
            HeadGradient.CHERRY_RED -> listOf(Color(0xFFFF8A80), Color(0xFFFF5252), Color(0xFFD50000))
            HeadGradient.ELECTRIC_PURPLE -> listOf(Color(0xFFE1BEE7), Color(0xFFBA68C8), Color(0xFF8E24AA))
            HeadGradient.GOLDEN_GLOW -> listOf(Color(0xFFFFF9C4), Color(0xFFFFE082), Color(0xFFFFC107))
            HeadGradient.MIDNIGHT_DARK -> listOf(Color(0xFF90A4AE), Color(0xFF607D8B), Color(0xFF37474F))
            HeadGradient.ALIEN_LIME -> listOf(Color(0xFFCCFF90), Color(0xFF76FF03), Color(0xFF64DD17))
        }

        val headBrush = Brush.radialGradient(
            colors = headColors,
            center = Offset(w * 0.4f, h * 0.35f),
            radius = w * 0.7f
        )
        // Cute slightly rounded oval head
        drawRoundRect(
            brush = headBrush,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.84f, h * 0.84f),
            cornerRadius = CornerRadius(w * 0.42f, h * 0.42f)
        )

        // 3. Eyebrows
        drawEyebrows(w, h, emoji.eyeStyle)

        // 4. Eyes
        drawEyes(w, h, emoji.eyeStyle)

        // 5. Mouth
        drawMouth(w, h, emoji.mouthStyle)

        // 6. Extras in front (Hands on cheeks like user's image, blush, tears, hearts, sweat)
        when (emoji.extraFeature) {
            ExtraFeature.HANDS_ON_CHEEKS -> drawHandsOnCheeks(w, h)
            ExtraFeature.BLUSH_CHEEKS -> drawBlush(w, h)
            ExtraFeature.TEAR_DROPS -> drawTearDrops(w, h)
            ExtraFeature.SWEAT_DROP -> drawSweatDrop(w, h)
            ExtraFeature.HEART_FLOAT -> drawFloatingHeart(w, h)
            else -> {}
        }
    }
}

// === DRAWING HELPERS ===

private fun DrawScope.drawHandsOnCheeks(w: Float, h: Float) {
    // Golden-yellow puffy hands hugging both cheeks, EXACTLY like custom-emoji.svg!
    val handBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFEE58), Color(0xFFFFB300), Color(0xFFFF8F00)),
        start = Offset(0f, h * 0.5f),
        end = Offset(0f, h * 0.9f)
    )

    // Left hand
    val leftPath = Path().apply {
        moveTo(w * 0.02f, h * 0.58f)
        cubicTo(w * 0.02f, h * 0.48f, w * 0.26f, h * 0.52f, w * 0.28f, h * 0.68f)
        cubicTo(w * 0.30f, h * 0.82f, w * 0.12f, h * 0.92f, w * 0.04f, h * 0.86f)
        close()
    }
    drawPath(leftPath, brush = handBrush)

    // Right hand
    val rightPath = Path().apply {
        moveTo(w * 0.98f, h * 0.58f)
        cubicTo(w * 0.98f, h * 0.48f, w * 0.74f, h * 0.52f, w * 0.72f, h * 0.68f)
        cubicTo(w * 0.70f, h * 0.82f, w * 0.88f, h * 0.92f, w * 0.96f, h * 0.86f)
        close()
    }
    drawPath(rightPath, brush = handBrush)
}

private fun DrawScope.drawEyes(w: Float, h: Float, style: EyeStyle) {
    when (style) {
        EyeStyle.BIG_CARTOON, EyeStyle.PUPPY_CUTE -> {
            // Big cartoon eyes: large white sclera, dark pupil, white specular dot (User's reference)
            val scleraRadius = w * 0.16f
            val pupilRadius = w * 0.10f
            val leftEyeCenter = Offset(w * 0.33f, h * 0.38f)
            val rightEyeCenter = Offset(w * 0.67f, h * 0.38f)

            // Sclera
            drawCircle(Color.White, radius = scleraRadius, center = leftEyeCenter)
            drawCircle(Color.White, radius = scleraRadius, center = rightEyeCenter)

            // Pupils (dark plum/black)
            drawCircle(Color(0xFF2A1B28), radius = pupilRadius, center = Offset(leftEyeCenter.x + w * 0.02f, leftEyeCenter.y + h * 0.02f))
            drawCircle(Color(0xFF2A1B28), radius = pupilRadius, center = Offset(rightEyeCenter.x - w * 0.02f, rightEyeCenter.y + h * 0.02f))

            // White catchlight reflections
            drawCircle(Color.White, radius = w * 0.035f, center = Offset(leftEyeCenter.x, leftEyeCenter.y - h * 0.03f))
            drawCircle(Color.White, radius = w * 0.035f, center = Offset(rightEyeCenter.x - w * 0.04f, rightEyeCenter.y - h * 0.03f))
        }
        EyeStyle.HEART_EYES -> {
            drawHeart(w * 0.33f, h * 0.38f, w * 0.16f, Color(0xFFFF1744))
            drawHeart(w * 0.67f, h * 0.38f, w * 0.16f, Color(0xFFFF1744))
        }
        EyeStyle.STAR_EYES -> {
            drawStar(w * 0.33f, h * 0.38f, w * 0.16f, Color(0xFFFFD600))
            drawStar(w * 0.67f, h * 0.38f, w * 0.16f, Color(0xFFFFD600))
        }
        EyeStyle.WINK_LEFT -> {
            // Left eye wink
            val winkPath = Path().apply {
                moveTo(w * 0.22f, h * 0.38f)
                quadraticBezierTo(w * 0.33f, h * 0.32f, w * 0.44f, h * 0.38f)
            }
            drawPath(winkPath, color = Color(0xFF2A1B28), style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))

            // Right eye big cartoon
            drawCircle(Color.White, radius = w * 0.16f, center = Offset(w * 0.67f, h * 0.38f))
            drawCircle(Color(0xFF2A1B28), radius = w * 0.10f, center = Offset(w * 0.67f, h * 0.38f))
            drawCircle(Color.White, radius = w * 0.035f, center = Offset(w * 0.64f, h * 0.35f))
        }
        EyeStyle.SUNGLASSES -> {
            val glassBrush = Brush.linearGradient(listOf(Color(0xFF1E1E1E), Color(0xFF000000)))
            // Left lens
            drawRoundRect(glassBrush, topLeft = Offset(w * 0.16f, h * 0.30f), size = Size(w * 0.32f, h * 0.22f), cornerRadius = CornerRadius(w * 0.08f))
            // Right lens
            drawRoundRect(glassBrush, topLeft = Offset(w * 0.52f, h * 0.30f), size = Size(w * 0.32f, h * 0.22f), cornerRadius = CornerRadius(w * 0.08f))
            // Bridge
            drawLine(Color(0xFF1E1E1E), start = Offset(w * 0.46f, h * 0.36f), end = Offset(w * 0.54f, h * 0.36f), strokeWidth = w * 0.04f)
            // Glare reflection lines
            drawLine(Color.White.copy(alpha = 0.6f), start = Offset(w * 0.22f, h * 0.33f), end = Offset(w * 0.38f, h * 0.46f), strokeWidth = w * 0.02f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.6f), start = Offset(w * 0.58f, h * 0.33f), end = Offset(w * 0.74f, h * 0.46f), strokeWidth = w * 0.02f, cap = StrokeCap.Round)
        }
        EyeStyle.SQUINT_LAUGH -> {
            val left = Path().apply {
                moveTo(w * 0.20f, h * 0.40f)
                lineTo(w * 0.32f, h * 0.34f)
                lineTo(w * 0.42f, h * 0.40f)
            }
            val right = Path().apply {
                moveTo(w * 0.58f, h * 0.40f)
                lineTo(w * 0.68f, h * 0.34f)
                lineTo(w * 0.80f, h * 0.40f)
            }
            drawPath(left, color = Color(0xFF2A1B28), style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))
            drawPath(right, color = Color(0xFF2A1B28), style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))
        }
        EyeStyle.TEAR_EYES -> {
            drawCircle(Color.White, radius = w * 0.16f, center = Offset(w * 0.33f, h * 0.38f))
            drawCircle(Color.White, radius = w * 0.16f, center = Offset(w * 0.67f, h * 0.38f))
            drawCircle(Color(0xFF2A1B28), radius = w * 0.10f, center = Offset(w * 0.33f, h * 0.38f))
            drawCircle(Color(0xFF2A1B28), radius = w * 0.10f, center = Offset(w * 0.67f, h * 0.38f))
            // Water layer
            drawCircle(Color(0xFF40C4FF).copy(alpha = 0.7f), radius = w * 0.07f, center = Offset(w * 0.33f, h * 0.44f))
            drawCircle(Color(0xFF40C4FF).copy(alpha = 0.7f), radius = w * 0.07f, center = Offset(w * 0.67f, h * 0.44f))
        }
        EyeStyle.SPIRAL_DIZZY -> {
            drawCircle(Color(0xFF2A1B28), radius = w * 0.12f, center = Offset(w * 0.33f, h * 0.38f), style = Stroke(width = w * 0.035f))
            drawCircle(Color(0xFF2A1B28), radius = w * 0.05f, center = Offset(w * 0.33f, h * 0.38f), style = Stroke(width = w * 0.035f))
            drawCircle(Color(0xFF2A1B28), radius = w * 0.12f, center = Offset(w * 0.67f, h * 0.38f), style = Stroke(width = w * 0.035f))
            drawCircle(Color(0xFF2A1B28), radius = w * 0.05f, center = Offset(w * 0.67f, h * 0.38f), style = Stroke(width = w * 0.035f))
        }
        EyeStyle.X_DEAD -> {
            val stroke = w * 0.045f
            drawLine(Color(0xFF2A1B28), Offset(w * 0.24f, h * 0.32f), Offset(w * 0.40f, h * 0.44f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(Color(0xFF2A1B28), Offset(w * 0.40f, h * 0.32f), Offset(w * 0.24f, h * 0.44f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(Color(0xFF2A1B28), Offset(w * 0.60f, h * 0.32f), Offset(w * 0.76f, h * 0.44f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(Color(0xFF2A1B28), Offset(w * 0.76f, h * 0.32f), Offset(w * 0.60f, h * 0.44f), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        EyeStyle.ANGRY_SLANT -> {
            drawCircle(Color.White, radius = w * 0.15f, center = Offset(w * 0.33f, h * 0.38f))
            drawCircle(Color.White, radius = w * 0.15f, center = Offset(w * 0.67f, h * 0.38f))
            drawCircle(Color(0xFF2A1B28), radius = w * 0.09f, center = Offset(w * 0.35f, h * 0.40f))
            drawCircle(Color(0xFF2A1B28), radius = w * 0.09f, center = Offset(w * 0.65f, h * 0.40f))
            // Slanted brow line cut
            drawLine(Color(0xFF2A1B28), Offset(w * 0.18f, h * 0.28f), Offset(w * 0.42f, h * 0.36f), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
            drawLine(Color(0xFF2A1B28), Offset(w * 0.82f, h * 0.28f), Offset(w * 0.58f, h * 0.36f), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
        }
        EyeStyle.SLEEPY_CLOSED -> {
            val left = Path().apply {
                moveTo(w * 0.22f, h * 0.38f)
                quadraticBezierTo(w * 0.33f, h * 0.44f, w * 0.44f, h * 0.38f)
            }
            val right = Path().apply {
                moveTo(w * 0.56f, h * 0.38f)
                quadraticBezierTo(w * 0.67f, h * 0.44f, w * 0.78f, h * 0.38f)
            }
            drawPath(left, color = Color(0xFF2A1B28), style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))
            drawPath(right, color = Color(0xFF2A1B28), style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawEyebrows(w: Float, h: Float, style: EyeStyle) {
    if (style == EyeStyle.SUNGLASSES || style == EyeStyle.ANGRY_SLANT) return
    val browColor = Color(0xFF372733)
    val stroke = w * 0.03f

    // Arched cute brows
    val left = Path().apply {
        moveTo(w * 0.22f, h * 0.22f)
        quadraticBezierTo(w * 0.33f, h * 0.16f, w * 0.44f, h * 0.23f)
    }
    val right = Path().apply {
        moveTo(w * 0.56f, h * 0.23f)
        quadraticBezierTo(w * 0.67f, h * 0.16f, w * 0.78f, h * 0.22f)
    }
    drawPath(left, color = browColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
    drawPath(right, color = browColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
}

private fun DrawScope.drawMouth(w: Float, h: Float, style: MouthStyle) {
    when (style) {
        MouthStyle.SHOCKED_O -> {
            // Deep magenta/purple small cute oval mouth (EXACTLY like custom-emoji.svg!)
            drawOval(
                color = Color(0xFF880E4F),
                topLeft = Offset(w * 0.43f, h * 0.66f),
                size = Size(w * 0.14f, h * 0.16f)
            )
        }
        MouthStyle.BIG_SMILE -> {
            val path = Path().apply {
                moveTo(w * 0.32f, h * 0.64f)
                quadraticBezierTo(w * 0.50f, h * 0.86f, w * 0.68f, h * 0.64f)
                close()
            }
            drawPath(path, color = Color(0xFF5D0022))
            // Tongue
            val tongue = Path().apply {
                moveTo(w * 0.40f, h * 0.76f)
                quadraticBezierTo(w * 0.50f, h * 0.86f, w * 0.60f, h * 0.76f)
            }
            drawPath(tongue, color = Color(0xFFFF5252))
        }
        MouthStyle.WIDE_LAUGH -> {
            val path = Path().apply {
                moveTo(w * 0.26f, h * 0.60f)
                quadraticBezierTo(w * 0.50f, h * 0.88f, w * 0.74f, h * 0.60f)
                close()
            }
            drawPath(path, color = Color(0xFF4A001A))
            // White teeth bar
            val teeth = Path().apply {
                moveTo(w * 0.30f, h * 0.61f)
                lineTo(w * 0.70f, h * 0.61f)
                lineTo(w * 0.66f, h * 0.67f)
                lineTo(w * 0.34f, h * 0.67f)
                close()
            }
            drawPath(teeth, color = Color.White)
        }
        MouthStyle.KISS_PUCKER -> {
            drawCircle(Color(0xFFD81B60), radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.72f))
            drawCircle(Color(0xFF880E4F), radius = w * 0.03f, center = Offset(w * 0.50f, h * 0.72f))
        }
        MouthStyle.TONGUE_OUT -> {
            // Smile line
            val smile = Path().apply {
                moveTo(w * 0.32f, h * 0.64f)
                quadraticBezierTo(w * 0.50f, h * 0.78f, w * 0.68f, h * 0.64f)
            }
            drawPath(smile, color = Color(0xFF2A1B28), style = Stroke(width = w * 0.04f, cap = StrokeCap.Round))
            // Tongue shape
            drawRoundRect(
                Color(0xFFFF4081),
                topLeft = Offset(w * 0.44f, h * 0.68f),
                size = Size(w * 0.14f, h * 0.16f),
                cornerRadius = CornerRadius(w * 0.07f)
            )
        }
        MouthStyle.SCREAM_OVAL -> {
            drawOval(Color(0xFF2A1B28), topLeft = Offset(w * 0.38f, h * 0.60f), size = Size(w * 0.24f, h * 0.26f))
        }
        MouthStyle.SMIRK -> {
            val path = Path().apply {
                moveTo(w * 0.36f, h * 0.72f)
                quadraticBezierTo(w * 0.56f, h * 0.76f, w * 0.68f, h * 0.62f)
            }
            drawPath(path, color = Color(0xFF2A1B28), style = Stroke(width = w * 0.04f, cap = StrokeCap.Round))
        }
        MouthStyle.WAVY_NERVOUS -> {
            val path = Path().apply {
                moveTo(w * 0.34f, h * 0.72f)
                quadraticBezierTo(w * 0.42f, h * 0.68f, w * 0.50f, h * 0.72f)
                quadraticBezierTo(w * 0.58f, h * 0.76f, w * 0.66f, h * 0.72f)
            }
            drawPath(path, color = Color(0xFF2A1B28), style = Stroke(width = w * 0.038f, cap = StrokeCap.Round))
        }
        MouthStyle.SAD_FROWN -> {
            val path = Path().apply {
                moveTo(w * 0.36f, h * 0.76f)
                quadraticBezierTo(w * 0.50f, h * 0.66f, w * 0.64f, h * 0.76f)
            }
            drawPath(path, color = Color(0xFF2A1B28), style = Stroke(width = w * 0.04f, cap = StrokeCap.Round))
        }
        MouthStyle.CAT_3 -> {
            val path = Path().apply {
                moveTo(w * 0.34f, h * 0.68f)
                quadraticBezierTo(w * 0.42f, h * 0.76f, w * 0.50f, h * 0.70f)
                quadraticBezierTo(w * 0.58f, h * 0.76f, w * 0.66f, h * 0.68f)
            }
            drawPath(path, color = Color(0xFF2A1B28), style = Stroke(width = w * 0.038f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawBlush(w: Float, h: Float) {
    drawCircle(Color(0xFFFF4081).copy(alpha = 0.35f), radius = w * 0.10f, center = Offset(w * 0.18f, h * 0.54f))
    drawCircle(Color(0xFFFF4081).copy(alpha = 0.35f), radius = w * 0.10f, center = Offset(w * 0.82f, h * 0.54f))
}

private fun DrawScope.drawTearDrops(w: Float, h: Float) {
    val tearColor = Color(0xFF00B0FF)
    drawOval(tearColor, topLeft = Offset(w * 0.20f, h * 0.52f), size = Size(w * 0.08f, h * 0.18f))
    drawOval(tearColor, topLeft = Offset(w * 0.72f, h * 0.52f), size = Size(w * 0.08f, h * 0.18f))
}

private fun DrawScope.drawSweatDrop(w: Float, h: Float) {
    val sweatColor = Color(0xFF00E5FF)
    drawOval(sweatColor, topLeft = Offset(w * 0.78f, h * 0.20f), size = Size(w * 0.10f, h * 0.18f))
}

private fun DrawScope.drawFloatingHeart(w: Float, h: Float) {
    drawHeart(w * 0.82f, h * 0.22f, w * 0.12f, Color(0xFFFF1744))
}

private fun DrawScope.drawAngelHalo(w: Float, h: Float) {
    drawOval(
        color = Color(0xFFFFD700),
        topLeft = Offset(w * 0.20f, h * 0.01f),
        size = Size(w * 0.60f, h * 0.14f),
        style = Stroke(width = w * 0.045f)
    )
}

private fun DrawScope.drawDevilHorns(w: Float, h: Float) {
    val hornColor = Color(0xFFD50000)
    val leftHorn = Path().apply {
        moveTo(w * 0.18f, h * 0.22f)
        quadraticBezierTo(w * 0.10f, h * 0.02f, w * 0.28f, h * 0.04f)
        close()
    }
    val rightHorn = Path().apply {
        moveTo(w * 0.82f, h * 0.22f)
        quadraticBezierTo(w * 0.90f, h * 0.02f, w * 0.72f, h * 0.04f)
        close()
    }
    drawPath(leftHorn, color = hornColor)
    drawPath(rightHorn, color = hornColor)
}

private fun DrawScope.drawPartyHat(w: Float, h: Float) {
    val hatPath = Path().apply {
        moveTo(w * 0.28f, h * 0.14f)
        lineTo(w * 0.50f, -h * 0.16f)
        lineTo(w * 0.72f, h * 0.14f)
        close()
    }
    drawPath(hatPath, brush = Brush.linearGradient(listOf(Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E5FF))))
    drawCircle(Color(0xFFFFEA00), radius = w * 0.05f, center = Offset(w * 0.50f, -h * 0.16f))
}

private fun DrawScope.drawCatEars(w: Float, h: Float) {
    val earColor = Color(0xFFFF80AB)
    val leftEar = Path().apply {
        moveTo(w * 0.12f, h * 0.24f)
        lineTo(w * 0.18f, h * 0.02f)
        lineTo(w * 0.36f, h * 0.16f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(w * 0.88f, h * 0.24f)
        lineTo(w * 0.82f, h * 0.02f)
        lineTo(w * 0.64f, h * 0.16f)
        close()
    }
    drawPath(leftEar, color = earColor)
    drawPath(rightEar, color = earColor)
}

private fun DrawScope.drawHeart(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy + radius * 0.6f)
        cubicTo(cx - radius * 1.1f, cy, cx - radius * 0.8f, cy - radius * 0.8f, cx, cy - radius * 0.3f)
        cubicTo(cx + radius * 0.8f, cy - radius * 0.8f, cx + radius * 1.1f, cy, cx, cy + radius * 0.6f)
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawStar(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - radius)
        lineTo(cx + radius * 0.3f, cy - radius * 0.3f)
        lineTo(cx + radius, cy - radius * 0.2f)
        lineTo(cx + radius * 0.45f, cy + radius * 0.25f)
        lineTo(cx + radius * 0.65f, cy + radius * 0.9f)
        lineTo(cx, cy + radius * 0.45f)
        lineTo(cx - radius * 0.65f, cy + radius * 0.9f)
        lineTo(cx - radius * 0.45f, cy + radius * 0.25f)
        lineTo(cx - radius, cy - radius * 0.2f)
        lineTo(cx - radius * 0.3f, cy - radius * 0.3f)
        close()
    }
    drawPath(path, color = color)
}
