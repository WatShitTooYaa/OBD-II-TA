package com.example.obd_iiservice.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class BluetoothModule {

    @Provides
    fun providesBluetoothAdapter(
        @ApplicationContext context: Context
    ) : BluetoothAdapter {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    @Provides
    @Singleton
    fun provideBluetoothRepository(
        bluetoothAdapter: BluetoothAdapter
    ) : BluetoothRepository {
        return BluetoothRepositoryImpl(bluetoothAdapter)
    }

}