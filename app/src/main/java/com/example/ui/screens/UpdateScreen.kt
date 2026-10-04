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
import androidx.compose.ui.text.style.TextAlign
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
    val updateRepository = remember { UpdateRepository() }

    // RUTA DE TU REPOSITORIO
    val GITHUB_REPO = "menajohan90-cell/Mi-app-de-tiktok2.0"

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

    // GitHub Sync State
    var githubApkUrl by remember { mutableStateOf<String?>(null) }
    var isCheckingGithub by remember { mutableStateOf(false) }
    
    // Dialog State
    var showUpdateConfirmDialog by remember { mutableStateOf(false) }
    var selectedApkUrl by remember { mutableStateOf("") }

    fun startDownload(urlToUse: String) {
        if (urlToUse.isBlank()) {
            Toast.makeText(context, "URL no válida", Toast.LENGTH_SHORT).show()
            return
        }
        isDownloading = true
        downloadComplete = false
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
                Toast.makeText(context, "Descarga completada.", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun fetchUpdates() {
        scope.launch {
            isChecking = true
            checkError = null
            
            // Check Firebase
            updateRepository.getLatestUpdate().onSuccess { update ->
                latestUpdate = update
            }

            // Check GitHub
            isCheckingGithub = true
            updateRepository.getGitHubLatestRelease(GITHUB_REPO).onSuccess { url ->
                githubApkUrl = url
            }.onFailure {
                githubApkUrl = null
            }
            
            isCheckingGithub = false
            isChecking = false
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        fetchUpdates()
    }

    val hasGithubUpdate = githubApkUrl != null
    val remoteCode = latestUpdate?.versionCode ?: localVersionCode
    val hasFirebaseUpdate = latestUpdate != null && remoteCode > localVersionCode
    val anyUpdateFound = hasGithubUpdate || hasFirebaseUpdate
    
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Centro de Instalación", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { fetchUpdates() }) {
                        Icon(if (isChecking) Icons.Default.Sync else Icons.Default.Refresh, contentDescription = "Sincronizar", tint = Color(0xFF22C55E))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF22C55E))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- CABECERA DE ESTADO ---
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(if (anyUpdateFound) Color(0xFF22C55E).copy(alpha = 0.1f) else Color(0xFF3F3F46).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (anyUpdateFound) Icons.Default.SystemUpdate else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (anyUpdateFound) Color(0xFF22C55E) else Color.Gray,
                            modifier = Modifier.size(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (anyUpdateFound) "¡Actualización Encontrada!" else "No se encontró actualización o parche de seguridad",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (anyUpdateFound) "Hay una nueva versión lista para instalar en tu dispositivo." else "Tu aplicación Momentos ya cuenta con todas las novedades y parches instalados.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // --- ACCIONES SI HAY ACTUALIZACIÓN ---
                    if (anyUpdateFound) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF27272A), RoundedCornerShape(24.dp))
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Información de la entrega", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (hasGithubUpdate) {
                                    UpdateItemRow("Fuente", "GitHub (Auto-Sync)")
                                    UpdateItemRow("Estado", "Lista para instalar")
                                } else {
                                    UpdateItemRow("Versión", latestUpdate?.versionName ?: "Nueva")
                                    UpdateItemRow("Tipo", latestUpdate?.updateType ?: "Oficial")
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))

                                if (isDownloading) {
                                    DownloadProgressView(downloadProgress, downloadedBytesText)
                                } else if (downloadComplete) {
                                    Button(
                                        onClick = {
                                            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                                            val apkFile = File(downloadsDir, "update_latest.apk")
                                            updateRepository.installApk(context, apkFile)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(Icons.Default.InstallMobile, contentDescription = null)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("INSTALAR AHORA", fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { 
                                            selectedApkUrl = githubApkUrl ?: latestUpdate?.apkUrl ?: ""
                                            showUpdateConfirmDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("ACTUALIZAR E INSTALAR", fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // --- BOTÓN DE REINTENTAR SI NO HAY NADA ---
                        OutlinedButton(
                            onClick = { fetchUpdates() },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
                            enabled = !isChecking
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Comprobar de nuevo", color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- PIE DE PÁGINA ---
                    Text(
                        text = "Versión actual: v$localVersionName (código $localVersionCode)",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // DIÁLOGO DE CONFIRMACIÓN
        if (showUpdateConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showUpdateConfirmDialog = false },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color(0xFF22C55E))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Confirmar Instalación", color = Color.White)
                    }
                },
                text = {
                    Text(
                        "Se procederá a descargar e instalar el nuevo parche de seguridad o actualización. ¿Deseas continuar?",
                        color = Color.LightGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUpdateConfirmDialog = false
                            startDownload(selectedApkUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                    ) {
                        Text("Actualizar Ahora")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateConfirmDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF18181B),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}

@Composable
fun UpdateItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun DownloadProgressView(progress: Float, text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { progress },
            color = Color(0xFF22C55E),
            trackColor = Color(0xFF27272A),
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${(progress * 100).toInt()}% descargando...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text, color = Color.Gray, fontSize = 12.sp)
        }
    }
}
