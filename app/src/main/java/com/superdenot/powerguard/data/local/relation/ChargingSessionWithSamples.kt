package com.superdenot.powerguard.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity

data class ChargingSessionWithSamples(
    @Embedded val session: ChargingSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val samples: List<BatterySampleEntity>
)
