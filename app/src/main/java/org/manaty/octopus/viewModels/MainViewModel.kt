package org.manaty.octopus.viewModels

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import com.pixplicity.easyprefs.library.Prefs
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.StreamObserver
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import net.manaty.octopusync.api.*
import org.manaty.octopus.PrefsKey
import org.manaty.octopus.api.ECClientInterceptor
import org.manaty.octopus.rxBus.RxBus
import org.manaty.octopus.rxBus.RxBusEvents
import kotlin.math.roundToInt

class MainViewModel : ViewModel(){

    var counter : MutableLiveData<Int> = MutableLiveData()
    var serverStatus : MutableLiveData<Boolean> = MutableLiveData()
    var headsetStatus : MutableLiveData<Boolean> = MutableLiveData()
    var impedanceValue : MutableLiveData<Int> = MutableLiveData()
    var isInternetAvailable = true

    val compositeDisposable = CompositeDisposable()
    val showErrorToast : PublishSubject<String> = PublishSubject.create()
    val subjectShowDialog : PublishSubject<String> = PublishSubject.create()
    val sessionStatus : PublishSubject<Boolean> = PublishSubject.create()

    val channel : ManagedChannel
    private val stub : OctopuSyncGrpc.OctopuSyncBlockingStub
    private val asyncStub : OctopuSyncGrpc.OctopuSyncStub

    lateinit var requestObserver : StreamObserver<ClientSyncMessage>
    var isInitializeConnection = true

    private val countDownTimer  = object : CountDownTimer(10000, 1000){
        override fun onTick(millisUntilFinished: Long) {
            Logger.d("timer = $millisUntilFinished")
        }

        override fun onFinish() {
            headsetStatus.postValue(false)
            isCountDownRunning = false
        }
    }
    private var isCountDownRunning = false

    init {
        channel = OkHttpChannelBuilder.forAddress(Prefs.getString(PrefsKey.HOST_KEY, "-1")
                , Prefs.getInt(PrefsKey.PORT_KEY, 0))
            .usePlaintext()
            .build()

        stub = OctopuSyncGrpc.OctopuSyncBlockingStub(channel)
            .withInterceptors(ECClientInterceptor())

        asyncStub = OctopuSyncGrpc.OctopuSyncStub(channel)
            .withInterceptors(ECClientInterceptor())

        counter.value = 0
        serverStatus.value = false
        headsetStatus.value = false
    }

    /** Note countdown for checking of headset signal
     * more than 10 secs is safe to assume that there is something wrong
     * with the headset
     */

    fun resetCountdown(){
        if (isCountDownRunning){
            countDownTimer.cancel()
        }

        countDownTimer.start()
        isCountDownRunning = true
    }

    fun computeGlobalImpedance(devEvent: DevEvent) : Int {
        var impedance = 0.00

        /** add all then divide by 56 then multiply by 100 **/
        impedance = ((devEvent.af3 + devEvent.f7 + devEvent.f3 + devEvent.fc5
        + devEvent.t7 + devEvent.p7 + devEvent.o1 + devEvent.o2
        + devEvent.p8 + devEvent.t8 + devEvent.fc6 + devEvent.f4
        + devEvent.f8 + devEvent.af4) / 56) * 100

        return impedance.roundToInt()
    }

    fun mockImpedance(){
        val devEvent = Notification.newBuilder().devEventBuilder
        devEvent.signal = Math.random().toInt()
        devEvent.af3 = Math.random()
        devEvent.f7 = Math.random()
        devEvent.f3 = Math.random()
        devEvent.fc5 = Math.random()
        devEvent.t7 = Math.random()
        devEvent.p7 = Math.random()
        devEvent.o1 = Math.random()
        devEvent.o2 = Math.random()
        devEvent.p8 = Math.random()
        devEvent.t8 = Math.random()
        devEvent.fc6 = Math.random()
        devEvent.f4 = Math.random()
        devEvent.f8 = Math.random()
        devEvent.af4 = Math.random()
        devEvent.battery = Math.random().toInt()


        impedanceValue.postValue(computeGlobalImpedance(devEvent.build()))
        RxBus.publish(RxBusEvents.EventDevEvent(devEvent.build()))
    }

    fun requestSync(){
        requestObserver = asyncStub.sync(
            object : StreamObserver<ServerSyncMessage>{
                override fun onNext(value: ServerSyncMessage?) {
                    serverStatus.postValue(true)
//                    mockImpedance()

                    when(value?.messageCase) {
                        ServerSyncMessage.MessageCase.SYNC_TIME_REQUEST -> {
                            Logger.d("message case = sync time request")

                            val request = ClientSyncMessage.newBuilder()
                            val synctimeresponse = SyncTimeResponse.newBuilder()
                            synctimeresponse.seqnum = value?.syncTimeRequest?.seqnum ?: 1L
                            synctimeresponse.receivedTimeUtc = System.currentTimeMillis()
                            request.syncTimeResponse = synctimeresponse.build()

                            requestObserver.onNext(request.build())
                        }

                        ServerSyncMessage.MessageCase.NOTIFICATION -> {
                            Logger.d("message case = notification")

                            when (value.notification.notificationCase) {
                                Notification.NotificationCase.EXPERIENCE_STARTED_EVENT -> {
                                    //TODO comment for future use
//                                    message = "Start"
//                                    sessionStatus.onNext(true)
                                }

                                Notification.NotificationCase.EXPERIENCE_STOPPED_EVENT -> {
                                    //TODO comment for future use
//                                    message = "End"
//                                    sessionStatus.onNext(false)
//                                    requestObserver.onCompleted()
                                }

                                Notification.NotificationCase.DEV_EVENT -> {
                                    resetCountdown()
                                    headsetStatus.postValue(true)
                                    impedanceValue.postValue(computeGlobalImpedance(value.notification.devEvent))
                                    RxBus.publish(RxBusEvents.EventDevEvent(value.notification.devEvent))
                                    //TODO comment for future use
//                                    message = "End"
//                                    sessionStatus.onNext(false)
//                                    requestObserver.onCompleted()
                                }

                                Notification.NotificationCase.NOTIFICATION_NOT_SET -> {

                                }
                            }

//                            Logger.d("notification $message")
//                            showErrorToast.onNext(message)
                        }

                        ServerSyncMessage.MessageCase.MESSAGE_NOT_SET -> {
                            Logger.e("Message not set")
                        }

                    }
                }

                override fun onError(t: Throwable?) {
                    serverStatus.postValue(false)
                    headsetStatus.postValue(false)
                    t?.let {
                        showErrorToast.onNext(it.cause.toString())
                    }
                    Logger.e(t, "requestSync onError")
                    onCompleted()
                }

                override fun onCompleted() {
                    Logger.d("requestSync onComplete")
                }
            })

        try{
            val request = ClientSyncMessage.newBuilder()

            if (isInitializeConnection){
                val session = Session.newBuilder()
                session.id = Prefs.getString(PrefsKey.SESSION_KEY, "-1")
                request.setSession(session)

                requestObserver.onNext(request.build())
                isInitializeConnection = false

                /** note start countdown  for headset signal**/
                countDownTimer.start()
                isCountDownRunning = true

                /** note initially set headset signal to good**/
                headsetStatus.postValue(true)
            }
        }
        catch (e : StatusRuntimeException){
            Logger.e(e,"${requestSync().toString()} catch")
            showErrorToast.onNext(e.cause.toString())
        }


    }

    fun requestUpdateState(state : State) : Maybe<UpdateStateResponse>{
        var response : UpdateStateResponse

        try{
            val session = Session.newBuilder()
            session.id = Prefs.getString(PrefsKey.SESSION_KEY, "0")
            val request = UpdateStateRequest.newBuilder()
                .setSession(session)
                .setSinceTimeUtc(System.currentTimeMillis())
                .setState(state)
                .build()

            response = stub.updateState(request)
        }
        catch (e : StatusRuntimeException){
            Logger.e(e, "requestUpdateState error")
            return Maybe.error(e)
        }

        return Maybe.just(response)
            .subscribeOn(Schedulers.io())
    }

}