package com.example.obd_iiservice.main

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MainModule {

    @Provides
    @Singleton
    fun providesMainRepository(
        application: Application
    ) : MainRepository {
        return MainRepositoryImpl(application)
    }
}