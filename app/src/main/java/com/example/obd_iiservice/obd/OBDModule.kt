package com.example.obd_iiservice.obd

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class OBDModule {

    @Provides
    @Singleton
    fun providesOBDRepository() : OBDRepository {
        return OBDRepositoryImpl()
    }
}