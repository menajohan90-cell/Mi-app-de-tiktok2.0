package com.example.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class SvgEmojiItem(
    val id: String,
    val name: String,
    val category: String,
    val icon: ImageVector,
    val color: Color
)

object SvgEmojis {
    val list: List<SvgEmojiItem> = listOf(
        // === CARAS Y EXPRESIONES BÁSICAS (1-20) ===
        SvgEmojiItem("happy_smile", "Sonrisa Feliz", "Caras", Icons.Default.SentimentVerySatisfied, Color(0xFFFFD233)),
        SvgEmojiItem("satisfied", "Satisfecho", "Caras", Icons.Default.SentimentSatisfied, Color(0xFFFFC107)),
        SvgEmojiItem("neutral", "Neutral", "Caras", Icons.Default.SentimentNeutral, Color(0xFFFFE082)),
        SvgEmojiItem("dissatisfied", "Disgustado", "Caras", Icons.Default.SentimentDissatisfied, Color(0xFFFFB74D)),
        SvgEmojiItem("very_sad", "Muy Triste", "Caras", Icons.Default.SentimentVeryDissatisfied, Color(0xFFFF8A65)),
        SvgEmojiItem("emoji_mood", "Emocionado", "Caras", Icons.Default.Mood, Color(0xFFFFD54F)),
        SvgEmojiItem("bad_mood", "Mal Humor", "Caras", Icons.Default.MoodBad, Color(0xFFEF5350)),
        SvgEmojiItem("emotions", "Carita Alegre", "Caras", Icons.Default.EmojiEmotions, Color(0xFFFFCA28)),
        SvgEmojiItem("face_classic", "Cara Clásica", "Caras", Icons.Default.Face, Color(0xFFFFD54F)),
        SvgEmojiItem("heart_eyes", "Ojos Enamorados", "Caras", Icons.Default.Favorite, Color(0xFFFF1744)),
        SvgEmojiItem("heart_border", "Cariñoso", "Caras", Icons.Default.FavoriteBorder, Color(0xFFFF4081)),
        SvgEmojiItem("star_eyes", "Estrella Asombrada", "Caras", Icons.Default.Star, Color(0xFFFFD700)),
        SvgEmojiItem("celebration_face", "Fiesta y Confeti", "Caras", Icons.Default.Celebration, Color(0xFFFF6D00)),
        SvgEmojiItem("flame_hot", "Cara de Fuego", "Caras", Icons.Default.Whatshot, Color(0xFFFF3D00)),
        SvgEmojiItem("fire_passionate", "Pasión Ardiente", "Caras", Icons.Default.LocalFireDepartment, Color(0xFFFF5722)),
        SvgEmojiItem("flash_energy", "Energía Eléctrica", "Caras", Icons.Default.FlashOn, Color(0xFFFFEA00)),
        SvgEmojiItem("magic_spark", "Cara Mágica", "Caras", Icons.Default.AutoAwesome, Color(0xFFAB47BC)),
        SvgEmojiItem("idea_bright", "Tengo una Idea", "Caras", Icons.Default.EmojiObjects, Color(0xFFFFEB3B)),
        SvgEmojiItem("psychology_think", "Pensamiento Profundo", "Caras", Icons.Default.Psychology, Color(0xFF42A5F5)),
        SvgEmojiItem("visibility_eye", "Ojo Atento", "Caras", Icons.Default.Visibility, Color(0xFF26A69A)),

        // === PERSONAJES Y MONSTRUOS (21-40) ===
        SvgEmojiItem("robot_smart", "Robot Inteligente", "Personajes", Icons.Default.SmartToy, Color(0xFF00E5FF)),
        SvgEmojiItem("pet_animal", "Gatito/Perrito", "Personajes", Icons.Default.Pets, Color(0xFF8D6E63)),
        SvgEmojiItem("alien_face", "Extraterrestre", "Personajes", Icons.Default.BrightnessAuto, Color(0xFF76FF03)),
        SvgEmojiItem("monster_mask", "Máscara de Teatro", "Personajes", Icons.Default.TheaterComedy, Color(0xFFFF4081)),
        SvgEmojiItem("sports_ninja", "Luchador", "Personajes", Icons.Default.SportsKabaddi, Color(0xFFE040FB)),
        SvgEmojiItem("sports_martial", "Guerrero", "Personajes", Icons.Default.SportsMma, Color(0xFFFF5252)),
        SvgEmojiItem("superstar_grade", "Superestrella", "Personajes", Icons.Default.Grade, Color(0xFFFFD600)),
        SvgEmojiItem("trophy_winner", "Campeón", "Personajes", Icons.Default.EmojiEvents, Color(0xFFFFAB00)),
        SvgEmojiItem("military_hero", "Héroe", "Personajes", Icons.Default.MilitaryTech, Color(0xFFC0CA33)),
        SvgEmojiItem("workspace_vip", "Insignia VIP", "Personajes", Icons.Default.WorkspacePremium, Color(0xFFFFB300)),
        SvgEmojiItem("verified_blue", "Verificado Oficial", "Personajes", Icons.Default.Verified, Color(0xFF2979FF)),
        SvgEmojiItem("security_guard", "Guardián Seguro", "Personajes", Icons.Default.Security, Color(0xFF00B0FF)),
        SvgEmojiItem("shield_protector", "Escudo Protector", "Personajes", Icons.Default.Shield, Color(0xFF00E676)),
        SvgEmojiItem("crown_king", "Corona Real", "Personajes", Icons.Default.AutoFixHigh, Color(0xFFFFD700)),
        SvgEmojiItem("vpn_key", "Llave Maestra", "Personajes", Icons.Default.VpnKey, Color(0xFFFFC400)),
        SvgEmojiItem("lock_secret", "Secreto Candado", "Personajes", Icons.Default.Lock, Color(0xFFFF9100)),
        SvgEmojiItem("badge_id", "Identidad", "Personajes", Icons.Default.Badge, Color(0xFF536DFE)),
        SvgEmojiItem("admin_shield", "Administrador", "Personajes", Icons.Default.AdminPanelSettings, Color(0xFF651FFF)),
        SvgEmojiItem("groups_squad", "La Pandilla", "Personajes", Icons.Default.Groups, Color(0xFF1DE9B6)),
        SvgEmojiItem("person_friend", "Mejor Amigo", "Personajes", Icons.Default.Person, Color(0xFF00BFA5)),

        // === GESTOS Y REACCIONES (41-60) ===
        SvgEmojiItem("thumbs_up", "Pulgar Arriba", "Gestos", Icons.Default.ThumbUp, Color(0xFF4CAF50)),
        SvgEmojiItem("thumbs_down", "Pulgar Abajo", "Gestos", Icons.Default.ThumbDown, Color(0xFFF44336)),
        SvgEmojiItem("hand_stop", "¡Alto Ahí!", "Gestos", Icons.Default.PanTool, Color(0xFFFF9800)),
        SvgEmojiItem("hand_wave", "Saludando", "Gestos", Icons.Default.WavingHand, Color(0xFFFFCA28)),
        SvgEmojiItem("back_hand", "Mano Levantada", "Gestos", Icons.Default.BackHand, Color(0xFFFFB74D)),
        SvgEmojiItem("handshake_deal", "Trato Hecho", "Gestos", Icons.Default.Handshake, Color(0xFF8D6E63)),
        SvgEmojiItem("touch_app", "Tócame", "Gestos", Icons.Default.TouchApp, Color(0xFF00BCD4)),
        SvgEmojiItem("sign_language", "Paz y Amor", "Gestos", Icons.Default.SignLanguage, Color(0xFF7E57C2)),
        SvgEmojiItem("gesture_draw", "Firma Cool", "Gestos", Icons.Default.Gesture, Color(0xFF26A69A)),
        SvgEmojiItem("check_done", "Completado", "Gestos", Icons.Default.Check, Color(0xFF4CAF50)),
        SvgEmojiItem("done_all", "Visto Todo", "Gestos", Icons.Default.DoneAll, Color(0xFF2196F3)),
        SvgEmojiItem("close_cancel", "Rechazado", "Gestos", Icons.Default.Close, Color(0xFFE53935)),
        SvgEmojiItem("warning_alert", "Alerta Máxima", "Gestos", Icons.Default.Warning, Color(0xFFFFC107)),
        SvgEmojiItem("error_stop", "¡Peligro!", "Gestos", Icons.Default.Error, Color(0xFFD32F2F)),
        SvgEmojiItem("info_tip", "Dato Curioso", "Gestos", Icons.Default.Info, Color(0xFF0288D1)),
        SvgEmojiItem("help_question", "¿Qué Pasó?", "Gestos", Icons.Default.Help, Color(0xFF5C6BC0)),
        SvgEmojiItem("add_plus", "Añadir Más", "Gestos", Icons.Default.Add, Color(0xFF43A047)),
        SvgEmojiItem("remove_minus", "Menos", "Gestos", Icons.Default.Remove, Color(0xFFE53935)),
        SvgEmojiItem("refresh_loop", "Repetir", "Gestos", Icons.Default.Refresh, Color(0xFF00ACC1)),
        SvgEmojiItem("sync_vibes", "En Sintonía", "Gestos", Icons.Default.Sync, Color(0xFF3949AB)),

        // === OBJETOS DIVERTIDOS Y SÍMBOLOS (61-80) ===
        SvgEmojiItem("rocket_launch", "Despegue Cohete", "Objetos", Icons.Default.RocketLaunch, Color(0xFFFF3D00)),
        SvgEmojiItem("flight_plane", "Me Voy de Viaje", "Objetos", Icons.Default.Flight, Color(0xFF00B0FF)),
        SvgEmojiItem("music_note", "Buena Música", "Objetos", Icons.Default.MusicNote, Color(0xFFE040FB)),
        SvgEmojiItem("headset_audio", "Escuchando Temazo", "Objetos", Icons.Default.Headphones, Color(0xFF00E676)),
        SvgEmojiItem("volume_up", "A Todo Volumen", "Objetos", Icons.Default.VolumeUp, Color(0xFFFF9100)),
        SvgEmojiItem("volume_off", "Silencio Porfa", "Objetos", Icons.Default.VolumeOff, Color(0xFF78909C)),
        SvgEmojiItem("mic_sing", "Cantando al Máximo", "Objetos", Icons.Default.Mic, Color(0xFFFF4081)),
        SvgEmojiItem("camera_snap", "Foto Espontánea", "Objetos", Icons.Default.CameraAlt, Color(0xFF00E5FF)),
        SvgEmojiItem("videocam_stream", "En Vivo", "Objetos", Icons.Default.Videocam, Color(0xFFFF1744)),
        SvgEmojiItem("palette_art", "Puro Arte", "Objetos", Icons.Default.Palette, Color(0xFFFF80AB)),
        SvgEmojiItem("brush_style", "Pintando Estilo", "Objetos", Icons.Default.Brush, Color(0xFF7C4DFF)),
        SvgEmojiItem("color_lens", "Lentes de Colores", "Objetos", Icons.Default.ColorLens, Color(0xFF00BFA5)),
        SvgEmojiItem("sun_bright", "Día Soleado", "Objetos", Icons.Default.WbSunny, Color(0xFFFFD600)),
        SvgEmojiItem("night_moon", "Noche Mágica", "Objetos", Icons.Default.Nightlight, Color(0xFF90CAF9)),
        SvgEmojiItem("dark_mode", "Modo Oscuro", "Objetos", Icons.Default.DarkMode, Color(0xFFB0BEC5)),
        SvgEmojiItem("light_bulb", "Foco Brillante", "Objetos", Icons.Default.Lightbulb, Color(0xFFFFEE58)),
        SvgEmojiItem("shopping_bag", "Modo Compras", "Objetos", Icons.Default.ShoppingBag, Color(0xFFFF4081)),
        SvgEmojiItem("coffee_cup", "Cafecito Mañanero", "Objetos", Icons.Default.LocalCafe, Color(0xFF795548)),
        SvgEmojiItem("pizza_slice", "Hora de Comer", "Objetos", Icons.Default.LocalPizza, Color(0xFFFF9800)),
        SvgEmojiItem("bar_drink", "Brindis Salud", "Objetos", Icons.Default.LocalBar, Color(0xFFFF4081)),

        // === ESTADOS, ACCIONES Y EXTRA COOL (81-105) ===
        SvgEmojiItem("fire_truck", "Emergencia", "Estados", Icons.Default.LocalShipping, Color(0xFFF44336)),
        SvgEmojiItem("car_speed", "Acelerando", "Estados", Icons.Default.DirectionsCar, Color(0xFF3F51B5)),
        SvgEmojiItem("bike_ride", "Paseo en Bici", "Estados", Icons.Default.DirectionsBike, Color(0xFF4CAF50)),
        SvgEmojiItem("walk_chill", "Caminando Tranqui", "Estados", Icons.Default.DirectionsWalk, Color(0xFF009688)),
        SvgEmojiItem("battery_100", "100% de Batería", "Estados", Icons.Default.BatteryFull, Color(0xFF00E676)),
        SvgEmojiItem("wifi_online", "Full Conexión", "Estados", Icons.Default.Wifi, Color(0xFF2979FF)),
        SvgEmojiItem("signal_max", "Señal Al Tope", "Estados", Icons.Default.SignalCellularAlt, Color(0xFF651FFF)),
        SvgEmojiItem("bluetooth_link", "Conectado", "Estados", Icons.Default.Bluetooth, Color(0xFF0091EA)),
        SvgEmojiItem("notification_bell", "¡Notificación!", "Estados", Icons.Default.Notifications, Color(0xFFFFAB00)),
        SvgEmojiItem("location_pin", "Aquí Estoy", "Estados", Icons.Default.LocationOn, Color(0xFFFF1744)),
        SvgEmojiItem("map_explorer", "Explorando", "Estados", Icons.Default.Map, Color(0xFF43A047)),
        SvgEmojiItem("bookmark_save", "Guardado en Favoritos", "Estados", Icons.Default.Bookmark, Color(0xFF00E5FF)),
        SvgEmojiItem("flag_finish", "Meta Cumplida", "Estados", Icons.Default.Flag, Color(0xFFFF9100)),
        SvgEmojiItem("cloud_upload", "Subido a la Nube", "Estados", Icons.Default.CloudUpload, Color(0xFF448AFF)),
        SvgEmojiItem("download_done", "Descargado", "Estados", Icons.Default.Download, Color(0xFF00E676)),
        SvgEmojiItem("visibility_off", "No Quiero Ver", "Estados", Icons.Default.VisibilityOff, Color(0xFF9E9E9E)),
        SvgEmojiItem("power_button", "Encendido", "Estados", Icons.Default.PowerSettingsNew, Color(0xFFFF5252)),
        SvgEmojiItem("speed_dial", "Rápido y Furioso", "Estados", Icons.Default.Speed, Color(0xFFFF3D00)),
        SvgEmojiItem("diamond_gem", "Joya Preciosa", "Estados", Icons.Default.Diamond, Color(0xFF00E5FF)),
        SvgEmojiItem("military_shield", "Invencible", "Estados", Icons.Default.ShieldMoon, Color(0xFF7C4DFF)),
        SvgEmojiItem("spa_relax", "Relajación Total", "Estados", Icons.Default.Spa, Color(0xFF66BB6A)),
        SvgEmojiItem("fitness_gym", "Modo Bestia Gym", "Estados", Icons.Default.FitnessCenter, Color(0xFFFF5722)),
        SvgEmojiItem("pool_swim", "Piscina y Sol", "Estados", Icons.Default.Pool, Color(0xFF03A9F4)),
        SvgEmojiItem("park_nature", "Aire Puro", "Estados", Icons.Default.Park, Color(0xFF2E7D32)),
        SvgEmojiItem("cake_birthday", "Pastel Cumpleaños", "Estados", Icons.Default.Cake, Color(0xFFFF4081))
    )

    private val map: Map<String, SvgEmojiItem> = list.associateBy { it.id }

    fun getEmoji(id: String): SvgEmojiItem? = map[id]

    fun isSvgEmoji(text: String): Boolean {
        return text.startsWith("[svg_emoji:") && text.endsWith("]")
    }

    fun parseEmojiId(text: String): String {
        return text.removePrefix("[svg_emoji:").removeSuffix("]")
    }

    fun formatMessage(id: String): String {
        return "[svg_emoji:$id]"
    }
}
