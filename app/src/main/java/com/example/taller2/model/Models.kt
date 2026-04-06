package com.example.taller2.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Player(
    val id: String = "",
    val name: String = "",
    val money: Int = 1000,
    val isAlive: Boolean = true,
    val lastAction: String = "",
    val turnPlayed: Int = 0
)

@IgnoreExtraProperties
data class ChatMessage(
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class GameRoom(
    val id: String = "",
    val status: String = "WAITING", // WAITING, PLAYING, FINISHED
    val currentTurn: Int = 1,
    val players: Map<String, Player> = emptyMap(),
    val messages: Map<String, ChatMessage> = emptyMap()
)
