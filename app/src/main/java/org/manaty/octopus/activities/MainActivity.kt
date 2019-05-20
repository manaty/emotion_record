package org.manaty.octopus.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.orhanobut.logger.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import net.manaty.octopusync.api.State
import org.manaty.octopus.R
import org.manaty.octopus.viewModels.MainViewModel
import org.manaty.octopus.views.StatusButton

class MainActivity : BaseActivity(), View.OnTouchListener {
    lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this as FragmentActivity).get(MainViewModel::class.java)

        initView()
        subscribeObservables()

        viewModel.requestSync()
    }

    fun initView(){
        button_frisson.setOnTouchListener (this)
        button_plaisir.setOnTouchListener (this)
        button_faible.setOnTouchListener (this)
        button_neutre.setOnTouchListener (this)
    }

    fun subscribeObservables(){
        viewModel.compositeDisposable.add(viewModel.showErrorToast
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                toast(message = it)
            }
        )

        val serverObserver = Observer<Boolean> {
                server -> view_status_server.setState(
            if(server){
                view_status_server.setLogo(R.drawable.server_on)
                StatusButton.State.ENABLED
            }
            else{
                view_status_server.setLogo(R.drawable.server_off)
                StatusButton.State.DISABLED// automatically returned
            })
        }



        val headsetObserver = Observer<Boolean> {
                headset -> view_status_headset.setState(
            if(headset){
                view_status_headset.setLogo(R.drawable.headset_on)
                StatusButton.State.ENABLED
            }
            else{
                view_status_headset.setLogo(R.drawable.headset_off)
                StatusButton.State.DISABLED
            })
        }

        viewModel.serverStatus.observe(this, serverObserver)
        viewModel.headsetStatus.observe(this, headsetObserver)

        /**
         * setting of serverstatus
         * viewModel.serverStatus.postValue(true)
         */
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event!!.action == MotionEvent.ACTION_DOWN){
            var state = State.NEUTRE

            when(v){

                button_frisson -> {
                    state = State.FRISSON_MUSICAL
                    setButtonsDefault()
                    button_frisson.setBackgroundResource(R.drawable.bg_main_button_selected)
                    button_frisson.setTextColor(ContextCompat.getColor(this, R.color.colorFrisson))
                }

                button_plaisir -> {
                    state = State.PLAISIR_INTENSE
                    setButtonsDefault()
                    button_plaisir.setBackgroundResource(R.drawable.bg_main_button_selected)
                    button_plaisir.setTextColor(ContextCompat.getColor(this, R.color.colorFrisson))
                }

                button_faible -> {
                    state = State.FAIBLE_PLAISIR
                    setButtonsDefault()
                    button_faible.setBackgroundResource(R.drawable.bg_main_button_selected)
                    button_faible.setTextColor(ContextCompat.getColor(this, R.color.colorFrisson))
                }

                button_neutre -> {
                    state = State.NEUTRE
                    setButtonsDefault()
                    button_neutre.setBackgroundResource(R.drawable.bg_main_button_selected)
                    button_neutre.setTextColor(ContextCompat.getColor(this, R.color.colorFrisson))
                }

            }

            updateState(state)
        }
        return false
    }


    private fun setButtonsDefault(){
        button_frisson.setBackgroundColor(ContextCompat.getColor(this, R.color.colorFrisson))
        button_frisson.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))

        button_plaisir.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPlaisir))
        button_plaisir.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))

        button_faible.setBackgroundColor(ContextCompat.getColor(this, R.color.colorFaible))
        button_faible.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))

        button_neutre.setBackgroundColor(ContextCompat.getColor(this, R.color.colorNeutre))
        button_neutre.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
    }

    fun updateState(state : State){
        viewModel.compositeDisposable.add(viewModel.requestUpdateState(state)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {response ->
                Logger.d("updateState = $response")
            }
        )
    }



}
