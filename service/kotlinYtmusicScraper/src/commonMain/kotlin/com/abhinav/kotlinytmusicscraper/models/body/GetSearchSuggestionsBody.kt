package com.abhinav.kotlinytmusicscraper.models.body

import com.abhinav.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)