package com.abhinav.kotlinytmusicscraper.models

import kotlinx.serialization.Serializable

@Serializable
data class Runs(
    val runs: List<Run>?,
)

@Serializable
data class Run(
    val text: String,
    val navigationEndpoint: NavigationEndpoint?,
)

fun List<Run>.splitBySeparator(): List<List<Run>> {
    val res = mutableListOf<List<Run>>()
    var tmp = mutableListOf<Run>()
    forEach { run ->
        if (run.navigationEndpoint == null && run.text.trim() in bylineSeparators) {
            res.add(tmp)
            tmp = mutableListOf()
        } else {
            tmp.add(run)
        }
    }
    res.add(tmp)
    return res
}

fun List<Run>.oddElements() =
    filterIndexed { index, _ ->
        index % 2 == 0
    }
// Google emits standalone separators with ordinary, non-breaking and narrow spaces.
private val bylineSeparators = setOf("\u2022", "\u00B7", "\u2219", "\u2027")
