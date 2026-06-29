package com.example.ui.viewmodel.collaborators

import java.util.Stack

object SaveManager {
    fun serializeBoard(board: Array<IntArray>): String {
        return board.joinToString(";") { row -> row.joinToString(",") }
    }

    fun deserializeBoard(str: String): Array<IntArray>? = try {
        val rows = str.split(";")
        Array(rows.size) { r ->
            val cols = rows[r].split(",")
            IntArray(cols.size) { c -> cols[c].toInt() }
        }
    } catch (e: Exception) {
        null
    }

    fun serializeHistory(history: Stack<Array<IntArray>>): String {
        return history.joinToString("|") { serializeBoard(it) }
    }

    fun deserializeHistory(str: String): Stack<Array<IntArray>>? = try {
        val stack = Stack<Array<IntArray>>()
        if (str.isNotEmpty()) {
            str.split("|").forEach { boardStr ->
                if (boardStr.isNotEmpty()) {
                    val board = deserializeBoard(boardStr) ?: throw IllegalArgumentException("Malformed board in history")
                    stack.push(board)
                }
            }
        }
        stack
    } catch (e: Exception) {
        null
    }
}
