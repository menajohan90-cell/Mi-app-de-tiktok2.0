package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.auth.AuthRepository
import com.example.auth.AuthResult
import com.example.auth.GoogleAuthService
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController, onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var forgotLoading by remember { mutableStateOf(false) }
    var forgotMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    val authService = remember { GoogleAuthService(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Decorative background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A),
                            Color.Black
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MOMENTOS",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 4.sp
            )
            
            Text(
                text = "Inicia sesión en tu cuenta",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            if (errorMessage.isNotEmpty()) {
                Surface(
                    color = Color(0x33EF4444),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            AuthTextField(
                value = identifier,
                onValueChange = { 
                    identifier = it
                    if (errorMessage.isNotEmpty()) errorMessage = ""
                },
                label = "Correo o Usuario",
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { 
                    password = it
                    if (errorMessage.isNotEmpty()) errorMessage = ""
                },
                label = "Contraseña",
                icon = Icons.Default.Lock,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
            )

            Spacer(modifier = Modifier.height(32.dp))

            GradientButton(
                text = "Iniciar Sesión",
                isLoading = isLoading,
                onClick = {
                    if (isLoading || isGoogleLoading) return@GradientButton
                    if (identifier.isBlank() || password.isBlank()) {
                        errorMessage = "Completa todos los campos."
                        return@GradientButton
                    }
                    
                    isLoading = true
                    errorMessage = ""
                    scope.launch {
                        val result = authRepository.login(identifier, password)
                        isLoading = false
                        if (result is AuthResult.SuccessExistingUser || result is AuthResult.SuccessNewUser) {
                            onAuthSuccess()
                        } else if (result is AuthResult.Error) {
                            errorMessage = result.message
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF333333))
                Text(" o ", color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF333333))
            }

            Spacer(modifier = Modifier.height(24.dp))

            GoogleSignInButton(
                isLoading = isGoogleLoading,
                onClick = {
                    isGoogleLoading = true
                    errorMessage = ""
                    scope.launch {
                        val result = authService.signInWithGoogle()
                        isGoogleLoading = false
                        if (result is AuthResult.SuccessNewUser || result is AuthResult.SuccessExistingUser) {
                            onAuthSuccess()
                        } else if (result is AuthResult.Error) {
                            errorMessage = result.message
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "¿Olvidaste tu contraseña?",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.clickable { 
                    forgotMessage = ""
                    showForgotDialog = true 
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Text("¿No tienes cuenta? ", color = Color.Gray)
                Text(
                    "Regístrate",
                    color = Color(0xFFE50914),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { navController.navigate("register") }
                )
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { if (!forgotLoading) showForgotDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Recuperar Contraseña", color = Color.White) },
            text = {
                Column {
                    Text(
                        "Ingresa tu correo electrónico para recibir un enlace de recuperación.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    AuthTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = "Correo electrónico",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )
                    if (forgotMessage.isNotEmpty()) {
                        Text(
                            text = forgotMessage,
                            color = if (forgotMessage.contains("Error")) Color(0xFFEF4444) else Color(0xFF10B981),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (forgotEmail.isBlank()) {
                            forgotMessage = "Ingresa tu correo."
                            return@TextButton
                        }
                        forgotLoading = true
                        scope.launch {
                            val result = authRepository.forgotPassword(forgotEmail)
                            forgotLoading = false
                            if (result is AuthResult.SuccessExistingUser || result is AuthResult.SuccessNewUser) {
                                forgotMessage = "Correo enviado. Revisa tu bandeja de entrada."
                            } else if (result is AuthResult.Error) {
                                forgotMessage = result.message
                            }
                        }
                    },
                    enabled = !forgotLoading
                ) {
                    if (forgotLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Enviar", color = Color(0xFFE50914))
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }, enabled = !forgotLoading) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun RegisterScreen(navController: NavController, onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    val authService = remember { GoogleAuthService(context) }

    // Password strength logic
    val isPasswordWeak = remember(password) {
        password.length >= 8 && (
            password.all { it.isDigit() } || 
            password.all { it.isLetter() } || 
            password.all { !it.isLetterOrDigit() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Únete a Momentos",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Crea una cuenta para empezar",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF4444),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Correo electrónico",
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = username,
                onValueChange = { username = it },
                label = "Nombre de usuario",
                icon = Icons.Default.AlternateEmail
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Contraseña",
                icon = Icons.Default.Lock,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
            )

            if (isPasswordWeak) {
                Text(
                    text = "Esta contraseña es débil. Puedes usarla, pero recomendamos una contraseña más segura.",
                    color = Color(0xFFFBBF24), // Amber color for warning
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp).align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirmar contraseña",
                icon = Icons.Default.Lock,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
            )

            Spacer(modifier = Modifier.height(32.dp))

            GradientButton(
                text = "Crear Cuenta",
                isLoading = isLoading,
                onClick = {
                    if (email.isBlank() || username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                        errorMessage = "Completa todos los campos."
                        return@GradientButton
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Las contraseñas no coinciden."
                        return@GradientButton
                    }
                    
                    isLoading = true
                    errorMessage = ""
                    scope.launch {
                        val result = authRepository.register(email, username, password)
                        isLoading = false
                        if (result is AuthResult.SuccessNewUser || result is AuthResult.SuccessExistingUser) {
                            onAuthSuccess()
                        } else if (result is AuthResult.Error) {
                            errorMessage = result.message
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF333333))
                Text(" o ", color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF333333))
            }

            Spacer(modifier = Modifier.height(24.dp))

            GoogleSignInButton(
                isLoading = isGoogleLoading,
                onClick = {
                    isGoogleLoading = true
                    errorMessage = ""
                    scope.launch {
                        val result = authService.signInWithGoogle()
                        isGoogleLoading = false
                        if (result is AuthResult.SuccessNewUser || result is AuthResult.SuccessExistingUser) {
                            onAuthSuccess()
                        } else if (result is AuthResult.Error) {
                            errorMessage = result.message
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Text("¿Ya tienes cuenta? ", color = Color.Gray)
                Text(
                    "Inicia sesión",
                    color = Color(0xFFE50914),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "googleButtonScale"
    )

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White,
            containerColor = Color.Transparent
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continuar con Google",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            if (isPassword && onPasswordToggle != null) {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        singleLine = true,
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color(0xFF333333),
            focusedContainerColor = Color(0xFF121212),
            unfocusedContainerColor = Color(0xFF121212)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun GradientButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        enabled = !isLoading,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isLoading) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE50914).copy(alpha = 0.6f),
                                Color(0xFFFF3D00).copy(alpha = 0.6f)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE50914),
                                Color(0xFFFF3D00)
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
