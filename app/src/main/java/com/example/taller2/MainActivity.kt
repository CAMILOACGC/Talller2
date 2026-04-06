package com.example.taller2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taller2.model.ChatMessage
import com.example.taller2.model.GameRoom
import com.example.taller2.model.Player
import com.example.taller2.ui.theme.Taller2Theme
import com.example.taller2.viewmodel.GameViewModel

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
    val gameRoom by viewModel.gameRoom.collectAsState()
    val currentPlayerId by viewModel.currentPlayerId.collectAsState()
    var playerName by remember { mutableStateOf("") }
    var roomId by remember { mutableStateOf("room1") }

    if (currentPlayerId == null) {
        JoinScreen(
            playerName = playerName,
            onPlayerNameChange = { playerName = it },
            roomId = roomId,
            onRoomIdChange = { roomId = it },
            onJoin = { viewModel.joinRoom(roomId, playerName) }
        )
    } else {
        if (gameRoom == null) {
            // Pantalla de carga para que no se vea negro
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Conectando a la sala...")
                    Text("Verifica tu conexión a Firebase en el Logcat", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        } else {
            GameScreen(gameRoom!!, currentPlayerId!!, viewModel)
        }
    }
}

@Composable
fun JoinScreen(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    roomId: String,
    onRoomIdChange: (String) -> Unit,
    onJoin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tío Rico - Modo Supervivencia", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        TextField(
            value = playerName,
            onValueChange = onPlayerNameChange,
            label = { Text("Tu nombre") },
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
    }
}

@Composable
fun GameScreen(room: GameRoom, currentPlayerId: String, viewModel: GameViewModel) {
    val currentPlayer = room.players[currentPlayerId] ?: return
    val messages by viewModel.messages.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Game Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Turno: ${room.currentTurn}/10", fontWeight = FontWeight.Bold)
            Text("Dinero: $${currentPlayer.money}", color = if (currentPlayer.money > 0) Color.Green else Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("Estado: ${room.status}", style = MaterialTheme.typography.bodySmall)

        if (room.status == "WAITING") {
            Button(onClick = { viewModel.startGame(room.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Empezar Juego")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Players List
        Text("Jugadores:", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.height(100.dp)) {
            items(room.players.values.toList()) { player ->
                Text("${player.name}: $${player.money} ${if (!player.isAlive) "(ELIMINADO)" else ""}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
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
        } else if (!currentPlayer.isAlive) {
            Text("ESTÁS ELIMINADO", color = Color.Red, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Chat
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
