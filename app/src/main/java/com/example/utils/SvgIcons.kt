package com.example.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object SvgIcons {
    // 100+ Distinct SVG / Vector Icons categorized
    val faces = listOf(
        Icons.Default.Face, Icons.Default.Mood, Icons.Default.SentimentVerySatisfied,
        Icons.Default.SentimentSatisfied, Icons.Default.SentimentNeutral, Icons.Default.SentimentDissatisfied,
        Icons.Default.SentimentVeryDissatisfied, Icons.Default.EmojiEmotions, Icons.Default.SmartToy,
        Icons.Default.Pets, Icons.Default.AccountCircle, Icons.Default.SupervisorAccount
    )

    val expressions = listOf(
        Icons.Default.ThumbUp, Icons.Default.ThumbDown, Icons.Default.Favorite, Icons.Default.Star,
        Icons.Default.LocalFireDepartment, Icons.Default.FlashOn, Icons.Default.Celebration,
        Icons.Default.FavoriteBorder, Icons.Default.Grade, Icons.Default.Whatshot,
        Icons.Default.EmojiObjects, Icons.Default.AutoAwesome
    )

    val gestures = listOf(
        Icons.Default.PanTool, Icons.Default.BackHand, Icons.Default.WavingHand,
        Icons.Default.Handshake, Icons.Default.SignLanguage, Icons.Default.TouchApp,
        Icons.Default.Gesture, Icons.Default.MilitaryTech, Icons.Default.WorkspacePremium,
        Icons.Default.EmojiEvents, Icons.Default.Verified, Icons.Default.StarRate
    )

    val people = listOf(
        Icons.Default.Person, Icons.Default.PersonAdd, Icons.Default.PersonRemove,
        Icons.Default.Group, Icons.Default.Groups, Icons.Default.GroupAdd,
        Icons.Default.AdminPanelSettings, Icons.Default.Security, Icons.Default.Shield,
        Icons.Default.Badge, Icons.Default.VpnKey, Icons.Default.Lock
    )

    val objects = listOf(
        Icons.Default.Phone, Icons.Default.Email, Icons.Default.Chat, Icons.Default.Message,
        Icons.Default.Send, Icons.Default.Share, Icons.Default.Bookmark, Icons.Default.Label,
        Icons.Default.Flag, Icons.Default.Map, Icons.Default.Navigation, Icons.Default.Place
    )

    val reactions = listOf(
        Icons.Default.ThumbUp, Icons.Default.Favorite, Icons.Default.Star, Icons.Default.LocalFireDepartment,
        Icons.Default.Celebration, Icons.Default.FavoriteBorder, Icons.Default.ThumbDown, Icons.Default.EmojiEmotions,
        Icons.Default.Grade, Icons.Default.Whatshot, Icons.Default.FlashOn, Icons.Default.AutoAwesome
    )

    val symbols = listOf(
        Icons.Default.Check, Icons.Default.Close, Icons.Default.Add, Icons.Default.Remove,
        Icons.Default.Done, Icons.Default.DoneAll, Icons.Default.Warning, Icons.Default.Error,
        Icons.Default.Info, Icons.Default.Help, Icons.Default.Settings, Icons.Default.Search
    )

    val states = listOf(
        Icons.Default.Wifi, Icons.Default.SignalCellularAlt, Icons.Default.BatteryFull, Icons.Default.Bluetooth,
        Icons.Default.LocationOn, Icons.Default.Visibility, Icons.Default.VisibilityOff, Icons.Default.Notifications,
        Icons.Default.NotificationsOff, Icons.Default.VolumeUp, Icons.Default.VolumeOff, Icons.Default.Sync
    )

    val actions = listOf(
        Icons.Default.Edit, Icons.Default.Delete, Icons.Default.Refresh, Icons.Default.PlayArrow,
        Icons.Default.Pause, Icons.Default.Stop, Icons.Default.Download, Icons.Default.Upload,
        Icons.Default.CameraAlt, Icons.Default.Image, Icons.Default.Videocam, Icons.Default.Mic
    )

    val decorative = listOf(
        Icons.Default.Palette, Icons.Default.Brush, Icons.Default.ColorLens, Icons.Default.Image,
        Icons.Default.AutoFixHigh, Icons.Default.Texture, Icons.Default.BlurOn, Icons.Default.Grain,
        Icons.Default.LightMode, Icons.Default.DarkMode, Icons.Default.WbSunny, Icons.Default.Nightlight
    )

    fun getAllIcons(): List<ImageVector> {
        return (faces + expressions + gestures + people + objects + reactions + symbols + states + actions + decorative).distinct()
    }
}
