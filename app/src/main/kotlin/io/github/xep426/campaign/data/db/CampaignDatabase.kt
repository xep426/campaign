package io.github.xep426.campaign.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        StepEntity::class,
        CampaignEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class CampaignDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao
    abstract fun campaignDao(): CampaignDao
}
