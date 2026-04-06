package com.example.taller2.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.taller2.logic.GameLogic
import com.example.taller2.model.ChatMessage
import com.example.taller2.model.GameRoom
import com.example.taller2.model.Player
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameViewModel : ViewModel() {
    // Si tienes la URL de tu base de datos, ponla aquí: FirebaseDatabase.getInstance("URL").reference
    private val database = FirebaseDatabase.getInstance().reference
    
    private val _gameRoom = MutableStateFlow<GameRoom?>(null)
    val gameRoom: StateFlow<GameRoom?> = _gameRoom

    private val _currentPlayerId = MutableStateFlow<String?>(null)
    val currentPlayerId: StateFlow<String?> = _currentPlayerId

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun joinRoom(roomId: String, playerName: String) {
        Log.d("FirebaseGame", "Intentando unirse a sala: $roomId con nombre: $playerName")
        val playerId = database.child("rooms").child(roomId).child("players").push().key ?: return
        _currentPlayerId.value = playerId
        
        val player = Player(id = playerId, name = playerName)
        database.child("rooms").child(roomId).child("players").child(playerId).setValue(player)
            .addOnSuccessListener {
                Log.d("FirebaseGame", "Jugador agregado exitosamente")
                observeRoom(roomId)
            }
            .addOnFailureListener {
                Log.e("FirebaseGame", "Error al agregar jugador", it)
            }
    }

    private fun observeRoom(roomId: String) {
        database.child("rooms").child(roomId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseGame", "Datos recibidos de la sala: ${snapshot.exists()}")
                val room = snapshot.getValue(GameRoom::class.java)
                _gameRoom.value = room
                
                room?.messages?.let {
                    _messages.value = it.values.sortedBy { m -> m.timestamp }
                }
                
                if (room != null && room.status == "PLAYING") {
                    checkTurnCompletion(room)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseGame", "Error en Firebase: ${error.message} - ${error.details}")
            }
        })
    }

    private fun checkTurnCompletion(room: GameRoom) {
        val activePlayers = room.players.values.filter { it.isAlive }
        val playersWhoPlayed = activePlayers.filter { it.turnPlayed == room.currentTurn }
        
        if (activePlayers.isNotEmpty() && playersWhoPlayed.size == activePlayers.size) {
            if (room.currentTurn < GameLogic.MAX_TURNS) {
                database.child("rooms").child(room.id).child("currentTurn").setValue(room.currentTurn + 1)
            } else {
                database.child("rooms").child(room.id).child("status").setValue("FINISHED")
            }
        }
    }

    fun startGame(roomId: String) {
        database.child("rooms").child(roomId).child("id").setValue(roomId)
        database.child("rooms").child(roomId).child("status").setValue("PLAYING")
    }

    fun performAction(roomId: String, playerId: String, action: String) {
        val room = _gameRoom.value ?: return
        val player = room.players[playerId] ?: return
        
        if (!player.isAlive || player.turnPlayed == room.currentTurn) return

        var newMoney = GameLogic.calculateNewMoney(player.money, action)
        newMoney = GameLogic.applyRandomEvent(newMoney)

        val stillAlive = GameLogic.isPlayerAlive(newMoney)
        
        val updates = hashMapOf<String, Any>(
            "players/$playerId/money" to if (stillAlive) newMoney else 0,
            "players/$playerId/isAlive" to stillAlive,
            "players/$playerId/turnPlayed" to room.currentTurn,
            "players/$playerId/lastAction" to action
        )
        
        database.child("rooms").child(roomId).updateChildren(updates)
    }

    fun sendMessage(roomId: String, text: String, senderName: String) {
        val messageId = database.child("rooms").child(roomId).child("messages").push().key ?: return
        val message = ChatMessage(senderId = _currentPlayerId.value ?: "", senderName = senderName, text = text)
        database.child("rooms").child(roomId).child("messages").child(messageId).setValue(message)
    }
    
    fun restartGame(roomId: String) {
        val room = _gameRoom.value ?: return
        val updatedPlayers = room.players.mapValues { (_, p) ->
            p.copy(money = GameLogic.INITIAL_MONEY, isAlive = true, turnPlayed = 0, lastAction = "")
        }
        database.child("rooms").child(roomId).updateChildren(mapOf(
            "status" to "WAITING",
            "currentTurn" to 1,
            "players" to updatedPlayers,
            "messages" to null
        ))
    }
}
