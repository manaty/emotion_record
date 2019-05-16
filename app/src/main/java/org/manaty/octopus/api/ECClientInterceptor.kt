package org.manaty.octopus.api

import com.orhanobut.logger.Logger
import io.grpc.*

class ECClientInterceptor : ClientInterceptor{

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>?,
        callOptions: CallOptions?,
        next: Channel?
    ): ClientCall<ReqT, RespT> {
        return object :
            ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next?.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                super.start(object :
                    ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    override fun onHeaders(headers: Metadata) {
                        Logger.d("header received from server: $headers")
                        super.onHeaders(headers)
                    }

                    override fun onMessage(message: RespT) {
                        Logger.d("onMessage received from server: $message")
                        super.onMessage(message)
                    }
                }, headers)
            }
        }
    }
}