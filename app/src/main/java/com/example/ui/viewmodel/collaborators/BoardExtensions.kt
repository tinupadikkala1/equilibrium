package com.example.ui.viewmodel.collaborators

fun Array<IntArray>.deepCopy(): Array<IntArray> = Array(size) { row -> this[row].clone() }
