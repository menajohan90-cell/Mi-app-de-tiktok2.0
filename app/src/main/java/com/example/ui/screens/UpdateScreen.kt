package com.example.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.AppUpdateModel
import com.example.data.UpdateRepository
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val updateRepository = remember { UpdateRepository() }

    var isLoading by remember { mutableStateOf(true) }
    var isChecking by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var latestUpdate by remember { mutableStateOf<AppUpdateModel?>(null) }
    val localVersionName = remember { updateRepository.getInstalledVersionName(context) }
    val localVersionCode = remember { updateRepository.getInstalledVersionCode(context) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadedBytesText by remember { mutableStateOf("") }
    var downloadComplete by remember { mutableStateOf(false) }

    // Diálogo para configurar enlace de descarga directo
    var showConfigUrlDialog by remember { mutableStateOf(false) }
    var inputApkUrl by remember { mutableStateOf("") }
    var isSavingUrl by remember { mutableStateOf(false) }
    var pendingActionAfterSave by remember { mutableStateOf<String?>(null) }

    fun fetchUpdate(isManual: Boolean = false) {
        scope.launch {
            if (isManual) isChecking = true else isLoading = true
            checkError = null
            val result = updateRepository.getLatestUpdate()
            result.onSuccess { update ->
                latestUpdate = update
                if (!update?.apkUrl.isNullOrBlank()) {
                    inputApkUrl = update!!.apkUrl
                }
            }.onFailure { e ->
                checkError = e.message ?: "Error de conexión"
            }
            isLoading = false
            isChecking = false
        }
    }

    LaunchedEffect(Unit) {
        fetchUpdate(false)
    }

    val remoteCode = latestUpdate?.versionCode ?: localVersionCode
    val hasNewVersion = latestUpdate != null && remoteCode > localVersionCode
    val scrollState = rememberScrollState()

    fun startDownload(urlToUse: String) {
        if (urlToUse.isBlank()) {
            pendingActionAfterSave = "download"
            showConfigUrlDialog = true
            return
        }
        isDownloading = true
        scope.launch {
            val result = updateRepository.downloadAndInstallApk(
                context = context,
                apkUrl = urlToUse,
                onProgress = { progress, downloaded, total ->
                    downloadProgress = progress
                    val downloadedMb = String.format("%.1f MB", downloaded / (1024.0 * 1024.0))
                    val totalMb = String.format("%.1f MB", total / (1024.0 * 1024.0))
                    downloadedBytesText = "$downloadedMb / $totalMb"
                }
            )
            isDownloading = false
            result.onSuccess {
                downloadComplete = true
            }.onFailure { e ->
                Toast.makeText(context, "Error al descargar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Actualizaciones", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { fetchUpdate(true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Buscar actualizaciones", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            if (isLoading || isChecking) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isChecking) "Comprobando actualizaciones..." else "Cargando información...",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            } else if (checkError != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No se pudo comprobar la actualización", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(checkError ?: "", color = Color.Gray, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { fetchUpdate(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reintentar", color = Color.White)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Tarjeta Principal de Estado de Versión
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF27272A), RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(if (hasNewVersion) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasNewVersion) {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = if (hasNewVersion) "¡Nueva versión lista!" else "Tu aplicación está al día",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Instalada: v$localVersionName (código $localVersionCode)",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            if (hasNewVersion && latestUpdate != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color(0xFF27272A))
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Disponible: v${latestUpdate!!.versionName}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    if (latestUpdate!!.securityPatch || latestUpdate!!.updateType.contains("Parche", true)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "Parche", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Código de versión: ${latestUpdate!!.versionCode} • Fecha: ${latestUpdate!!.releaseDate.ifBlank { "Reciente" }}",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 12.sp
                                )
                                if (latestUpdate!!.fileSize.isNotBlank()) {
                                    Text(
                                        text = "Tamaño del archivo: ${latestUpdate!!.fileSize}",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 12.sp
                                    )
                                }

                                if (latestUpdate!!.releaseNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Novedades:",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = latestUpdate!!.releaseNotes,
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Proceso de descarga o botón de actualización directa
                                if (isDownloading) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress },
                                            color = Color(0xFF3B82F6),
                                            trackColor = Color(0xFF27272A),
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${(downloadProgress * 100).toInt()}% descargando...",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = downloadedBytesText,
                                                color = Color(0xFFA1A1AA),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                } else if (downloadComplete) {
                                    Button(
                                        onClick = {
                                            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                                            val apkFile = File(downloadsDir, "update_latest.apk")
                                            if (apkFile.exists()) {
                                                updateRepository.installApk(context, apkFile)
                                            } else {
                                                Toast.makeText(context, "Archivo APK no encontrado", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.InstallMobile, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Instalar actualización ahora", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            val apkUrl = latestUpdate?.apkUrl ?: ""
                                            startDownload(apkUrl)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("1-Clic: Actualizar directamente", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECCIÓN DE MÉTODOS ALTERNATIVOS SIN CABLE USB
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF27272A), RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.UsbOff, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Actualizar SIN cable USB",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Descarga directa a tu teléfono por internet",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Opción 1: Abrir enlace en el Navegador (Chrome)
                            OutlinedButton(
                                onClick = {
                                    val apkUrl = latestUpdate?.apkUrl ?: ""
                                    if (apkUrl.isNotBlank()) {
                                        updateRepository.openInBrowser(context, apkUrl)
                                    } else {
                                        pendingActionAfterSave = "browser"
                                        showConfigUrlDialog = true
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F3F46)),
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Descargar desde Navegador (Chrome)", fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Opción 2: Compartir / Enviar enlace (WhatsApp / Drive / Telegram)
                            OutlinedButton(
                                onClick = {
                                    val apkUrl = latestUpdate?.apkUrl ?: ""
                                    if (apkUrl.isNotBlank()) {
                                        updateRepository.shareDownloadLink(context, apkUrl, latestUpdate?.versionName ?: "nueva")
                                    } else {
                                        pendingActionAfterSave = "share"
                                        showConfigUrlDialog = true
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F3F46)),
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Compartir enlace de descarga", fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Opción 3: Copiar enlace al portapapeles
                            OutlinedButton(
                                onClick = {
                                    val apkUrl = latestUpdate?.apkUrl ?: ""
                                    if (apkUrl.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(updateRepository.formatDirectDownloadUrl(apkUrl)))
                                        Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                    } else {
                                        pendingActionAfterSave = "copy"
                                        showConfigUrlDialog = true
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F3F46)),
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copiar enlace del APK", fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Botón para configurar / cambiar el enlace del APK
                            Button(
                                onClick = {
                                    pendingActionAfterSave = null
                                    showConfigUrlDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pegar o Cambiar URL del APK", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Guía clara paso a paso
                            Text(
                                text = "Instrucciones para tener tu APK sin cable:",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Sube tu archivo APK a Google Drive o Dropbox (enlace público) o copia el link de tu compilación.\n2. Toca 'Pegar o Cambiar URL del APK' y pega el enlace.\n3. ¡Listo! Pulsa '1-Clic: Actualizar directamente' o 'Descargar desde Navegador'. Tu teléfono se actualizará inmediatamente sin conectar ningún cable USB.",
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón para volver a comprobar
                    OutlinedButton(
                        onClick = { fetchUpdate(true) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Volver a comprobar actualizaciones", fontSize = 13.sp)
                    }
                }
            }
        }

        // Diálogo para ingresar / configurar el enlace del APK
        if (showConfigUrlDialog) {
            AlertDialog(
                onDismissRequest = { 
                    if (!isSavingUrl) showConfigUrlDialog = false 
                },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enlace de descarga del APK", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Pega aquí el enlace donde subiste el APK (Google Drive, Dropbox, GitHub Releases, MediaFire o cualquier enlace directo):\n\nSi usas Google Drive, asegúrate de poner el enlace como 'Cualquier persona con el enlace'. La aplicación lo convertirá automáticamente en descarga directa.",
                            color = Color(0xFFA1A1AA),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = inputApkUrl,
                            onValueChange = { inputApkUrl = it },
                            placeholder = { Text("https://drive.google.com/file/d/...", color = Color.Gray) },
                            label = { Text("URL del APK", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF3F3F46),
                                focusedContainerColor = Color(0xFF18181B),
                                unfocusedContainerColor = Color(0xFF18181B)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputApkUrl.isBlank()) {
                                Toast.makeText(context, "Por favor escribe o pega un enlace", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSavingUrl = true
                            scope.launch {
                                val saveResult = updateRepository.setApkUrl(inputApkUrl)
                                isSavingUrl = false
                                saveResult.onSuccess {
                                    Toast.makeText(context, "¡Enlace del APK guardado correctamente!", Toast.LENGTH_LONG).show()
                                    showConfigUrlDialog = false
                                    fetchUpdate(true)

                                    // Ejecutar la acción pendiente si existía
                                    when (pendingActionAfterSave) {
                                        "download" -> startDownload(inputApkUrl)
                                        "browser" -> updateRepository.openInBrowser(context, inputApkUrl)
                                        "share" -> updateRepository.shareDownloadLink(context, inputApkUrl, latestUpdate?.versionName ?: "nueva")
                                        "copy" -> {
                                            clipboardManager.setText(AnnotatedString(updateRepository.formatDirectDownloadUrl(inputApkUrl)))
                                            Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    pendingActionAfterSave = null
                                }.onFailure { e ->
                                    Toast.makeText(context, "Error al guardar enlace: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        enabled = !isSavingUrl
                    ) {
                        if (isSavingUrl) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Guardar y Continuar")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfigUrlDialog = false },
                        enabled = !isSavingUrl
                    ) {
                        Text("Cancelar", color = Color(0xFFA1A1AA))
                    }
                },
                containerColor = Color(0xFF1F2937),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}
