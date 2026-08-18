package io.github.xep426.campaign.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyTaskEntity::class,
        CampaignEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class CampaignDatabase : RoomDatabase() {
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun campaignDao(): CampaignDao
}
