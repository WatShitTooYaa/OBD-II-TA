package com.example.obd_iiservice.helper

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

object SSLSocketFactoryGenerator {
    fun createSocketFactory(): SSLSocketFactory {
        val context = SSLContext.getInstance("TLS")
        context.init(null, null, null)
        return context.socketFactory
    }
}