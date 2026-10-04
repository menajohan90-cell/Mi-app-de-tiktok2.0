package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.BlockRepository
import com.example.data.ChatRepository
import com.example.data.MessageResponse
import com.example.data.ProfileResponse
import com.example.data.UserRepository
import com.example.ui.components.UserAvatar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(navController: NavController, chatId: String) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val chatRepository = remember { ChatRepository() }
    val userRepository = remember { UserRepository() }
    val blockRepository = remember { BlockRepository() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    var messages by remember { mutableStateOf<List<MessageResponse>>(emptyList()) }
    var chatPartnerProfile by remember { mutableStateOf<ProfileResponse?>(null) }
    var partnerUid by remember { mutableStateOf("") }
    var isBlocked by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var isPartnerTyping by remember { mutableStateOf(false) }
    var isPartnerOnline by remember { mutableStateOf(false) }
    var selectedImageUriForPreview by remember { mutableStateOf<Uri?>(null) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSaveSuccessDialog by remember { mutableStateOf(false) }
    var savedLink by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUriForPreview = uri
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val currentUid = currentUser?.uid ?: ""
    val resolvedPartnerUid = remember(chatId, currentUid) {
        if (chatId.contains("_")) {
            val parts = chatId.split("_")
            parts.firstOrNull { it != currentUid } ?: ""
        } else {
            chatId
        }
    }
    val canonicalChatId = remember(currentUid, resolvedPartnerUid) {
        if (currentUid.isNotBlank() && resolvedPartnerUid.isNotBlank() && resolvedPartnerUid != currentUid) {
            if (currentUid < resolvedPartnerUid) "${currentUid}_$resolvedPartnerUid" else "${resolvedPartnerUid}_$currentUid"
        } else {
            chatId
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Escucha en tiempo real de Firestore para mensajes instantáneos
    DisposableEffect(canonicalChatId) {
        val listener = chatRepository.listenMessages(canonicalChatId) { newMsgs ->
            if (newMsgs.size > messages.size && messages.isNotEmpty()) {
                val latest = newMsgs.lastOrNull()
                if (latest != null && latest.senderId != currentUser?.uid) {
                    val textToNotify = if (com.example.utils.CustomSvgEmojis.isSvgEmoji(latest.text)) {
                        val item = com.example.utils.CustomSvgEmojis.getEmoji(com.example.utils.CustomSvgEmojis.parseEmojiId(latest.text))
                        "Te envió un emoji: ${item?.name ?: "Emoji"}"
                    } else if (com.example.utils.SvgEmojis.isSvgEmoji(latest.text)) {
                        val item = com.example.utils.SvgEmojis.getEmoji(com.example.utils.SvgEmojis.parseEmojiId(latest.text))
                        "Te envió un emoji SVG: ${item?.name ?: "Emoji"}"
                    } else if (!latest.imageUrl.isNullOrBlank()) {
                        "Te envió una imagen"
                    } else {
                        latest.text
                    }
                    com.example.utils.NotificationHelper.showMessageNotification(
                        context = context,
                        senderName = chatPartnerProfile?.displayName ?: "Nuevo Mensaje",
                        messageText = textToNotify
                    )
                }
            }
            messages = newMsgs

            // Marcar mensajes recibidos como leídos
            if (resolvedPartnerUid.isNotBlank()) {
                scope.launch {
                    chatRepository.markMessagesAsRead(canonicalChatId, resolvedPartnerUid)
                }
            }
        }

        onDispose {
            listener.remove()
        }
    }

    // Actualización de estado del partner (perfil, bloqueo, typing y presencia online)
    LaunchedEffect(canonicalChatId, resolvedPartnerUid) {
        partnerUid = resolvedPartnerUid
        if (currentUser != null && resolvedPartnerUid.isNotBlank()) {
            isBlocked = blockRepository.isBlocked(resolvedPartnerUid)
            val profileResult = userRepository.getProfile(resolvedPartnerUid)
            profileResult.onSuccess { chatPartnerProfile = it }
            chatRepository.markMessagesAsRead(canonicalChatId, resolvedPartnerUid)

            while (true) {
                delay(2000)
                try {
                    isBlocked = blockRepository.isBlocked(resolvedPartnerUid)
                    isPartnerTyping = chatRepository.getPartnerTyping(canonicalChatId, resolvedPartnerUid)

                    // Consultar presencia real del usuario
                    val doc = FirebaseFirestore.getInstance().collection("users").document(resolvedPartnerUid).get()
                    // Si el documento existe, comprobar su campo isOnline o lastActive
                    val partnerDoc = com.google.android.gms.tasks.Tasks.await(doc)
                    if (partnerDoc.exists()) {
                        val online = partnerDoc.getBoolean("isOnline") ?: false
                        val lastActive = partnerDoc.getLong("lastActive") ?: 0L
                        isPartnerOnline = online && (System.currentTimeMillis() - lastActive < 90000)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    if (isBlocked) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Chat no disponible", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            },
            containerColor = Color.Black
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = Color.Red, modifier = Modifier.size(64.dp))
                    Text(
                        text = "El usuario pudo haberte bloqueado o eliminado la cuenta.",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val result = blockRepository.saveChatCopy(canonicalChatId, messages, chatPartnerProfile?.displayName ?: "Usuario")
                                result.onSuccess { link ->
                                    savedLink = link
                                    showSaveSuccessDialog = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar chat", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Borrar chat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar con icono humano y punto de presencia real
                        UserAvatar(
                            photoUrl = chatPartnerProfile?.photoUrl,
                            size = 40.dp,
                            isOnline = isPartnerOnline
                        )

                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = chatPartnerProfile?.displayName ?: chatPartnerProfile?.username ?: "Chat", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 16.sp
                            )
                            // Estado de escritura o presencia
                            if (isPartnerTyping) {
                                Text(
                                    text = "está escribiendo...", 
                                    color = Color(0xFF38BDF8), 
                                    fontSize = 11.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            } else if (isPartnerOnline) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("En línea", color = Color(0xFF10B981), fontSize = 11.sp)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White).border(1.dp, Color.Black, CircleShape))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Desconectado", color = Color(0xFFA1A1AA), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Bloquear cuenta", color = Color.Red) },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    if (partnerUid.isNotBlank()) {
                                        blockRepository.blockUser(partnerUid)
                                        isBlocked = true
                                        Toast.makeText(context, "Cuenta bloqueada", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Guardar chat", color = Color.White) },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    val result = blockRepository.saveChatCopy(canonicalChatId, messages, chatPartnerProfile?.displayName ?: "Usuario")
                                    result.onSuccess { link ->
                                        savedLink = link
                                        showSaveSuccessDialog = true
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Borrar chat", color = Color.White) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("¡Conectados! Dile hola a tu amigo", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed(), key = { it.messageId }) { msg ->
                        val isMe = msg.senderId == currentUser?.uid
                        val isCustomSvg = com.example.utils.CustomSvgEmojis.isSvgEmoji(msg.text)
                        val customEmojiItem = if (isCustomSvg) com.example.utils.CustomSvgEmojis.getEmoji(com.example.utils.CustomSvgEmojis.parseEmojiId(msg.text)) else null
                        val isSvg = com.example.utils.SvgEmojis.isSvgEmoji(msg.text)
                        val emojiItem = if (isSvg) com.example.utils.SvgEmojis.getEmoji(com.example.utils.SvgEmojis.parseEmojiId(msg.text)) else null

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            if (customEmojiItem != null) {
                                // Emoji Ilustrado Personalizado
                                Column(
                                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = if (isMe) Color(0xFF1E3A8A).copy(alpha = 0.40f) else Color(0xFF262626),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8EC5FC).copy(alpha = 0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            com.example.utils.CustomSvgEmojiGraphic(
                                                emoji = customEmojiItem,
                                                modifier = Modifier.size(68.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = customEmojiItem.name,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.padding(top = 2.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.createdAt)),
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                        if (isMe) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (msg.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                                contentDescription = if (msg.isRead) "Visto" else "Enviado",
                                                tint = if (msg.isRead) Color(0xFF38BDF8) else Color(0xFFA1A1AA),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            } else if (emojiItem != null) {
                                // Sticker Emoji SVG
                                Column(
                                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = if (isMe) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFF262626),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, emojiItem.color.copy(alpha = 0.7f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape)
                                                    .background(emojiItem.color.copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = emojiItem.icon,
                                                    contentDescription = emojiItem.name,
                                                    tint = emojiItem.color,
                                                    modifier = Modifier.size(42.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = emojiItem.name,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.padding(top = 2.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.createdAt)),
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                        if (isMe) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (msg.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                                contentDescription = if (msg.isRead) "Visto" else "Enviado",
                                                tint = if (msg.isRead) Color(0xFF38BDF8) else Color(0xFFA1A1AA),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            } else if (!msg.imageUrl.isNullOrBlank()) {
                                // Foto / Video
                                Column(
                                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    AsyncImage(
                                        model = msg.imageUrl,
                                        contentDescription = "Imagen de chat",
                                        modifier = Modifier
                                            .widthIn(max = 260.dp)
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { expandedImageUrl = msg.imageUrl }
                                    )
                                    if (msg.text.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = msg.text, color = Color.White, fontSize = 13.sp)
                                    }
                                    Row(
                                        modifier = Modifier.padding(top = 2.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.createdAt)),
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                        if (isMe) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (msg.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                                contentDescription = if (msg.isRead) "Visto" else "Enviado",
                                                tint = if (msg.isRead) Color(0xFF38BDF8) else Color(0xFFA1A1AA),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 16.dp
                                    ),
                                    color = if (isMe) Color(0xFF2563EB) else Color(0xFF27272A),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Text(
                                            text = msg.text,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            lineHeight = 19.sp
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.align(Alignment.End),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.createdAt)),
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 10.sp
                                            )
                                            if (isMe) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (msg.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                                    contentDescription = if (msg.isRead) "Visto" else "Enviado",
                                                    tint = if (msg.isRead) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Typing Indicator del compañero
            AnimatedVisibility(visible = isPartnerTyping) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${chatPartnerProfile?.displayName ?: "El usuario"} está escribiendo...",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Barra de entrada moderna
            Surface(
                color = Color(0xFF18181B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "Enviar foto", tint = Color(0xFF3B82F6))
                    }
                    IconButton(onClick = { showEmojiPicker = true }) {
                        Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = "Emojis SVG", tint = Color(0xFFF59E0B))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { 
                            inputText = it
                            scope.launch {
                                chatRepository.setTyping(canonicalChatId, it.isNotBlank())
                            }
                        },
                        placeholder = { Text("Escribe un mensaje...", color = Color(0xFFA1A1AA)) },
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp, max = 120.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF3F3F46),
                            focusedContainerColor = Color(0xFF27272A),
                            unfocusedContainerColor = Color(0xFF27272A)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val textToSend = inputText
                                inputText = ""
                                scope.launch {
                                    chatRepository.setTyping(canonicalChatId, false)
                                    chatRepository.sendMessage(canonicalChatId, textToSend)
                                }
                            }
                        },
                        modifier = Modifier.size(44.dp).background(Color(0xFF3B82F6), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (selectedImageUriForPreview != null) {
        AlertDialog(
            onDismissRequest = { selectedImageUriForPreview = null },
            title = { Text("Enviar imagen al chat") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = selectedImageUriForPreview,
                        contentDescription = "Previsualización",
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Esta imagen se enviará exclusivamente a este chat.", color = Color.Gray, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedImageUriForPreview!!
                        selectedImageUriForPreview = null
                        scope.launch {
                            chatRepository.sendMessage(canonicalChatId, "", imageUrl = uri.toString())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedImageUriForPreview = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (expandedImageUrl != null) {
        Dialog(onDismissRequest = { expandedImageUrl = null }) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black).clickable { expandedImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = expandedImageUrl,
                    contentDescription = "Imagen ampliada",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Borrar chat") },
            text = { Text("¿Seguro que deseas borrar este chat?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    navController.popBackStack()
                }) {
                    Text("Borrar chat", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showSaveSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSaveSuccessDialog = false },
            title = { Text("Chat guardado") },
            text = { Text("Copia de seguridad persistente generada:\n\n$savedLink") },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(savedLink))
                    Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    showSaveSuccessDialog = false
                    navController.popBackStack()
                }) {
                    Text("Copiar enlace y salir")
                }
            }
        )
    }

    if (showEmojiPicker) {
        var selectedCategory by remember { mutableStateOf("Todos") }
        val categories = listOf("Todos", "Caras", "Amor", "Fiesta", "Cool", "Sorpresa", "Fantasía", "Drama", "Enojo")
        val filteredEmojis = remember(selectedCategory) {
            if (selectedCategory == "Todos") com.example.utils.CustomSvgEmojis.list
            else com.example.utils.CustomSvgEmojis.list.filter { it.category == selectedCategory }
        }

        ModalBottomSheet(
            onDismissRequest = { showEmojiPicker = false },
            containerColor = Color(0xFF1E1E1E)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).height(500.dp)) {
                Text(
                    "100 Emojis SVG Ilustrados (Estilo Personalizado)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF2A2A2A),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredEmojis.size) { index ->
                        val emoji = filteredEmojis[index]
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8EC5FC).copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showEmojiPicker = false
                                    scope.launch {
                                        chatRepository.sendMessage(
                                            canonicalChatId,
                                            com.example.utils.CustomSvgEmojis.formatMessage(emoji.id)
                                        )
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                com.example.utils.CustomSvgEmojiGraphic(
                                    emoji = emoji,
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = emoji.name,
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
