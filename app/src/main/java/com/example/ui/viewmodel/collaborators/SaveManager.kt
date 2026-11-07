package com.example.ui.viewmodel.collaborators

import java.util.Stack

object SaveManager {
    fun serializeBoard(board: Array<IntArray>): String =
        board.joinToString(";") { it.joinToString(",") }

    fun deserializeBoard(str: String): Array<IntArray>? {
        if (str.length > 2048) return null
        val rows = str.split(";")
        if (rows.size !in 3..6) return null
        val board = Array(rows.size) { IntArray(rows.size) }
        for ((r, row) in rows.withIndex()) {
            val cells = row.split(",")
            if (cells.size != rows.size) return null
            for ((c, cell) in cells.withIndex()) {
                val value = cell.toIntOrNull() ?: return null
                if (value !in -10000..10000) return null
                board[r][c] = value
            }
        }
        return board
    }

    fun serializeHistory(history: Stack<Array<IntArray>>): String =
        history.joinToString("|") { serializeBoard(it) }

    fun deserializeHistory(str: String): Stack<Array<IntArray>>? {
        if (str.length > 2_048_000) return null
        val stack = Stack<Array<IntArray>>()
        if (str.isEmpty()) return stack
        for (encoded in str.split("|")) stack.push(deserializeBoard(encoded) ?: return null)
        return stack
    }
}
