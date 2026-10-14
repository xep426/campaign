package io.github.xep426.campaign.di

import android.content.Context
import androidx.room.Room
import io.github.xep426.campaign.data.CampaignRepositoryImpl
import io.github.xep426.campaign.data.StepRepositoryImpl
import io.github.xep426.campaign.data.db.CampaignDao
import io.github.xep426.campaign.data.db.CampaignDatabase
import io.github.xep426.campaign.data.db.StepDao
import io.github.xep426.campaign.data.db.MIGRATION_1_2
import io.github.xep426.campaign.data.db.MIGRATION_2_3
import io.github.xep426.campaign.data.db.MIGRATION_3_4
import io.github.xep426.campaign.domain.repository.CampaignRepository
import io.github.xep426.campaign.domain.repository.StepRepository
import io.github.xep426.campaign.domain.repository.WidgetRefresher
import io.github.xep426.campaign.widget.GlanceWidgetRefresher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CampaignDatabase =
        Room.databaseBuilder(
            context,
            CampaignDatabase::class.java,
            "campaign.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

    @Provides
    fun provideStepDao(db: CampaignDatabase): StepDao = db.stepDao()

    @Provides
    fun provideCampaignDao(db: CampaignDatabase): CampaignDao = db.campaignDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStepRepository(impl: StepRepositoryImpl): StepRepository

    @Binds
    @Singleton
    abstract fun bindCampaignRepository(impl: CampaignRepositoryImpl): CampaignRepository

    @Binds
    @Singleton
    abstract fun bindWidgetRefresher(impl: GlanceWidgetRefresher): WidgetRefresher
}
