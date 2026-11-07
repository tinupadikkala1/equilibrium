package com.example.ui.viewmodel.collaborators

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** A single countdown. Pausing, ads and backgrounding all suspend the same clock. */
class TimerController(private val scope: CoroutineScope) {
    val secondsElapsed = MutableStateFlow(0)
    val countdownSecondsLeft = MutableStateFlow(0)
    val isTimeUp = MutableStateFlow(false)
    private var job: Job? = null

    fun reset(par: Int) = restore(0, maxOf(45, par * 20))

    fun restore(elapsed: Int, remaining: Int) {
        pause()
        secondsElapsed.value = elapsed.coerceAtLeast(0)
        countdownSecondsLeft.value = remaining.coerceAtLeast(0)
        isTimeUp.value = countdownSecondsLeft.value == 0
    }

    fun resume() {
        if (job?.isActive == true || isTimeUp.value) return
        job = scope.launch {
            while (countdownSecondsLeft.value > 0) {
                delay(1000)
                secondsElapsed.value += 1
                countdownSecondsLeft.value -= 1
                if (countdownSecondsLeft.value == 0) isTimeUp.value = true
            }
        }
    }

    fun pause() { job?.cancel(); job = null }

    fun addSeconds(seconds: Int) {
        countdownSecondsLeft.value += seconds.coerceIn(0, 300)
        isTimeUp.value = countdownSecondsLeft.value == 0
    }
}
