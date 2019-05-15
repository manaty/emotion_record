package org.manaty.octopus.viewModels

import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import net.manaty.octopusync.api.*
import org.manaty.octopus.api.ECClientInterceptor


class SplashViewModel : ViewModel(){
    private val host = "10.0.2.2"
    private val port = 9991 //5432

    val compositeDisposable = CompositeDisposable()
    val isShowLoading : BehaviorSubject<Boolean> = BehaviorSubject.create()
    val showRequestError : BehaviorSubject<String> = BehaviorSubject.create()
    var requestCount = 0
    var listHeadsets : MutableList<Headset> = ArrayList()

    val channel : ManagedChannel
    private val stub : OctopuSyncGrpc.OctopuSyncBlockingStub

    init {
        channel = OkHttpChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()

        stub = OctopuSyncGrpc.OctopuSyncBlockingStub(channel)
            .withInterceptors(ECClientInterceptor())
    }

    fun requestCreateSession(headsetCode : String, deviceId : String) : Maybe<CreateSessionResponse>{
        isShowLoading.onNext(true)

        var createSessionResponse : CreateSessionResponse
        try {
            val request = CreateSessionRequest.newBuilder()
            request.deviceId = deviceId
            request.headsetCode = headsetCode

            createSessionResponse = stub.createSession(request.build())
        }
        catch (e : StatusRuntimeException){
            Logger.e(e, "requestCreateSession error")
            e.status.description?.let {
                showRequestError.onNext(it)
            }

            val session = Session.newBuilder()
            session.id = "-1"
            createSessionResponse = CreateSessionResponse.newBuilder().setSession(session).build()
        }

        isShowLoading.onNext(false)
        return Maybe.just(createSessionResponse)
            .subscribeOn(Schedulers.io())

    }

    fun requestHeadsetList() : Maybe<GetHeadsetsResponse>{
        isShowLoading.onNext(true)

        var getHeadsetsResponse : GetHeadsetsResponse
        try{
            val request = GetHeadsetsRequest.newBuilder().build()
            getHeadsetsResponse = stub.getHeadsets(request)
        }
        catch (e : StatusRuntimeException){
            Logger.e(e, "requestCreateSession error")
            e.status.description?.let {
                showRequestError.onNext(it)
            }
            getHeadsetsResponse = GetHeadsetsResponse.newBuilder().build()
        }

        return Maybe.just(getHeadsetsResponse)
            .subscribeOn(Schedulers.io())

    }


}
