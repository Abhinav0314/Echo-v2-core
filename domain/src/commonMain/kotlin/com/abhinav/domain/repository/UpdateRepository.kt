package com.abhinav.domain.repository

import com.abhinav.domain.data.model.update.UpdateData
import com.abhinav.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    fun checkForEchoUpdate(currentVersionCode: Int): Flow<Resource<UpdateData>>

    fun checkForGithubReleaseUpdate(): Flow<Resource<UpdateData>>
    fun checkForFdroidUpdate(): Flow<Resource<UpdateData>>

    fun getFdroidSigningKeys(): Flow<Resource<List<String>>>
}