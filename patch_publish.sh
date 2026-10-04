sed -i 's/var isUploading by remember { mutableStateOf(false) }/var isUploading by remember { mutableStateOf(false) }\n    var uploadError by remember { mutableStateOf("") }/g' app/src/main/java/com/example/ui/screens/PublishScreen.kt

sed -i 's/if (isUploading) {/if (uploadError.isNotEmpty()) {\n                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {\n                        Text(uploadError, color = Color(0xFFEF4444), modifier = Modifier.padding(bottom = 16.dp))\n                        Button(onClick = { uploadError = "" }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("Reintentar", color = Color.White) }\n                    }\n                } else if (isUploading) {/g' app/src/main/java/com/example/ui/screens/PublishScreen.kt

sed -i 's/android.util.Log.e("PublishScreen", "Upload failed", task.exception)/android.util.Log.e("PublishScreen", "Upload failed", task.exception)/g' app/src/main/java/com/example/ui/screens/PublishScreen.kt

sed -i 's/Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()/uploadError = "Error: ${task.exception?.message ?: "No se pudo subir. Comprueba tu conexión."}"/g' app/src/main/java/com/example/ui/screens/PublishScreen.kt
