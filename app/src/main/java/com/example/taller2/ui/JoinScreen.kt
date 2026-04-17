package com.example.taller2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
