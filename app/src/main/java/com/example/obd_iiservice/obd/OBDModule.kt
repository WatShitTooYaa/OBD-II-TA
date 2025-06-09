package com.example.obd_iiservice.obd

import android.content.Context
import com.example.obd_iiservice.app.ApplicationScope
import com.example.obd_iiservice.helper.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class OBDModule {

    @Provides
    @Singleton
    fun providesOBDRepository(
        preferenceManager: PreferenceManager,
        @ApplicationScope applicationScope: CoroutineScope,
        @ApplicationContext context: Context
    ) : OBDRepository {
        return OBDRepositoryImpl(preferenceManager, applicationScope, context)
    }
}