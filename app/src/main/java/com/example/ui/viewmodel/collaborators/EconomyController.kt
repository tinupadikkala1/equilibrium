package com.example.ui.viewmodel.collaborators

import com.example.data.repository.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class EconomyController(
    private val repository: GameRepository,
    private val scope: CoroutineScope
) {
    fun consumeUndo(onSuccess: () -> Unit, onFailure: () -> Unit) {
        scope.launch {
            val success = repository.useUndo()
            if (success) {
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    fun consumeHint(onSuccess: () -> Unit, onFailure: () -> Unit) {
        scope.launch {
            val success = repository.useHint()
            if (success) {
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    fun consumeSkip(onSuccess: () -> Unit, onFailure: () -> Unit) {
        scope.launch {
            val success = repository.useSkip()
            if (success) {
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    fun grantReward(type: String, amount: Int) {
        scope.launch {
            when (type.lowercase()) {
                "undo", "undos" -> repository.addUndos(amount)
                "hint", "hints" -> repository.addHints(amount)
                "skip", "skips" -> repository.addSkips(amount)
            }
        }
    }
}
