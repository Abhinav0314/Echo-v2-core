package com.abhinav.kotlinytmusicscraper.models.body

import com.abhinav.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)