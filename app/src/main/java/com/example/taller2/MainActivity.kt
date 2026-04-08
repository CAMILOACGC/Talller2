package com.example.taller2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taller2.model.ChatMessage
import com.example.taller2.model.GameRoom
import com.example.taller2.ui.theme.Taller2Theme
import com.example.taller2.viewmodel.GameViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Taller2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun GameApp(viewModel: GameViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val gameRoom by viewModel.gameRoom.collectAsState()
    val currentPlayerId by viewModel.currentPlayerId.collectAsState()
    var playerName by remember { mutableStateOf("") }
    var roomId by remember { mutableStateOf("room1") }

    if (currentUser == null) {
        AuthScreen(viewModel)
    } else if (currentPlayerId == null) {
        JoinScreen(
            playerName = playerName,
            onPlayerNameChange = { playerName = it },
            roomId = roomId,
            onRoomIdChange = { roomId = it },
            onJoin = { viewModel.joinRoom(roomId, playerName) },
            onSignOut = { viewModel.signOut() }
        )
    } else {
        if (gameRoom == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Conectando a la sala...")
                }
            }
        } else {
            GameScreen(gameRoom!!, currentPlayerId!!, viewModel)
        }
    }
}

@Composable
fun AuthScreen(viewModel: GameViewModel) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authError by viewModel.authError.collectAsState()

    // Configuración de Google Sign-In usando el Client ID del JSON
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("710428830847-nb1rfhgvjbrgh9gtd179phecu9mtvcqj.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { viewModel.signInWithGoogle(it) {} }
        } catch (e: ApiException) {
            Log.e("Auth", "Google sign in failed code: ${e.statusCode}")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tío Rico - Autenticación", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        if (authError != null) {
            Text(authError!!, color = Color.Red, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        val canAuth = email.isNotBlank() && password.length >= 6
        
        Button(
            onClick = { viewModel.signIn(email, password) {} },
            modifier = Modifier.fillMaxWidth(),
            enabled = canAuth
        ) {
            Text("Iniciar Sesión")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.signUp(email, password) {} },
            modifier = Modifier.fillMaxWidth(),
            enabled = canAuth
        ) {
            Text("Registrarse")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { launcher.launch(googleSignInClient.signInIntent) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
        ) {
            Text("Continuar con Google", color = Color.White)
        }
        
        if (password.isNotEmpty() && password.length < 6) {
            Text("Mínimo 6 caracteres", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun JoinScreen(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    roomId: String,
    onRoomIdChange: (String) -> Unit,
    onJoin: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bienvenido al Juego", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        TextField(
            value = playerName,
            onValueChange = onPlayerNameChange,
            label = { Text("Tu nombre de jugador") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = roomId,
            onValueChange = onRoomIdChange,
            label = { Text("ID de la sala") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onJoin,
            enabled = playerName.isNotBlank() && roomId.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar a la Sala")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onSignOut) {
            Text("Cerrar Sesión")
        }
    }
}

@Composable
fun GameScreen(room: GameRoom, currentPlayerId: String, viewModel: GameViewModel) {
    val currentPlayer = room.players[currentPlayerId] ?: return
    val messages by viewModel.messages.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Turno: ${room.currentTurn}/10", fontWeight = FontWeight.Bold)
            Text("Dinero: $${currentPlayer.money}", color = if (currentPlayer.money > 0) Color.Green else Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Estado: ${room.status}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { viewModel.signOut() }) {
                Text("Salir", color = Color.Gray)
            }
        }

        if (room.status == "WAITING") {
            Button(onClick = { viewModel.startGame(room.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Empezar Juego")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Jugadores:", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.height(100.dp)) {
            items(room.players.values.toList()) { player ->
                Text("${player.name}: $${player.money} ${if (!player.isAlive) "(ELIMINADO)" else ""}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (room.status == "PLAYING" && currentPlayer.isAlive) {
            if (currentPlayer.turnPlayed < room.currentTurn) {
                Text("Elige tu acción para este turno:")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { viewModel.performAction(room.id, currentPlayerId, "SAVE") }) { Text("Ahorrar") }
                    Button(onClick = { viewModel.performAction(room.id, currentPlayerId, "INVEST") }) { Text("Invertir") }
                    Button(onClick = { viewModel.performAction(room.id, currentPlayerId, "SPEND") }) { Text("Gastar") }
                }
            } else {
                Text("Esperando a otros jugadores...", color = Color.Gray)
            }
        } else if (room.status == "FINISHED") {
            val won = currentPlayer.isAlive && currentPlayer.money > 0
            Text(if (won) "¡HAS GANADO EL JUEGO!" else "HAS PERDIDO EL JUEGO", 
                color = if (won) Color.Green else Color.Red, 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            
            Button(onClick = { viewModel.restartGame(room.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Reiniciar Juego")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        ChatSection(messages = messages, onSendMessage = { text ->
            viewModel.sendMessage(room.id, text, currentPlayer.name)
        })
    }
}

@Composable
fun ChatSection(messages: List<ChatMessage>, onSendMessage: (String) -> Unit) {
    var chatText by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.LightGray.copy(alpha = 0.2f)).padding(8.dp)) {
        Text("Chat de la Partida", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                Text("${msg.senderName}: ${msg.text}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = chatText,
                onValueChange = { chatText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje...") }
            )
            IconButton(onClick = {
                if (chatText.isNotBlank()) {
                    onSendMessage(chatText)
                    chatText = ""
                }
            }) {
                Text("Send")
            }
        }
    }
}
