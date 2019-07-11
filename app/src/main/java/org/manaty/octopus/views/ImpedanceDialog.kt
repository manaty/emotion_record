package org.manaty.octopus.views

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_impedance.*
import net.manaty.octopusync.api.Notification
import org.manaty.octopus.R
import org.manaty.octopus.rxBus.RxBus
import org.manaty.octopus.rxBus.RxBusEvents
import java.text.DecimalFormat

class ImpedanceDialog : DialogFragment(){
    private var compositeDisposable : CompositeDisposable = CompositeDisposable()

//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        return AlertDialog.Builder(requireActivity())
//            .setTitle("Impédance des électrodes")
//            .setView(R.layout.dialog_impedance)
//            .create()
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_impedance, container, false)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        AlertDialog.Builder(requireActivity())
            .setTitle("Impédance des électrodes")
            .setView(view)
            .create()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeImpedance()
        button_fermer.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        compositeDisposable.dispose()
    }

    private fun observeImpedance(){
        compositeDisposable.add(
            RxBus.listen(RxBusEvents.EventDevEvent::class.java)
                .subscribeOn(Schedulers.io())
                .map {
                    val format = DecimalFormat("#.##")

                    val devEvent = Notification.newBuilder().devEventBuilder
                    devEvent.af3 = format.format(it.devEvent.af3).toDouble()
                    devEvent.f7 = format.format(it.devEvent.f7).toDouble()
                    devEvent.f3 = format.format(it.devEvent.f3).toDouble()
                    devEvent.fc5 = format.format(it.devEvent.fc5).toDouble()
                    devEvent.t7 = format.format(it.devEvent.t7).toDouble()
                    devEvent.p7 = format.format(it.devEvent.p7).toDouble()
                    devEvent.o1 = format.format(it.devEvent.o1).toDouble()
                    devEvent.o2 = format.format(it.devEvent.o2).toDouble()
                    devEvent.p8 = format.format(it.devEvent.p8).toDouble()
                    devEvent.t8 = format.format(it.devEvent.t8).toDouble()
                    devEvent.fc6 = format.format(it.devEvent.fc6).toDouble()
                    devEvent.f4 = format.format(it.devEvent.f4).toDouble()
                    devEvent.f8 = format.format(it.devEvent.f8).toDouble()
                    devEvent.af4 = format.format(it.devEvent.af4).toDouble()

                    return@map devEvent.build()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    textview_af3_value.text = it.af3.toString()
                    textview_f7_value.text = it.f7.toString()
                    textview_f3_value.text = it.f3.toString()
                    textview_fc5_value.text = it.fc5.toString()
                    textview_t7_value.text = it.t7.toString()
                    textview_p7_value.text = it.p7.toString()
                    textview_o1_value.text = it.o1.toString()
                    textview_o2_value.text = it.o2.toString()
                    textview_p8_value.text = it.p8.toString()
                    textview_t8_value.text = it.t8.toString()
                    textview_fc6_value.text = it.fc6.toString()
                    textview_f4_value.text = it.f4.toString()
                    textview_f8_value.text = it.f8.toString()
                    textview_af4_value.text = it.af4.toString()
                }
        )
    }
}