package com.pintraveler.ptkit

import android.util.Log

class FilterManager<T>(classT: Class<T>, private val manager: CollectionManager<T>, val name: String, val limit:Int = 0, var filterFn: ((T) -> Boolean)):
    CollectionManager<T>(classT) where T: Comparable<T>{
    override val TAG = "FilterManager$name"
    init {
        manager.registerListener(TAG){ event, b, a ->
            var allChanged = mutableListOf<CollectionChange<T>>()
            when(event){
                ObservableEvent.ADD -> {
                    val elem = a ?: return@registerListener
                    if(filterFn(elem) && (limit == 0 || elems.size < limit)){
                        onInternalAdd(elem)
                        allChanged.add(CollectionChange(ObservableEvent.ADD, null, elem))
                    }
                }
                ObservableEvent.REMOVE -> {
                    val elem = b ?: return@registerListener
                    if(filterFn(elem) && (limit == 0 || insertionIndexOf(elem) < limit)){
                        onInternalRemove(elem)
                        allChanged.add(CollectionChange(ObservableEvent.REMOVE, elem, null))
                    }
                }
                ObservableEvent.MODIFY -> {
                    val e1 = b ?: return@registerListener
                    val e2 = a ?: return@registerListener
                    if(limit == 0 || insertionIndexOf(e1) < limit) {
                        if (filterFn(e1) && !filterFn(e2)) {
                            onInternalRemove(e1)
                            allChanged.add(CollectionChange(ObservableEvent.REMOVE, e1, null))
                        }
                        else if (filterFn(e2) && !filterFn(e1)) {
                            onInternalAdd(e2)
                            allChanged.add(CollectionChange(ObservableEvent.ADD, null, e2))
                        }
                        else if (filterFn(e1) && filterFn(e2)) {
                            onInternalModify(e1, e2)
                            allChanged.add(CollectionChange(ObservableEvent.MODIFY, e1, e2))
                        }
                    }
                }
            }
            if(allChanged.isNotEmpty())
                onAllChanges(allChanged)
        }
    }

    open fun changeFilter(newFilterFn: ((T) -> Boolean)){
        Log.i(TAG, "changing filter")
        filterFn = newFilterFn
        var allChanged = mutableListOf<CollectionChange<T>>()
        elems.toList().forEach {
            if(!filterFn(it)){
                onInternalRemove(it)
                allChanged.add(CollectionChange(ObservableEvent.REMOVE, it, null))
                Log.i(TAG, "Remove")
            }
        }

        manager.elems.toList().forEach {
            if(filterFn(it) && !elems.contains(it)){
                onInternalAdd(it)
                allChanged.add(CollectionChange(ObservableEvent.ADD, it, null))
                Log.i(TAG, "Add")
            }
        }
        if(allChanged.isNotEmpty())
            onAllChanges(allChanged)
    }
}