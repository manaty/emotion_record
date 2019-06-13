package org.m

import android.net.NetworkRequest
import com.orhanobut.logger.Logger

import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.manaty.octopus.R
import org.manaty.octopus.rxBus.RxBus
import org.manaty.octopus.rxBus.RxBusEvents
import java.util.logging.Handler

open class BaseActivity : AppCompatActivity(){
    var isAlertDialogShown = false
    private val networkRequest = NetworkRequest.Builder()
    private lateinit var connectivityManager : ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initConnectionObserver()
        registerConnectionObserver()
    }

    override fun onDestroy() {
        Logger.d("baseActivity onDestroy")
        unregisterConnectionObserver()
        super.onDestroy()
    }

    fun toast(message : String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun alertDialog(message : String){
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Ok"){
                dialog, which ->
                    dialog.dismiss()
                    isAlertDialogShown = false
            }
            .create()
            .show()
//            .setPositiveButton("Ok", { dialog, which ->
//                dialog.dismiss()
//            })
    }

    fun verifyAvailableNetwork():Boolean{
        val networkInfo=connectivityManager.activeNetworkInfo
        return  networkInfo!=null && networkInfo.isConnected
    }

    private fun initConnectionObserver(){
        connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback(){
            override fun onAvailable(network: Network?) {
                super.onAvailable(network)
//                Logger.d("onAvailable ${network.toString()}")
                RxBus.publish(RxBusEvents.EventInternetStatus(true))

            }

            override fun onUnavailable() {
                super.onUnavailable()
//                Logger.d("onUnAvailable")
                RxBus.publish(RxBusEvents.EventInternetStatus(false))
            }

            override fun onLost(network: Network?) {
                super.onLost(network)
//                Logger.d("onLost")
                RxBus.publish(RxBusEvents.EventInternetStatus(false))
            }
        }
    }

    private fun registerConnectionObserver(){
        connectivityManager.registerNetworkCallback(networkRequest.build(),
            networkCallback)
    }

    private fun unregisterConnectionObserver(){
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    fun showCloseAppDialog(){
        val alertDialog = AlertDialog.Builder(this)
            .setMessage(getString(R.string.dummy_text))
            .setNegativeButton("Fermer I'application",
                { dialog, which ->
                    dialog.dismiss()
                    finish()
                })
            .create()
            .show()
    }
}