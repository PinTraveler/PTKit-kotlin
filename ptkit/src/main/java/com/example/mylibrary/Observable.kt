package com.example.mylibrary

import android.util.Log

enum class ObservableEvent{ ADD, REMOVE, MODIFY }

abstract class Observable<T> {
    private var listeners: MutableMap<String, (ObservableEvent, T?, T?) -> Unit> = mutableMapOf()

    protected abstract val TAG: String
    protected var initialized = false

    open fun getObservableValue(): T? { return null }

    protected fun onEvent(eventType: ObservableEvent, before: T?, after: T?){
        synchronized(listeners) {
            for ((_, f) in listeners) {
                f(eventType, before, after)
            }
        }
    }

    protected open fun onInternalAdd(elem: T){ onAdd(elem) }
    protected open fun onInternalRemove(elem: T){ onRemove(elem) }
    protected open fun onInternalModify(before: T, after: T){ onModify(before, after) }

    protected fun onAdd(after: T){ onEvent(ObservableEvent.ADD, null, after) }
    protected fun onRemove(before: T){ onEvent(ObservableEvent.REMOVE, before, null) }
    protected fun onModify(before: T, after: T){ onEvent(ObservableEvent.MODIFY, before, after) }

    open fun onRegister(listener: (ObservableEvent, T?, T?) -> Unit){
        if(initialized)
            synchronized(listeners) {
                listener(ObservableEvent.MODIFY, getObservableValue(), getObservableValue())
            }
    }

    open fun clean(){
        synchronized(listeners) {
            listeners = mutableMapOf()
        }
    }

    fun registerListener(name: String, listener: (ObservableEvent, T?, T?) -> Unit){
        synchronized(listeners){
            Log.d(TAG, "New Listener -- $name")
            onRegister(listener)
            listeners.put(name, listener)
        }
    }

    fun removeListener(name: String){
        synchronized(listeners) {
            Log.d(TAG, "Removed Listener -- $name")
            listeners.remove(name)
        }
    }
}