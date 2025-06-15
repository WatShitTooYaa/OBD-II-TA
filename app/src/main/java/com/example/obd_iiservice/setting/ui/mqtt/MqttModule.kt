package com.example.obd_iiservice.setting.ui.mqtt

import com.example.obd_iiservice.app.ApplicationScope
import com.example.obd_iiservice.helper.PreferenceManager
import dagger.Module
import dagger.Provides
//import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MqttModule {

    @Provides
    @Singleton
    fun providesMqttRepository(
        preferenceManager: PreferenceManager,
        @ApplicationScope applicationScope: CoroutineScope
    ) : MqttRepository {
        return MqttRepositoryImpl(
            preferenceManager,
            applicationScope
        )
    }
}