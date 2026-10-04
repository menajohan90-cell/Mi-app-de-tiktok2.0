package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PostRepository
import com.example.data.ReportModel
import com.example.utils.SvgFlag
import com.example.utils.SvgInfo
import com.example.utils.SvgSuccess
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: androidx.navigation.NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val postRepository = remember { PostRepository() }
    
    var reports by remember { mutableStateOf<List<ReportModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    fun loadReports() {
        scope.launch {
            isLoading = true
            val result = postRepository.getReports()
            result.onSuccess {
                reports = it
                isLoading = false
            }.onFailure {
                errorMessage = it.message ?: "Error al cargar denuncias"
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        val user = auth.currentUser
        val listener = if (user != null) {
            db.collection("reports")
                .whereEqualTo("reporterUid", user.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { it.toObject(ReportModel::class.java) }
                            .sortedByDescending { it.createdAt }
                        reports = list
                        isLoading = false
                    }
                }
        } else null

        onDispose {
            listener?.remove()
        }
    }

    LaunchedEffect(Unit) {
        loadReports()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Denuncias (Sistema)", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        SvgFlag(tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage.isNotEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage, color = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { loadReports() }) { Text("Reintentar") }
                }
            } else if (reports.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SvgFlag(tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No tienes denuncias registradas.", color = Color.Gray, fontSize = 15.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(reports, key = { it.reportId }) { report ->
                        ReportCard(report = report, onClick = {
                            navController.navigate("report_detail/${report.reportId}")
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCard(report: ReportModel, onClick: () -> Unit) {
    val statusText = when (report.status) {
        "reviewing_report" -> "El sistema está revisando tu denuncia. Espere por favor."
        "reviewing_publication" -> "El sistema está revisando la publicación."
        "resolved" -> if (report.systemResult == "accepted") "Denuncia aceptada" else "Denuncia rechazada"
        else -> report.systemAnalysis
    }

    val statusBadgeColor = when (report.status) {
        "resolved" -> if (report.systemResult == "accepted") Color(0xFF10B981) else Color(0xFF3B82F6)
        else -> Color(0xFFEF4444)
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(statusBadgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                SvgFlag(tint = statusBadgeColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Motivo: ${report.reason}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = android.text.format.DateUtils.getRelativeTimeSpanString(report.createdAt).toString(),
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
