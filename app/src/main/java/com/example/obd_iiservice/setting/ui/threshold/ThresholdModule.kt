package com.example.obd_iiservice.setting.ui.threshold

import com.example.obd_iiservice.helper.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class ThresholdModule {

    @Provides
    fun provideThreshold(
        preferenceManager: PreferenceManager
    ) : ThresholdRepository {
        return ThresholdRepositoryImpl(preferenceManager)
    }
}