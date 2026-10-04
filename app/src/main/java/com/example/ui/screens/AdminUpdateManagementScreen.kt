package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UpdateRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUpdateManagementScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateRepository = remember { UpdateRepository() }

    var versionName by remember { mutableStateOf("1.4.2") }
    var versionCode by remember { mutableStateOf("142") }
    var updateType by remember { mutableStateOf("Actualización oficial") }
    var securityPatch by remember { mutableStateOf(false) }
    var fileSize by remember { mutableStateOf("16.0 MB") }
    var releaseNotes by remember { mutableStateOf("Mejoras de rendimiento, corrección de chat y actualización inalámbrica.") }
    var minimumVersion by remember { mutableStateOf("100") }
    var mandatory by remember { mutableStateOf(false) }
    var directApkUrl by remember { mutableStateOf("") }
    var apkUri by remember { mutableStateOf<Uri?>(null) }
    var apkFileName by remember { mutableStateOf("Ningún archivo APK seleccionado") }
    var isPublishing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val latest = updateRepository.getLatestUpdate().getOrNull()
        if (latest != null) {
            versionName = latest.versionName
            versionCode = (latest.versionCode + 1).toString()
            directApkUrl = latest.apkUrl
            releaseNotes = latest.releaseNotes
            fileSize = latest.fileSize
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            apkUri = uri
            apkFileName = uri.lastPathSegment ?: "apk_seleccionado.apk"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Actualizaciones", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Publicar Nueva Versión en Firebase",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text("Version Name (ej: 1.4.2)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF3B82F6))
                )

                OutlinedTextField(
                    value = versionCode,
                    onValueChange = { versionCode = it },
                    label = { Text("Version Code (ej: 142)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF3B82F6))
                )

                OutlinedTextField(
                    value = updateType,
                    onValueChange = { updateType = it },
                    label = { Text("Tipo (ej: Actualización oficial, Parche de seguridad)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF3B82F6))
                )

                OutlinedTextField(
                    value = fileSize,
                    onValueChange = { fileSize = it },
                    label = { Text("Tamaño del APK (ej: 16.0 MB)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF3B82F6))
                )

                OutlinedTextField(
                    value = minimumVersion,
                    onValueChange = { minimumVersion = it },
                    label = { Text("Minimum Version Code", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF3B82F6))
                )

                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text("Novedades y notas de la versión", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF3B82F6))
                )

                // Enlace directo del APK (Google Drive, Dropbox, MediaFire, etc.)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF3B82F6))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Opción 1: Enlace directo del APK (Recomendado)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Pega aquí el enlace de tu APK (de Google Drive, Dropbox, GitHub Releases, o tu servidor). Si es de Google Drive, la app lo convertirá automáticamente en enlace de descarga directa.",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = directApkUrl,
                            onValueChange = { directApkUrl = it },
                            placeholder = { Text("https://drive.google.com/file/d/...", color = Color.Gray) },
                            label = { Text("URL Directo del APK", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF3B82F6))
                        )
                    }
                }

                // Opción 2: Subir archivo APK desde el dispositivo
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Opción 2: Subir APK a Firebase Storage", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(apkFileName, color = Color.Gray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("application/vnd.android.package-archive") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Seleccionar archivo APK del celular")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Es Parche de Seguridad?", color = Color.White, fontSize = 15.sp)
                    Switch(
                        checked = securityPatch,
                        onCheckedChange = { securityPatch = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF59E0B))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Actualización obligatoria?", color = Color.White, fontSize = 15.sp)
                    Switch(
                        checked = mandatory,
                        onCheckedChange = { mandatory = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF3B82F6))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val vCode = versionCode.toIntOrNull()
                        val mVersion = minimumVersion.toIntOrNull()
                        if (vCode == null || mVersion == null) {
                            Toast.makeText(context, "Version Code y Minimum Version deben ser números enteros", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (directApkUrl.isBlank() && apkUri == null) {
                            Toast.makeText(context, "Pega un enlace del APK o selecciona un archivo", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isPublishing = true
                        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                        scope.launch {
                            val result = updateRepository.publishUpdate(
                                versionName = versionName,
                                versionCode = vCode,
                                releaseNotes = releaseNotes,
                                releaseDate = currentDate,
                                mandatory = mandatory,
                                minimumVersion = mVersion,
                                fileSize = fileSize,
                                updateType = updateType,
                                securityPatch = securityPatch,
                                apkUri = apkUri,
                                directApkUrl = directApkUrl,
                                context = context
                            )
                            isPublishing = false
                            result.onSuccess {
                                Toast.makeText(context, "¡Actualización publicada con éxito en Firebase!", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }.onFailure { e ->
                                Toast.makeText(context, "Error al publicar: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isPublishing
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Publicar en Firebase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
