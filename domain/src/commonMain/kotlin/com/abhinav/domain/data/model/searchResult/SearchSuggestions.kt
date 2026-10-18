package com.abhinav.domain.data.model.searchResult

import com.abhinav.domain.data.type.SearchResultType

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<SearchResultType>,
)