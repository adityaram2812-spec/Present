package com.adityaram.present.domain.repository

import com.adityaram.present.data.dao.LeavePlanDao
import com.adityaram.present.data.model.LeavePlan
import kotlinx.coroutines.flow.Flow

class LeavePlanRepository(
    private val leavePlanDao: LeavePlanDao
) {
    fun getAllLeavePlans(): Flow<List<LeavePlan>> = leavePlanDao.getAllLeavePlans()

    suspend fun saveLeavePlan(leavePlan: LeavePlan) {
        if (leavePlan.id == 0L) {
            leavePlanDao.insert(leavePlan)
        } else {
            leavePlanDao.update(leavePlan)
        }
    }

    suspend fun getLeavePlan(id: Long): LeavePlan? {
        return leavePlanDao.getLeavePlan(id)
    }

    suspend fun deleteLeavePlan(leavePlan: LeavePlan) {
        leavePlanDao.delete(leavePlan)
    }
}
