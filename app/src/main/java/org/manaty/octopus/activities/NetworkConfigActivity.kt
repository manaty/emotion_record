package org.manaty.octopus.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import com.pixplicity.easyprefs.library.Prefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_network_config.*
import kotlinx.android.synthetic.main.activity_network_config.view.*
import org.manaty.octopus.PrefsKey
import org.manaty.octopus.R
import org.manaty.octopus.viewModels.NetworkConfigViewModel

class NetworkConfigActivity : BaseActivity() {
    lateinit var viewmodel : NetworkConfigViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_config)

        viewmodel = ViewModelProviders.of(this as FragmentActivity).get(NetworkConfigViewModel::class.java)
        initView()
        subscribeToObservables()
    }

    private fun initView(){
        if (Prefs.contains(PrefsKey.HOST_KEY)
            && Prefs.contains(PrefsKey.PORT_KEY)){
            edittext_host.setText(Prefs.getString(PrefsKey.HOST_KEY, "-1"))
            edittext_port.setText("${Prefs.getInt(PrefsKey.PORT_KEY, 0)}")
        }

        button_test_connection.setOnClickListener {

            if (validate())
                testConnection(edittext_host.text.toString(),
                    edittext_port.text.toString().toInt()
                    )
            else{
                toast("Invalid Host/Port")
            }
        }
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
    }

    private fun validate() : Boolean{
        if (edittext_host.text.toString().isEmpty()
            || edittext_port.toString().isEmpty()) {
            return false
        }

        return true
    }

    fun testConnection(host : String, port : Int){
        viewmodel.compositeDisposable.add(viewmodel.testConnection(host, port)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.headsetsCount > 0) {
                    toast("Connection successful!")
                    startActivity(Intent(this@NetworkConfigActivity,
                        SplashActivity::class.java))
                }
            }
        )
    }
}
