package com.example.taller2.ui

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
import com.example.taller2.viewmodel.AuthViewModel
import com.example.taller2.viewmodel.GameViewModel

@Composable
fun GameScreen(room: GameRoom, currentPlayerId: String, gameViewModel: GameViewModel, authViewModel: AuthViewModel) {
    val currentPlayer = room.players[currentPlayerId] ?: return
    val messages by gameViewModel.messages.collectAsState()

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
            TextButton(onClick = { authViewModel.signOut { gameViewModel.leaveRoom() } }) {
                Text("Salir", color = Color.Gray)
            }
        }

        if (room.status == "WAITING") {
            if (room.hostId == currentPlayerId) {
                Button(onClick = { gameViewModel.startGame(room.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Empezar Juego")
                }
            } else {
                Text("Esperando a que el host inicie la partida...", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
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
                    Button(onClick = { gameViewModel.performAction(room.id, currentPlayerId, "SAVE") }) { Text("Ahorrar") }
                    Button(onClick = { gameViewModel.performAction(room.id, currentPlayerId, "INVEST") }) { Text("Invertir") }
                    Button(onClick = { gameViewModel.performAction(room.id, currentPlayerId, "SPEND") }) { Text("Gastar") }
                }
            } else {
                Text("Esperando a otros jugadores...", color = Color.Gray)
            }
        } else if (room.status == "FINISHED") {
            val players = room.players.values.toList()
            
            // Ordenamos a los jugadores:
            // 1. Mayor dinero primero.
            // 2. Si hay empate, usamos el ID (menor ID primero) para garantizar un único ganador/perdedor.
            val sortedPlayers = players.sortedWith(compareByDescending<Player> { it.money }.thenBy { it.id })
            
            val winner = sortedPlayers.firstOrNull()
            // El perdedor es el último de la lista (el que menos dinero tiene o el que tiene mayor ID en empate)
            val loser = if (sortedPlayers.size > 1) sortedPlayers.lastOrNull() else null
            
            val isWinner = currentPlayer.id == winner?.id
            val isLoser = currentPlayer.id == loser?.id
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        isWinner -> "¡HAS GANADO EL JUEGO!"
                        isLoser -> "HAS PERDIDO EL JUEGO"
                        else -> "PARTIDA FINALIZADA"
                    },
                    color = when {
                        isWinner -> Color.Green
                        isLoser -> Color.Red
                        else -> Color.Gray
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isWinner) {
                    Text("¡Felicidades! Eres el jugador con mayor capital ($${currentPlayer.money})", fontSize = 16.sp)
                } else if (isLoser) {
                    if (currentPlayer.money <= 0) {
                        Text("Has quedado en quiebra ($${currentPlayer.money})", fontSize = 16.sp)
                    } else {
                        Text("Has perdido por tener el menor capital ($${currentPlayer.money})", fontSize = 16.sp)
                    }
                } else {
                    Text("Has terminado con un capital de $${currentPlayer.money}", fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cuadro de Honor:", fontWeight = FontWeight.Bold)
                        Text("🏆 Ganador: ${winner?.name} ($${winner?.money})", color = Color(0xFF388E3C))
                        if (loser != null && loser.id != winner?.id) {
                            Text("💀 Perdedor: ${loser.name} ($${loser.money})", color = Color.Red)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (room.hostId == currentPlayerId) {
                Button(onClick = { gameViewModel.restartGame(room.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Reiniciar Juego")
                }
            } else {
                Text("Esperando a que el host reinicie la partida...", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        ChatSection(messages = messages, onSendMessage = { text ->
            gameViewModel.sendMessage(room.id, text, currentPlayer.name)
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
