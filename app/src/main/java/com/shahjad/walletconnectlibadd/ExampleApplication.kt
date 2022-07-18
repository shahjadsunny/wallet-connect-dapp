package com.shahjad.walletconnectlibadd

import androidx.multidex.MultiDexApplication
import com.squareup.moshi.Moshi
import com.shahjad.walletconnectlibadd.server.BridgeServer
import okhttp3.OkHttpClient
import org.komputing.khex.extensions.toNoPrefixHexString
import org.walletconnect.Session
import org.walletconnect.impls.*
import org.walletconnect.nullOnThrow
import java.io.File
import java.util.*

class ExampleApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        initMoshi()
        initClient()
        initBridge()
        initSessionStorage()
    }

    private fun initClient() {
        client = OkHttpClient.Builder().build()
    }

    private fun initMoshi() {
        moshi = Moshi.Builder().build()
    }


    private fun initBridge() {
        bridge = BridgeServer(moshi)
        bridge.start()
    }

    private fun initSessionStorage() {
        storage = FileWCSessionStore(File(cacheDir, "session_store.json").apply { createNewFile() }, moshi)
    }

    companion object {
        lateinit var config: Session.Config
        private lateinit var client: OkHttpClient
        private lateinit var moshi: Moshi
        private lateinit var bridge: BridgeServer
        private lateinit var storage: WCSessionStore
        lateinit var session: Session

        fun resetSession() {
            nullOnThrow { session }?.clearCallbacks()
            val key = ByteArray(32).also { Random().nextBytes(it) }.toNoPrefixHexString()
            config = Session.Config(
                UUID.randomUUID().toString(),
                "wss://bridge.walletconnect.org",
//                "https://bridge.walletconnect.org",
                key)
            session = WCSession(
                config,
                    MoshiPayloadAdapter(moshi),
                    storage,
                    OkHttpTransport.Builder(client, moshi),
                    Session.PeerMeta(url="https://stage.vixo.tv",name = "Example App",description = "demp app",icons = arrayListOf())
            )
            session.offer()
        }
    }
}
