import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:google_sign_in/google_sign_in.dart';

// IMPORTANTE: Asegúrate de haber ejecutado 'flutterfire configure' 
// en tu terminal para que se genere este archivo.
import 'firebase_options.dart'; 

void main() async {
  // 1. Asegurar la inicialización de los bindings de Flutter antes de código nativo
  WidgetsFlutterBinding.ensureInitialized();
  
  // 2. Inicializar Firebase con las opciones específicas de la plataforma
  // Esto previene el 90% de los errores de "cliente desconectado"
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
  
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Login Firebase',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const LoginScreen(),
    );
  }
}

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool _isLoading = false;

  Future<void> signInWithGoogle(BuildContext context) async {
    setState(() => _isLoading = true);

    try {
      // 1. Disparar el flujo de autenticación de Google
      final GoogleSignInAccount? googleUser = await GoogleSignIn().signIn();
      if (googleUser == null) {
        setState(() => _isLoading = false);
        return; // El usuario canceló
      }

      // 2. Obtener credenciales de Google
      final GoogleSignInAuthentication googleAuth = await googleUser.authentication;
      final credential = GoogleAuthProvider.credential(
        accessToken: googleAuth.accessToken,
        idToken: googleAuth.idToken,
      );

      // 3. Autenticar en Firebase
      UserCredential userCredential = await FirebaseAuth.instance.signInWithCredential(credential);
      User? user = userCredential.user;

      if (user != null) {
        // 4. Guardar datos en Firestore usando merge: true
        await _saveUserToFirestore(user);

        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('¡Bienvenido, ${user.displayName}!'),
              backgroundColor: Colors.green,
            ),
          );
          // Aquí puedes hacer tu Navigator.pushReplacement(...)
        }
      }
    } on FirebaseAuthException catch (e) {
      _showError(context, 'Error de autenticación: ${e.message}');
    } on FirebaseException catch (e) {
      if (e.code == 'unavailable') {
        _showError(context, 'Sin conexión a la base de datos (Offline).');
      } else {
        _showError(context, 'Error de Firestore: ${e.message}');
      }
    } catch (e) {
      _showError(context, 'Ocurrió un error inesperado: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  /// Función separada y limpia para manejar Firestore
  Future<void> _saveUserToFirestore(User user) async {
    // Referencia al documento del usuario
    final docRef = FirebaseFirestore.instance.collection('users').doc(user.uid);
    
    final userData = {
      'uid': user.uid,
      'email': user.email ?? '',
      'nombre': user.displayName ?? 'Usuario',
      'foto': user.photoURL ?? '',
      'ultimoAcceso': FieldValue.serverTimestamp(),
    };

    // SetOptions(merge: true) actualiza los campos declarados arriba 
    // pero NO borra datos antiguos (como 'fechaCreacion' o 'rol') si el doc ya existía.
    // Usamos .timeout para evitar que la app se quede colgada infinitamente si no hay internet real.
    await docRef.set(
      userData, 
      SetOptions(merge: true)
    ).timeout(
      const Duration(seconds: 10),
      onTimeout: () {
        throw FirebaseException(
          plugin: 'cloud_firestore',
          code: 'unavailable',
          message: 'Tiempo de espera agotado al conectar con el servidor.',
        );
      },
    );
  }

  void _showError(BuildContext context, String message) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message), backgroundColor: Colors.red),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Login con Google')),
      body: Center(
        child: _isLoading
            ? const CircularProgressIndicator()
            : ElevatedButton.icon(
                icon: const Icon(Icons.login),
                label: const Text('Iniciar sesión con Google'),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                ),
                onPressed: () => signInWithGoogle(context),
              ),
      ),
    );
  }
}