package org.manaty.octopus.viewModels

import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import com.pixplicity.easyprefs.library.Prefs
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import net.manaty.octopusync.api.GetHeadsetsRequest
import net.manaty.octopusync.api.GetHeadsetsResponse
import net.manaty.octopusync.api.Headset
import net.manaty.octopusync.api.OctopuSyncGrpc
import org.manaty.octopus.PrefsKey
import org.manaty.octopus.api.ECClientInterceptor

class NetworkConfigViewModel : ViewModel(){

    val compositeDisposable = CompositeDisposable()
    val isRequesting : BehaviorSubject<Boolean> = BehaviorSubject.create()

    fun testConnection(host : String, port : Int) : Maybe<GetHeadsetsResponse>{

        isRequesting.onNext(true)

        var response : GetHeadsetsResponse
        try{
            val channel = OkHttpChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build()

            val stub = OctopuSyncGrpc.OctopuSyncBlockingStub(channel)
                .withInterceptors(ECClientInterceptor())

            response = stub.getHeadsets(GetHeadsetsRequest
                .newBuilder().build())

            Prefs.putString(PrefsKey.HOST_KEY, host)
            Prefs.putInt(PrefsKey.PORT_KEY, port)

            isRequesting.onNext(false)
            return Maybe.just(response)
                .subscribeOn(Schedulers.io())
            }
        catch (e : StatusRuntimeException){
            Logger.e(e, "testConnection error")
            response = GetHeadsetsResponse.newBuilder().build()
        }

        isRequesting.onNext(false)
        return Maybe.just(response)
    }
}