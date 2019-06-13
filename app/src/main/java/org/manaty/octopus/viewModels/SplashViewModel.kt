package org.manaty.octopus.viewModels

import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import com.pixplicity.easyprefs.library.Prefs
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import net.manaty.octopusync.api.*
import org.manaty.octopus.PrefsKey
import org.manaty.octopus.api.ECClientInterceptor


class SplashViewModel : ViewModel(){
//    private val host = "10.0.2.2"
//    private val port = 9991 //5432

    val compositeDisposable = CompositeDisposable()
    val isShowLoading : BehaviorSubject<Boolean> = BehaviorSubject.create()
    val showRequestError : BehaviorSubject<String> = BehaviorSubject.create()
    var requestCount = 0
    var listHeadsets : MutableList<Headset> = ArrayList()
    var isInternetAvailable  = true

    val channel : ManagedChannel
    private val stub : OctopuSyncGrpc.OctopuSyncBlockingStub

    init {
        channel = OkHttpChannelBuilder.forAddress(Prefs.getString(PrefsKey.HOST_KEY, "-1")
                , Prefs.getInt(PrefsKey.PORT_KEY, 0))
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

            isShowLoading.onNext(false)
        }
        catch (e : StatusRuntimeException){
            isShowLoading.onNext(false)
            Logger.e(e, "requestCreateSession error")
            return Maybe.error(e)
        }

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
            isShowLoading.onNext(false)
            Logger.e(e, "requestCreateSession error")
            return Maybe.error(e)
        }

        return Maybe.just(getHeadsetsResponse)
            .subscribeOn(Schedulers.io())

    }


}
