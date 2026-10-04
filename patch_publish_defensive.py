import re

with open("app/src/main/java/com/example/ui/screens/PublishScreen.kt", "r") as f:
    content = f.read()

validation_check = """                            val contentResolver = context.contentResolver
                            
                            // Validar que el archivo existe y tiene tamaño
                            try {
                                val cursor = contentResolver.query(selectedVideoUri!!, null, null, null, null)
                                if (cursor != null && cursor.moveToFirst()) {
                                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                    if (sizeIndex != -1) {
                                        val size = cursor.getLong(sizeIndex)
                                        if (size == 0L) {
                                            isUploading = false
                                            uploadError = "El archivo de video seleccionado está vacío (0 bytes)."
                                            cursor.close()
                                            return@Button
                                        }
                                    }
                                    cursor.close()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("PublishScreen", "Error reading video URI", e)
                            }
                            
                            val mimeType = contentResolver.getType(selectedVideoUri!!) ?: "video/mp4"
"""

content = content.replace("                            val contentResolver = context.contentResolver\n                            val mimeType = contentResolver.getType(selectedVideoUri!!) ?: \"video/mp4\"", validation_check)

with open("app/src/main/java/com/example/ui/screens/PublishScreen.kt", "w") as f:
    f.write(content)
