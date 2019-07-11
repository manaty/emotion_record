package org.manaty.octopus.rxBus

import net.manaty.octopusync.api.DevEvent

class RxBusEvents {
    data class EventInternetStatus(val isInternetAvailable : Boolean)
    data class EventDevEvent(var devEvent: DevEvent)
}