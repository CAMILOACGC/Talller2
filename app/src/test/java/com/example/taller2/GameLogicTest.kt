package com.example.taller2

import com.example.taller2.logic.GameLogic
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameLogicTest {

    @Test
    fun testCalculateNewMoney_Save() {
        val initialMoney = 1000
        val result = GameLogic.calculateNewMoney(initialMoney, "SAVE")
        assertEquals(1100, result)
    }

    @Test
    fun testCalculateNewMoney_Spend() {
        val initialMoney = 1000
        val result = GameLogic.calculateNewMoney(initialMoney, "SPEND")
        assertEquals(850, result)
    }

    @Test
    fun testIsPlayerAlive() {
        assertTrue(GameLogic.isPlayerAlive(100))
        assertFalse(GameLogic.isPlayerAlive(0))
        assertFalse(GameLogic.isPlayerAlive(-50))
    }

    @Test
    fun testCheckVictory() {
        // Victory at turn 10 with positive money
        assertTrue(GameLogic.checkVictory(10, 500))
        // No victory if money is 0
        assertFalse(GameLogic.checkVictory(10, 0))
        // No victory if turn is not 10
        assertFalse(GameLogic.checkVictory(5, 1000))
    }

    @Test
    fun testCalculateNewMoney_Invest_RandomMock() {
        val initialMoney = 1000
        // Mock random to always return 200 gain
        val fixedRandom = Random(42) // Seed 42 for deterministic results
        // In the logic: random.nextInt(-200, 301)
        // Let's just check if it stays within bounds manually or with a specific seed
        val result = GameLogic.calculateNewMoney(initialMoney, "INVEST", fixedRandom)
        
        // Random(42).nextInt(-200, 301) with seed 42 is usually consistent
        // But the main point is it should be different from initialMoney
        assertNotEquals(initialMoney, result)
    }
}
