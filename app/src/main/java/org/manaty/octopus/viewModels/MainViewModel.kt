package org.manaty.octopus.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import com.pixplicity.easyprefs.library.Prefs
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.StreamObserver
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import net.manaty.octopusync.api.*
import org.manaty.octopus.PrefsKey
import org.manaty.octopus.api.ECClientInterceptor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel(){
    private val host = "10.0.2.2"
    private val port = 9991 //5432

    var counter : MutableLiveData<Int> = MutableLiveData() !!
    var serverStatus : MutableLiveData<Boolean> = MutableLiveData() !!
    var headsetStatus : MutableLiveData<Boolean> = MutableLiveData() !!

    val compositeDisposable = CompositeDisposable()
    val showErrorToast : BehaviorSubject<String> = BehaviorSubject.create()
    val channel : ManagedChannel
    val stub : OctopuSyncGrpc.OctopuSyncBlockingStub
    val asyncStub : OctopuSyncGrpc.OctopuSyncStub

    lateinit var requestObserver : StreamObserver<ClientSyncMessage>
    var isInitializeConnection = true

    init {
        channel = OkHttpChannelBuilder.forAddress(host, port)
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
                    val request = ClientSyncMessage.newBuilder()
                    val synctimeresponse = SyncTimeResponse.newBuilder()
                    synctimeresponse.seqnum = value?.syncTimeRequest?.seqnum ?: 1L
                    synctimeresponse.receivedTimeUtc = System.currentTimeMillis()
                    request.syncTimeResponse = synctimeresponse.build()

                    requestObserver.onNext(request.build())
                }

                override fun onError(t: Throwable?) {
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
            e.status.description?.let {
                showErrorToast.onNext(it)
            }
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
            e.status.description?.let {
                it
            }

            response = UpdateStateResponse.newBuilder()
                .build()
        }

        return Maybe.just(response)
            .subscribeOn(Schedulers.io())
    }

}