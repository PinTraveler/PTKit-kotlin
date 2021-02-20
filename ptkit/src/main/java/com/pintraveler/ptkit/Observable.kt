package com.pintraveler.ptkit

import android.util.Log
import java.util.concurrent.locks.ReentrantLock

enum class ObservableEvent{ ADD, REMOVE, MODIFY }

abstract class Observable<T> {
    private var listeners: MutableMap<String, (ObservableEvent, T?, T?) -> Unit> = mutableMapOf()

    protected abstract val TAG: String
    protected val lock = ReentrantLock()
    protected var initialized = false

    abstract fun getObservableValue(): T

    protected fun onEvent(eventType: ObservableEvent, before: T?, after: T?){
        lock.lock()
        for((_,f) in listeners){
            f(eventType, before, after)
        }
        lock.unlock()
    }

    protected open fun onInternalAdd(after: T){ onAdd(after) }
    protected open fun onInternalRemove(before: T){ onRemove(before) }
    protected open fun onInternalModify(before: T, after: T){ onModify(before, after) }

    protected fun onAdd(after: T){ onEvent(ObservableEvent.ADD, null, after); }
    protected fun onRemove(before: T){ onEvent(ObservableEvent.REMOVE, before, null) }
    protected fun onModify(before: T, after: T){ onEvent(ObservableEvent.MODIFY, before, after) }

    open fun onRegister(listener: (ObservableEvent, T, T) -> Unit){
        if(initialized)
            listener(ObservableEvent.MODIFY, getObservableValue(), getObservableValue())
    }

    open fun clean(){
        lock.lock()
        listeners = mutableMapOf()
        lock.unlock()
    }

    fun registerListener(name: String, listener: (ObservableEvent, T?, T?) -> Unit){
        lock.lock()
        Log.d(TAG, "New Listener -- $name")
        onRegister(listener)
        listeners.put(name, listener)
        lock.unlock()
    }

    fun removeListener(name: String){
        lock.lock()
        Log.d(TAG, "Removed Listener -- $name")
        listeners.remove(name)
        lock.unlock()
    }
}