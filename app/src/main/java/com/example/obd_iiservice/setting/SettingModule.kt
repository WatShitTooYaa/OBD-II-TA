package com.example.obd_iiservice.setting

import com.example.obd_iiservice.app.ApplicationScope
import com.example.obd_iiservice.helper.PreferenceManager
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
class SettingModule {

    fun providesSettingRepository(
        preferenceManager: PreferenceManager,
        @ApplicationScope applicationScope: CoroutineScope
    ) : SettingRepository {
        return SettingRepositoryImpl(
            preferenceManager,
            applicationScope
        )
    }
}