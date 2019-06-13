package org.manaty.octopus.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orhanobut.logger.Logger
import com.pixplicity.easyprefs.library.Prefs
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.StreamObserver
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import net.manaty.octopusync.api.*
import org.manaty.octopus.ECApplication
import org.manaty.octopus.PrefsKey
import org.manaty.octopus.api.ECClientInterceptor

class MainViewModel : ViewModel(){

    var counter : MutableLiveData<Int> = MutableLiveData()
    var serverStatus : MutableLiveData<Boolean> = MutableLiveData()
    var headsetStatus : MutableLiveData<Boolean> = MutableLiveData()
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

    fun requestSync(){
        requestObserver = asyncStub.sync(
            object : StreamObserver<ServerSyncMessage>{
                override fun onNext(value: ServerSyncMessage?) {

                    value?.syncTimeRequest?.let {
                        Logger.d("syncTimeRequest ${it.seqnum}" )

                        val request = ClientSyncMessage.newBuilder()
                        val synctimeresponse = SyncTimeResponse.newBuilder()
                        synctimeresponse.seqnum = value?.syncTimeRequest?.seqnum ?: 1L
                        synctimeresponse.receivedTimeUtc = System.currentTimeMillis()
                        request.syncTimeResponse = synctimeresponse.build()

                        requestObserver.onNext(request.build())
                    }

                    value?.notification?.let {
                        var message = " "
                        it.experienceStartedEvent?.let {
                            message = "Start"
                        }

                        it.experienceStoppedEvent?.let {
                            message = "End"
//                            requestObserver.onCompleted()
                        }

                        Logger.d("notification $message")
                        showErrorToast.onNext(message)

                    }
                    serverStatus.postValue(true)
                }

                override fun onError(t: Throwable?) {
                    serverStatus.postValue(false)
                    Logger.e(t, "onError")
                }

                override fun onCompleted() {
                    Logger.d("onComplete")
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