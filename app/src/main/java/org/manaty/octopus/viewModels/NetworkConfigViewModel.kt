package org.manaty.octopus.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import com.pixplicity.easyprefs.library.Prefs
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import net.manaty.octopusync.api.GetHeadsetsRequest
import net.manaty.octopusync.api.GetHeadsetsResponse
import net.manaty.octopusync.api.Headset
import net.manaty.octopusync.api.OctopuSyncGrpc
import org.manaty.octopus.PrefsKey
import org.manaty.octopus.api.ECClientInterceptor

class NetworkConfigViewModel : ViewModel(){

    val compositeDisposable = CompositeDisposable()
    val isRequesting : BehaviorSubject<Boolean> = BehaviorSubject.create()
    var isInternetAvailable  = false

    fun testConnection(host : String, port : Int) : Maybe<GetHeadsetsResponse>{
        Logger.d("host = $host port = $port")
        isRequesting.onNext(true)

        var channel : ManagedChannel

        var response : GetHeadsetsResponse
        try{
            channel = OkHttpChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build()

            val stub = OctopuSyncGrpc.OctopuSyncBlockingStub(channel)
                .withInterceptors(ECClientInterceptor())

            response = stub.getHeadsets(GetHeadsetsRequest
                .newBuilder().build())

            Prefs.putString(PrefsKey.HOST_KEY, host)
            Prefs.putInt(PrefsKey.PORT_KEY, port)

            }
        catch (e : StatusRuntimeException){
            isRequesting.onNext(false)
            return Maybe.error(e)
        }

        isRequesting.onNext(false)
        return Maybe.just(response)
            .subscribeOn(Schedulers.io())
            .map {
                channel.shutdown()
                return@map it
            }
    }
}