package com.abhinav.domain.data.model.home

import com.abhinav.domain.data.model.home.chart.Chart
import com.abhinav.domain.data.model.mood.Mood
import com.abhinav.domain.utils.Resource

data class HomeResponse(
    val homeItem: Resource<ArrayList<HomeItem>>,
    val exploreMood: Resource<Mood>,
    val exploreChart: Resource<Chart>,
)