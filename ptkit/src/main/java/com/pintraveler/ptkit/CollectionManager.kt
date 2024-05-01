package com.pintraveler.ptkit

import android.util.Log
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class CollectionChange<T>(
    val event: ObservableEvent,
    val before: T?,
    val after: T?
) where T: Comparable<T>
open class CollectionManager<T>(protected val classT: Class<T>, override val TAG: String = "CollectionManager"): Observable<T>()
  where T: Comparable<T>
{
    private var allListeners: MutableMap<String, (List<CollectionChange<T>>) -> Unit> = mutableMapOf()

    //NOTE: No point synchronizing get/set as this is a reference
    var elems: MutableList<T> = mutableListOf()

    fun registerAllChangeListener(name: String, listener: (List<CollectionChange<T>>) -> Unit) {
        Log.i(TAG, "Registering all listener $name")
        if(allListeners.keys.contains(name))
            Log.w(TAG, "Overwriting existing listener $name!")
        allListeners[name] = listener
        listener(elems.map { CollectionChange(ObservableEvent.ADD, null, it) })
    }

    fun removeAllChangeListener(name: String) {
        Log.i(TAG, "Removing all listener $name")
        allListeners.remove(name)
    }

    open fun insertionIndexOf(v: T): Int {
        // if element _exists_ returns index of element
        // else returns index = (-insertionpoint - 1)
        val index = elems.binarySearch(v)
        if(index >= 0)
            return index
        return -(index + 1)
    }

    override fun onRegister(listener: (ObservableEvent, T?, T?) -> Unit) {
        elems.forEach { synchronized(elems){ listener(ObservableEvent.ADD, null, it) } }
    }

    suspend fun onInternalAddNoLock(elem: T) {
        val index = insertionIndexOf(elem)
        if (index < elems.size && elems[index].compareTo(elem) == 0) {
            Log.w(TAG, "InternalAdd: Element $elem already exists, ignoring.")
            return
        }
        Log.d(TAG, "Add $elem")
        this.elems.add(index, elem)
        onAdd(elem)

    }

    suspend fun onInternalRemoveNoLock(elem: T) {
        val index = insertionIndexOf(elem)
        if (index < elems.size && elems[index].compareTo(elem) == 0) {
            Log.d(TAG, "Remove $elem")
            elems.removeAt(index)
            onRemove(elem)
        }
    }

    suspend fun onInternalModifyNoLock(before: T, after: T) {
        val index = insertionIndexOf(after)
        if (index >= elems.size) {
            Log.d(TAG, "Add (mod) $after")
            elems.add(index, after)
            onAdd(after)
        }
        if (before.compareTo(after) == 0) {
            Log.d(TAG, "Modified: Passed the same object -- really modified ($before -> $after)")
            elems[index] = after
            onModify(before, after)
        } else {
            Log.w(TAG, "Modified: Passed dfferent object -- is insertion index wrong? $before -> $after"
            )
        }
    }
    override suspend fun onInternalAdd(elem: T) {
        dataMutex.withLock {
            onInternalAddNoLock(elem)
        }
    }

    override suspend fun onInternalRemove(elem: T){
        dataMutex.withLock {
            onInternalRemoveNoLock(elem)
        }
    }

    override suspend fun onInternalModify(before: T, after: T) {
        dataMutex.withLock {
            onInternalModifyNoLock(before, after)
        }
    }

    override fun clean() {
        super.clean()
        elems = mutableListOf()
    }

    open suspend fun removeAt(index: Int){
        dataMutex.withLock {
            Log.i(TAG, "REMOVING AT -4- ${index}")
            val elem = elems[index]
            elems.removeAt(index)
            onRemove(elem)
        }
    }

    open suspend fun remove(elem: T){
        dataMutex.withLock {
            elems.remove(elem)
            onRemove(elem)
        }
    }

    open suspend fun insertAt(index: Int, elem: T){
        dataMutex.withLock {
            elems.add(index, elem)
            onAdd(elem)
        }
    }

    open suspend fun insert(elem: T){
        dataMutex.withLock {
            elems.add(elem)
            onAdd(elem)
        }
    }

    open fun onAllChanges(allChanged: List<CollectionChange<T>>) {
        val add = allChanged.filter { it.event == ObservableEvent.ADD }.size
        val change = allChanged.filter { it.event == ObservableEvent.MODIFY }.size
        val remove = allChanged.filter { it.event == ObservableEvent.REMOVE }.size
        Log.i(TAG, "All Changes: ${allChanged.size} changes $add, $change, $remove")
        allListeners.forEach {
            try {
                Log.i(TAG, "Notifying listener ${it.key}")
                it.value(allChanged)
            }
            catch (e: Exception) {
                Log.e(TAG, "Error notifying listener ${it.key} $e")
            }
        }
    }
}