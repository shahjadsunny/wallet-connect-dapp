package com.shahjad.walletconnectlibadd
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import com.shahjad.walletconnectlibadd.databinding.ScreenMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.walletconnect.Session
import org.walletconnect.nullOnThrow


class MainActivity : Activity(), Session.Callback {

    private lateinit var binding: ScreenMainBinding
    private var txRequest: Long? = null
    private val uiScope = CoroutineScope(Dispatchers.Main)

    companion object{
        private const val TAG = "MainActivity"
    }
    override fun onStatus(status: Session.Status) {
        when(status) {
            Session.Status.Approved -> sessionApproved()
            Session.Status.Closed -> sessionClosed()
            Session.Status.Connected,
            Session.Status.Disconnected ,
            is Session.Status.Error -> {
                // Do Stuff
                Log.i(TAG, "onStatus: Session.Status.Error${status}")
                Log.i(TAG, "onStatus: ExampleApplication.session:${ExampleApplication.session}")
            }
        }
    }

    override fun onMethodCall(call: Session.MethodCall) {
        Log.i(TAG, "onMethodCall: call:${call}")
    }
    private fun sessionApproved() = with(binding) {
        uiScope.launch {
            screenMainStatus.text = "Connected: ${ExampleApplication.session.approvedAccounts()}"
            screenMainConnectButton.visibility = View.GONE
            screenMainDisconnectButton.visibility = View.VISIBLE
            screenMainTxButton.visibility = View.VISIBLE
        }
    }

    private fun sessionClosed()  = with(binding){
        uiScope.launch {
            screenMainStatus.text = "Disconnected"
            screenMainConnectButton.visibility = View.VISIBLE
            screenMainDisconnectButton.visibility = View.GONE
            screenMainTxButton.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() = with(binding) {
        super.onStart()
        initialSetup()
        screenMainConnectButton.setOnClickListener {
            ExampleApplication.resetSession()
            ExampleApplication.session.addCallback(this@MainActivity)
            val i = Intent(Intent.ACTION_VIEW)
            Log.i(
                TAG,
                "onStart: ExampleApplication.config.toWCUri():${ExampleApplication.config.toWCUri()}"
            )
            i.data = Uri.parse(ExampleApplication.config.toWCUri())
            startActivity(i)
        }
        screenMainDisconnectButton.setOnClickListener {
            ExampleApplication.session.kill()
        }
        screenMainTxButton.setOnClickListener {

            try{
                val from = ExampleApplication.session.approvedAccounts()?.first()
                    ?: return@setOnClickListener
                val txRequest = System.currentTimeMillis()
                Log.i(TAG, "onStart: from:${from}")
                val nonce = "0x0114"
                val gasPrice = "0x02540be400"
                val gasLimit = "0x9c40"
//                Log.i(TAG, "onStart: nonce:${nonce}")
//                Log.i(TAG, "onStart: gasPrice:${gasPrice}")
//                Log.i(TAG, "onStart: txRequest:${txRequest}")
//                Log.i(TAG, "onStart: from:${from}")
                ExampleApplication.session.performMethodCall(
                    Session.MethodCall.SendTransaction(
                        id = txRequest,
                        from =from,
                        to = from,
                        nonce = "0x0114",
                        gasPrice = gasPrice,
                        gasLimit = gasLimit,
                        value = "0x00",
                        data = "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675"
                    ),
                    ::handleResponse
                )
                this@MainActivity.txRequest = txRequest
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("wc:")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }catch (e:Exception){
                Log.i(TAG, "onStart: screenMainTxButton:${e.message}")
            }
        }
    }

    private fun initialSetup() {
        val session = nullOnThrow { ExampleApplication.session } ?: return
        session.addCallback(this)
        sessionApproved()
    }

    private fun handleResponse(resp: Session.MethodCall.Response)  = with(binding){
        Log.i(TAG, "handleResponse: response:${resp}")
        if (resp.id == txRequest) {
            txRequest = null
            uiScope.launch {
                screenMainResponse.visibility = View.VISIBLE
                screenMainResponse.text = "Last response: " + ((resp.result as? String) ?: "Unknown response")
            }
        }
    }

    override fun onDestroy() {
        ExampleApplication.session.removeCallback(this)
        super.onDestroy()
    }

//
//    fun test(){
//
//        MainApplication.session?.performMethodCall(
//            Session.MethodCall.SendTransaction(
//                txRequest,
//                from = viewModel.address.value,
//                to = contractAddress,
//                nonce = nonce.transactionCount.toString(16),
//                gasPrice = DefaultGasProvider.GAS_PRICE.toString(16),
//                gasLimit = DefaultGasProvider.GAS_LIMIT.toString(16),
//                value = BigInteger.ZERO.toString(16),
//                data = endcodedFunction
//            )
//        ) { resp ->
//            // do something with callback
//        }
//        val i = Intent(Intent.ACTION_VIEW)
//        i.data = Uri.parse("wc:")
//        context.startActivity(i)
//    }
}
