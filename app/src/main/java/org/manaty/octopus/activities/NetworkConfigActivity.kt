package org.manaty.octopus.activities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import com.orhanobut.logger.Logger
import com.pixplicity.easyprefs.library.Prefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_network_config.*
import kotlinx.android.synthetic.main.activity_network_config.view.*
import org.m.BaseActivity
import org.manaty.octopus.PrefsKey
import org.manaty.octopus.R
import org.manaty.octopus.rxBus.RxBus
import org.manaty.octopus.rxBus.RxBusEvents
import org.manaty.octopus.viewModels.NetworkConfigViewModel

class NetworkConfigActivity : BaseActivity() {
    lateinit var viewmodel : NetworkConfigViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_config)

        viewmodel = ViewModelProviders.of(this as FragmentActivity).get(NetworkConfigViewModel::class.java)
        initView()
        viewmodel.isInternetAvailable = verifyAvailableNetwork()
        subscribeToObservables()
    }

    private fun initView(){
        if (Prefs.contains(PrefsKey.HOST_KEY)
            && Prefs.contains(PrefsKey.PORT_KEY)){
            edittext_host.setText(Prefs.getString(PrefsKey.HOST_KEY, "-1"))
            edittext_port.setText("${Prefs.getInt(PrefsKey.PORT_KEY, 0)}")
        }

        button_test_connection.setOnClickListener {
            it.isEnabled = false

            if (viewmodel.isInternetAvailable){
                if (validate()) {
                    testConnection(
                        edittext_host.text.toString(),
                        edittext_port.text.toString().toInt()
                    )
                }
                else{
                    toast("Invalid Host/Port")
                    it.isEnabled = true
                }
            }
            else{
                toast("Internet connection unavailable")
                it.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewmodel.compositeDisposable.clear()
        viewmodel.compositeDisposable.dispose()
    }

    fun subscribeToObservables(){
        viewmodel.compositeDisposable.add(
            viewmodel.isRequesting
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when(it){
                        true -> {
                            button_test_connection.isEnabled = true
                            progressbar.visibility = View.VISIBLE
                        }
                        false -> {
                            button_test_connection.isEnabled = false
                            progressbar.visibility = View.GONE
                        }
                    }
                }
        )

        viewmodel.compositeDisposable.add(RxBus.listen(RxBusEvents.EventInternetStatus::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                when(event.isInternetAvailable){
                    true -> viewmodel.isInternetAvailable = true
                    false -> viewmodel.isInternetAvailable = false
                }
            }
        )
    }

    private fun validate() : Boolean{
        //check if valid parameters
        if (edittext_host.text.toString().isEmpty()
            || edittext_port.toString().isEmpty()) {
            return false
        }

        return true
    }

    private fun testConnection(host : String, port : Int){
        viewmodel.compositeDisposable.add(viewmodel.testConnection(host, port)
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorComplete {e ->
                toast(e.cause.toString())
                Logger.e(e, "testConnection error")
                button_test_connection.isEnabled = true
                return@onErrorComplete true
            }
            .subscribe {
                if (it.headsetsCount > 0) {
                    toast("Connection successful!")
                    finish()
                    startActivity(Intent(this@NetworkConfigActivity,
                        SplashActivity::class.java))
                }

                button_test_connection.isEnabled = true
            }
        )
    }
}
