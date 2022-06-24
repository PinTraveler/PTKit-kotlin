package com.example.mylibrary

import android.util.Log

class FilterManager<T>(classT: Class<T>, private val manager: CollectionManager<T>, val name: String, val limit:Int = 0, var filterFn: ((T) -> Boolean)):
    CollectionManager<T>(classT) where T: Comparable<T>{
    override val TAG = "FilterManager"
    init {
        manager.registerListener("FilterManager$name"){ event, b, a ->
            when(event){
                ObservableEvent.ADD -> {
                    val elem = a ?: return@registerListener
                    if(filterFn(elem) && (limit == 0 || elems.size < limit)){ onInternalAdd(elem) }
                }
                ObservableEvent.REMOVE -> {
                    val elem = b ?: return@registerListener
                    if(filterFn(elem) && (limit == 0 || insertionIndexOf(elem) < limit)){ onInternalRemove(elem) }
                }
                ObservableEvent.MODIFY -> {
                    val e1 = b ?: return@registerListener
                    val e2 = a ?: return@registerListener
                    if(limit == 0 || insertionIndexOf(e1) < limit) {
                        if (filterFn(e1) && !filterFn(e2))
                            onInternalRemove(e1)
                        else if (filterFn(e2) && !filterFn(e1))
                            onInternalAdd(e2)
                        else if (filterFn(e1) && filterFn(e2))
                            onInternalModify(e1, e2)
                    }
                }
            }
        }
    }

    open fun changeFilter(newFilterFn: ((T) -> Boolean)){
        filterFn = newFilterFn
        elems.toList().forEach {
            if(!filterFn(it)){
                onInternalRemove(it)
                Log.i(TAG, "Remove")
            }
        }

        manager.elems.toList().forEach {
            if(filterFn(it) && !elems.contains(it)){
                onInternalAdd(it)
                Log.i(TAG, "Add")
            }
        }
    }
}