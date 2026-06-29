package com.example.ui.viewmodel.collaborators

import com.example.ui.viewmodel.GameViewModel.Difficulty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TimerController(private val scope: CoroutineScope) {
    val secondsElapsed = MutableStateFlow(0)
    val countdownSecondsLeft = MutableStateFlow<Int?>(null)
    val isTimeUp = MutableStateFlow(false)

    private var timerJob: Job? = null

    fun startTimer(difficulty: Difficulty, par: Int, winStateFlow: MutableStateFlow<Boolean>, onTimeUp: () -> Unit) {
        timerJob?.cancel()
        secondsElapsed.value = 0
        isTimeUp.value = false

        if (difficulty == Difficulty.MASTER) {
            val baseTime = maxOf(45, par * 20)
            countdownSecondsLeft.value = baseTime
        } else {
            countdownSecondsLeft.value = null
        }

        timerJob = scope.launch {
            while (true) {
                delay(1000)
                if (!winStateFlow.value && !isTimeUp.value) {
                    secondsElapsed.value += 1
                    if (difficulty == Difficulty.MASTER) {
                        val left = countdownSecondsLeft.value ?: 0
                        if (left > 1) {
                            countdownSecondsLeft.value = left - 1
                        } else {
                            countdownSecondsLeft.value = 0
                            isTimeUp.value = true
                            onTimeUp()
                            break
                        }
                    }
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
    }

    fun addSeconds(seconds: Int) {
        val current = countdownSecondsLeft.value ?: return
        countdownSecondsLeft.value = current + seconds
        if (isTimeUp.value) {
            isTimeUp.value = false
            // Restart timer loop
            timerJob = scope.launch {
                while (true) {
                    delay(1000)
                    secondsElapsed.value += 1
                    val left = countdownSecondsLeft.value ?: 0
                    if (left > 1) {
                        countdownSecondsLeft.value = left - 1
                    } else {
                        countdownSecondsLeft.value = 0
                        isTimeUp.value = true
                        break
                    }
                }
            }
        }
    }

    fun resumeTimer(difficulty: Difficulty, par: Int, secondsElapsedValue: Int, winStateFlow: MutableStateFlow<Boolean>, onTimeUp: () -> Unit) {
        timerJob?.cancel()
        secondsElapsed.value = secondsElapsedValue
        isTimeUp.value = false

        if (difficulty == Difficulty.MASTER) {
            val baseTime = maxOf(45, par * 20)
            countdownSecondsLeft.value = maxOf(0, baseTime - secondsElapsedValue)
        } else {
            countdownSecondsLeft.value = null
        }

        timerJob = scope.launch {
            while (true) {
                delay(1000)
                if (!winStateFlow.value && !isTimeUp.value) {
                    secondsElapsed.value += 1
                    if (difficulty == Difficulty.MASTER) {
                        val left = countdownSecondsLeft.value ?: 0
                        if (left > 1) {
                            countdownSecondsLeft.value = left - 1
                        } else {
                            countdownSecondsLeft.value = 0
                            isTimeUp.value = true
                            onTimeUp()
                            break
                        }
                    }
                }
            }
        }
    }
}
