package org.manaty.octopus.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import com.orhanobut.logger.Logger
import com.pixplicity.easyprefs.library.Prefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_splash.*
import net.manaty.octopusync.api.Headset
import org.manaty.octopus.BuildConfig
import org.manaty.octopus.PrefsKey
import org.manaty.octopus.R
import org.manaty.octopus.adapters.HeadsetListAdapter
import org.manaty.octopus.viewModels.SplashViewModel

class SplashActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    lateinit var viewModel : SplashViewModel
    lateinit var headsetListAdapter : HeadsetListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        viewModel = ViewModelProviders.of(this as FragmentActivity).get(SplashViewModel::class.java)
        initView()
        subscribeToObservables()

    }

    fun initView(){
        textview_app_version.text = "v" + BuildConfig.VERSION_NAME
        headsetListAdapter = HeadsetListAdapter(this, viewModel.listHeadsets)
        spinner_headsets.adapter = headsetListAdapter
        spinner_headsets.onItemSelectedListener = this
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.channel.shutdownNow()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Logger.d("selected ${viewModel.listHeadsets[position].id}")
        if (position != 0){
            createSession(viewModel.listHeadsets[position].code)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun subscribeToObservables(){
        viewModel.compositeDisposable.add(viewModel.isShowLoading
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {isShowLoading ->
                when(isShowLoading){
                    true -> viewModel.requestCount++
                    false -> viewModel.requestCount--
                }

                Logger.d("request = ${viewModel.requestCount}")
                if (viewModel.requestCount > 0){
                    progressbar.visibility = View.VISIBLE
                }
                else{
                    progressbar.visibility = View.GONE
                }
            }
        )

        viewModel.compositeDisposable.add(viewModel.showRequestError
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {errorString ->
                Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show()
            }
        )

        viewModel.compositeDisposable.addAll(viewModel.requestHeadsetList()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { getHeadsetsResponse ->

                val defaultItem = Headset.newBuilder()
                defaultItem.id = "0"
                defaultItem.code = "Select headset"

                viewModel.listHeadsets.add(defaultItem.build())
                viewModel.listHeadsets.addAll(getHeadsetsResponse.headsetsList)
                headsetListAdapter.notifyDataSetChanged()
                viewModel.isShowLoading.onNext(false)
            }
        )
    }

    private fun createSession(headsetCode : String){
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        viewModel.compositeDisposable.add(viewModel.requestCreateSession(headsetCode, deviceId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Logger.d(it)
                if (it.session.id != "-1"){
                    Prefs.putString(PrefsKey.SESSION_KEY, it.session.id)
                    moveToMain()
                }
            }
        )
    }

    private fun moveToMain(){
        startActivity(Intent(this, MainActivity::class.java))
    }
}
