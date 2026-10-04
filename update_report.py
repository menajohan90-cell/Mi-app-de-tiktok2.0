import re

with open("app/src/main/java/com/example/data/PostRepository.kt", "r") as f:
    content = f.read()

old_report = """    suspend fun reportPost(videoId: String, reason: String, description: String? = null): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val reportId = UUID.randomUUID().toString()
            val reportData = mapOf(
                "reportId" to reportId,
                "videoId" to videoId,
                "reporterUid" to currentUid,
                "reason" to reason,
                "description" to description,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("reports").document(reportId).set(reportData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }"""

new_report = """    suspend fun reportPost(videoId: String, reason: String, description: String? = null): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val reportId = UUID.randomUUID().toString()
            val reportData = mapOf(
                "reportId" to reportId,
                "videoId" to videoId,
                "reporterUid" to currentUid,
                "reason" to reason,
                "description" to description,
                "createdAt" to System.currentTimeMillis(),
                "status" to "ANALYZING"
            )
            db.collection("reports").document(reportId).set(reportData).await()
            
            // Notify user
            val notifId = UUID.randomUUID().toString()
            db.collection("system_notifications").document(notifId).set(mapOf(
                "id" to notifId,
                "uid" to currentUid,
                "title" to "Nuevo reporte recibido",
                "message" to "Se ha reportado un video por \\"$reason\\". Estado: En análisis.",
                "createdAt" to System.currentTimeMillis(),
                "type" to "REPORT_RECEIVED"
            )).await()
            
            // Background analysis
            GlobalScope.launch(Dispatchers.IO) {
                delay(3000)
                // Simulate analysis
                db.collection("reports").document(reportId).update("status", "NEEDS_REVIEW", "confidence", 0.8).await()
                val notifId2 = UUID.randomUUID().toString()
                db.collection("system_notifications").document(notifId2).set(mapOf(
                    "id" to notifId2,
                    "uid" to currentUid,
                    "title" to "Análisis completado",
                    "message" to "El reporte del video fue analizado por el sistema de seguridad. Resultado: Requiere revisión.",
                    "createdAt" to System.currentTimeMillis(),
                    "type" to "REPORT_REVIEWED"
                )).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }"""

content = content.replace(old_report, new_report)

with open("app/src/main/java/com/example/data/PostRepository.kt", "w") as f:
    f.write(content)
