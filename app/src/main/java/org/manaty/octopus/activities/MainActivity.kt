package org.manaty.octopus.activities

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.orhanobut.logger.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import net.manaty.octopusync.api.State
import org.m.BaseActivity
import org.manaty.octopus.R
import org.manaty.octopus.rxBus.RxBus
import org.manaty.octopus.rxBus.RxBusEvents
import org.manaty.octopus.viewModels.MainViewModel
import org.manaty.octopus.views.ImpedanceDialog
import org.manaty.octopus.views.StatusButton

class MainActivity : BaseActivity(), View.OnTouchListener, View.OnClickListener {
    lateinit var viewModel: MainViewModel
    var state : State = State.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /** keep the screen on**/
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

        button_impedance_info.setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        /** remove screen on flag**/
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel.requestObserver.onCompleted()
        viewModel.channel.shutdownNow()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    fun subscribeObservables(){
        viewModel.compositeDisposable.add(viewModel.showErrorToast
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                toast(message = it)
            }
        )

        viewModel.compositeDisposable.add(
            viewModel.subjectShowDialog
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    alertDialog(it)
                }
        )

        viewModel.compositeDisposable.add(
            RxBus.listen(RxBusEvents.EventInternetStatus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    when(event.isInternetAvailable){
                        true -> {
                            viewModel.isInternetAvailable = true
                            viewModel.requestSync()
                        }
                        false -> {
                            viewModel.isInternetAvailable = false
                            viewModel.requestObserver.onCompleted()
                        }
                    }
                }
        )

        viewModel.compositeDisposable.add(viewModel.sessionStatus
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                when(it){
                    true -> {
                        textview_session_initial_text.visibility = View.GONE
                    }

                    false -> {
                        showCloseAppDialog()
                    }
                }
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
        viewModel.impedanceValue.observe(this, Observer {
            textview_impedance_value.setText("$it%")
        })
    }

    override fun onClick(v: View?) {
        when(v){
            button_impedance_info -> {
                ImpedanceDialog().show(supportFragmentManager, "impedanceDialog")
            }
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event!!.action == MotionEvent.ACTION_DOWN){
            setButtonsDefault()
            when(v){

                button_frisson -> {
                    if (isStateDuplicate(State.FRISSON_MUSICAL)){
                        state = State.NONE
                    }
                    else {
                        state = State.FRISSON_MUSICAL
                        button_frisson.setBackgroundResource(R.drawable.bg_main_button_selected)
                        button_frisson.setTextColor(ContextCompat.getColor(this, R.color.colorFrisson))
                    }
                }

                button_plaisir -> {
                    if (isStateDuplicate(State.PLAISIR_INTENSE)){
                        state = State.NONE
                    }
                    else {
                        state = State.PLAISIR_INTENSE
                        button_plaisir.setBackgroundResource(R.drawable.bg_main_button_selected)
                        button_plaisir.setTextColor(ContextCompat.getColor(this, R.color.colorFrisson))
                    }
                }

                button_faible -> {
                    if (isStateDuplicate(State.FAIBLE_PLAISIR)){
                        state = State.NONE
                    }
                    else {
                        state = State.FAIBLE_PLAISIR
                        button_faible.setBackgroundResource(R.drawable.bg_main_button_selected)
                        button_faible.setTextColor(ContextCompat.getColor(this, R.color.colorFrisson))
                    }
                }

                button_neutre -> {
                    if (isStateDuplicate(State.NEUTRE)){
                        state = State.NONE
                    }
                    else {
                        state = State.NEUTRE
                        button_neutre.setBackgroundResource(R.drawable.bg_main_button_selected)
                        button_neutre.setTextColor(ContextCompat.getColor(this, R.color.colorFrisson))
                    }
                }

            }

            updateState(state)
        }
        return false
    }

    /** check if the current state is the same with the supposedly next state **/
    private fun isStateDuplicate(state : State) : Boolean{
        if (this.state == state) {
            return true
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
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorComplete {
                return@onErrorComplete true
            }
            .subscribe {response ->
                Logger.d("updateState = $response")
            }
        )
    }



}
