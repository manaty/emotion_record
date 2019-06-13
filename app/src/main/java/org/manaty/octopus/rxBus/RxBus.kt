package org.manaty.octopus.rxBus

import com.orhanobut.logger.Logger
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * reference
 * https://android.jlelse.eu/rxbus-kotlin-listen-where-ever-you-want-e6fc0760a4a8
 */

object RxBus {
    private val publisher = PublishSubject.create<Any>()

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    // Listen should return an Observable and not the publisher
    // Using ofType we filter only events that match that class type
    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}