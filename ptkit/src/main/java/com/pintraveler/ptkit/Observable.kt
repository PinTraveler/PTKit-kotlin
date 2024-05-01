package com.pintraveler.ptkit

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ObservableEvent{ ADD, REMOVE, MODIFY }

abstract class Observable<T> {
    private var listeners: MutableMap<String, (ObservableEvent, T?, T?) -> Unit> = mutableMapOf()

    protected abstract val TAG: String
    protected var initialized = false

    protected var listenerMutex = Mutex(false)
    protected var dataMutex = Mutex(false)

    open fun getObservableValue(): T? { return null }

    protected suspend fun onEvent(eventType: ObservableEvent, before: T?, after: T?){
        listenerMutex.withLock {
            for ((_, f) in listeners) {
                f(eventType, before, after)
            }
        }
    }

    protected open suspend fun onInternalAdd(elem: T){
        dataMutex.withLock {
            onAdd(elem)
        }
    }
    protected open suspend fun onInternalRemove(elem: T) {
        dataMutex.withLock {
            onRemove(elem)
        }
    }
    protected open suspend fun onInternalModify(before: T, after: T) {
        dataMutex.withLock {
            onModify(before, after)
        }
    }

    protected suspend fun onAdd(after: T){
        onEvent(ObservableEvent.ADD, null, after)
    }

    protected suspend fun onRemove(before: T){
        onEvent(ObservableEvent.REMOVE, before, null)
    }
    protected suspend fun onModify(before: T, after: T){
        onEvent(ObservableEvent.MODIFY, before, after)
    }

    open fun onRegister(listener: (ObservableEvent, T?, T?) -> Unit){
        if (initialized)
            listener(ObservableEvent.MODIFY, getObservableValue(), getObservableValue())
    }

    open fun clean(){
        //NOTE: Not putting a mutex here as likely unnecessary and don't wanna deal w coroutines
        listeners = mutableMapOf()
    }

    fun registerListener(name: String, listener: (ObservableEvent, T?, T?) -> Unit){
        //NOTE: Not putting a mutex here as likely unnecessary and don't wanna deal w coroutines
        Log.d(TAG, "New Listener -- $name")
        onRegister(listener)
        listeners.put(name, listener)
    }

    fun removeListener(name: String){
        Log.d(TAG, "Removed Listener -- $name")
        listeners.remove(name)
    }
}