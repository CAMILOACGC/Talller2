package com.example.taller2.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.taller2.logic.GameLogic
import com.example.taller2.model.ChatMessage
import com.example.taller2.model.GameRoom
import com.example.taller2.model.Player
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _gameRoom = MutableStateFlow<GameRoom?>(null)
    val gameRoom: StateFlow<GameRoom?> = _gameRoom

    private val _currentPlayerId = MutableStateFlow<String?>(null)
    val currentPlayerId: StateFlow<String?> = _currentPlayerId

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    fun signUp(email: String, pass: String, onSuccess: () -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener { _currentUser.value = auth.currentUser; onSuccess() }.addOnFailureListener { _authError.value = it.message }
    }

    fun signIn(email: String, pass: String, onSuccess: () -> Unit) {
        auth.signInWithEmailAndPassword(email, pass).addOnSuccessListener { _currentUser.value = auth.currentUser; onSuccess() }.addOnFailureListener { _authError.value = it.message }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnSuccessListener { _currentUser.value = auth.currentUser; onSuccess() }.addOnFailureListener { _authError.value = it.message }
    }

    fun signOut() {
        leaveRoom()
        auth.signOut()
        _currentUser.value = null
    }

    fun leaveRoom() {
        val pid = _currentPlayerId.value
        val rid = _gameRoom.value?.id
        if (pid != null && rid != null) {
            database.child("rooms").child(rid).child("players").child(pid).removeValue()
        }
        _currentPlayerId.value = null
        _gameRoom.value = null
        _messages.value = emptyList()
    }

    fun joinRoom(roomId: String, playerName: String) {
        val user = auth.currentUser ?: return
        val playerId = user.uid
        _currentPlayerId.value = playerId
        
        val player = Player(id = playerId, name = playerName)
        
        _gameRoom.value = null
        _messages.value = emptyList()

        database.child("rooms").child(roomId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingRoom = snapshot.getValue(GameRoom::class.java)
                
                if (existingRoom == null || existingRoom.hostId.isEmpty() || existingRoom.status == "FINISHED" || existingRoom.players.isEmpty()) {
                    database.child("rooms").child(roomId).removeValue().addOnSuccessListener {
                        val initialRoom = GameRoom(
                            id = roomId,
                            hostId = playerId,
                            status = "WAITING",
                            currentTurn = 1,
                            players = mapOf(playerId to player)
                        )
                        database.child("rooms").child(roomId).setValue(initialRoom).addOnSuccessListener { 
                            setupPresence(roomId, playerId)
                            observeRoom(roomId) 
                        }
                    }
                } else {
                    database.child("rooms").child(roomId).child("players").child(playerId).setValue(player)
                        .addOnSuccessListener { 
                            setupPresence(roomId, playerId)
                            observeRoom(roomId) 
                        }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupPresence(roomId: String, playerId: String) {
        database.child("rooms").child(roomId).child("players").child(playerId).onDisconnect().removeValue()
    }

    private fun observeRoom(roomId: String) {
        database.child("rooms").child(roomId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(GameRoom::class.java)
                if (room != null) {
                    _gameRoom.value = room
                    _messages.value = room.messages.values.sortedBy { m -> m.timestamp }
                    if (room.status == "PLAYING") checkTurnCompletion(room)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkTurnCompletion(room: GameRoom) {
        val allPlayers = room.players.values
        // REGLA: Solo esperamos turnos de jugadores VIVOS con dinero
        val activePlayers = allPlayers.filter { it.isAlive && it.money > 0 }
        
        // Si no quedan jugadores activos, la partida termina
        if (activePlayers.isEmpty() && allPlayers.isNotEmpty()) {
            database.child("rooms").child(room.id).child("status").setValue("FINISHED")
            return
        }

        val playersWhoPlayed = activePlayers.filter { it.turnPlayed == room.currentTurn }
        
        // Si todos los jugadores ACTIVOS ya jugaron, avanzamos turno
        if (activePlayers.isNotEmpty() && playersWhoPlayed.size == activePlayers.size) {
            if (room.currentTurn < GameLogic.MAX_TURNS) {
                database.child("rooms").child(room.id).child("currentTurn").setValue(room.currentTurn + 1)
            } else {
                database.child("rooms").child(room.id).child("status").setValue("FINISHED")
            }
        }
    }

    fun startGame(roomId: String) {
        val room = _gameRoom.value ?: return
        if (room.hostId != _currentPlayerId.value) return 
        database.child("rooms").child(roomId).child("status").setValue("PLAYING")
    }

    fun performAction(roomId: String, playerId: String, action: String) {
        val room = _gameRoom.value ?: return
        val player = room.players[playerId] ?: return
        
        if (player.turnPlayed == room.currentTurn) return

        var newMoney = player.money
        var stillAlive = player.isAlive

        if (action == "SKIP") {
            newMoney = 0
            stillAlive = false
        } else if (player.isAlive && player.money > 0) {
            newMoney = GameLogic.calculateNewMoney(player.money, action)
            newMoney = GameLogic.applyRandomEvent(newMoney)
            stillAlive = newMoney > 0
        } else {
            // Si el jugador intenta jugar estando muerto o con 0
            newMoney = 0
            stillAlive = false
        }

        val finalMoney = if (stillAlive) newMoney else 0

        val updates = hashMapOf<String, Any>(
            "players/$playerId/money" to finalMoney,
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
        if (room.hostId != _currentPlayerId.value) return 
        val resetData = hashMapOf<String, Any?>(
            "status" to "WAITING",
            "currentTurn" to 1,
            "messages" to null
        )
        room.players.forEach { (id, _) ->
            resetData["players/$id/money"] = GameLogic.INITIAL_MONEY
            resetData["players/$id/isAlive"] = true
            resetData["players/$id/turnPlayed"] = 0
            resetData["players/$id/lastAction"] = ""
        }
        database.child("rooms").child(roomId).updateChildren(resetData)
    }
}
