package com.example.taller2.logic
import kotlin.random.Random

/**
 * Lógica pura del juego Tío Rico Simplificado.
 * Separada para facilitar las pruebas unitarias.
 */
object GameLogic {

    const val INITIAL_MONEY = 1000
    const val MAX_TURNS = 10
    const val SAVE_GAIN = 100
    const val SPEND_LOSS = 150

    // Constantes para las acciones para evitar strings hardcoded en la lógica
    const val ACTION_SAVE = "SAVE"
    const val ACTION_INVEST = "INVEST"
    const val ACTION_SPEND = "SPEND"

    /**
     * Calcula el nuevo saldo basado en la acción elegida.
     */
    fun calculateNewMoney(currentMoney: Int, action: String, random: Random = Random.Default): Int {
        var newMoney = currentMoney
        when (action) {
            ACTION_SAVE -> newMoney += GameLogic.SAVE_GAIN
            ACTION_INVEST -> {
                // Invertir puede generar ganancias (-200 a 300)
                val result = random.nextInt(-200, 301)
                newMoney += result
            }
            ACTION_SPEND -> newMoney -= GameLogic.SPEND_LOSS
        }
        return newMoney
    }

    /**
     * Aplica un evento aleatorio al dinero del jugador.
     * Retorna el nuevo monto y una descripción del evento (opcional).
     */
    fun applyRandomEvent(money: Int, random: Random = Random.Default): Int {
        var updatedMoney = money
        if (random.nextInt(100) < 20) { // 20% de probabilidad
            val eventAmount = random.nextInt(-100, 101)
            updatedMoney += eventAmount
        }
        return updatedMoney
    }

    /**
     * Verifica si el jugador sigue en juego.
     */
    fun isPlayerAlive(money: Int): Boolean {
        return money > 0
    }

    /**
     * Verifica si el jugador ha ganado al llegar al turno final.
     */
    fun checkVictory(currentTurn: Int, money: Int): Boolean {
        return currentTurn >= GameLogic.MAX_TURNS && money > 0
    }
}
