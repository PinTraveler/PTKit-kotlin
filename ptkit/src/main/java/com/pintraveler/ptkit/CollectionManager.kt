package com.pintraveler.ptkit

import android.util.Log
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
        allListeners[UUID.randomUUID().toString()] = listener
        listener(elems.map { CollectionChange(ObservableEvent.ADD, null, it) })
    }

    open fun insertionIndexOf(v: T): Int {
        synchronized(this) {
            // if element _exists_ returns index of element
            // else returns index = (-insertionpoint - 1)
            val index = elems.binarySearch(v)
            if(index >= 0)
                return index
            return -(index + 1)
        }
    }

    override fun onRegister(listener: (ObservableEvent, T?, T?) -> Unit) {
        elems.forEach { synchronized(elems){ listener(ObservableEvent.ADD, null, it) } }
    }

    override fun onInternalAdd(elem: T) {
        val index = insertionIndexOf(elem)
        synchronized(elems) {
            if (index < elems.size && elems[index].compareTo(elem) == 0) {
                Log.w(TAG, "InternalAdd: Element $elem already exists, ignoring.")
                return
            }
            Log.d(TAG, "Add $elem")
            this.elems.add(index, elem)
            onAdd(elem)
        }
    }

    override fun onInternalRemove(elem: T){
        synchronized(elems){
            val index = insertionIndexOf(elem)
            if (index < elems.size && elems[index].compareTo(elem) == 0) {
                Log.d(TAG, "Remove $elem")
                elems.removeAt(index)
                onRemove(elem)
            }
        }
    }

    override fun onInternalModify(before: T, after: T) {
        synchronized(elems) {
            val index = insertionIndexOf(after)
            if (index >= elems.size) {
                Log.d(TAG, "Add (mod) $after")
                elems.add(index, after)
                onAdd(after)
            }
            if (before.compareTo(after) == 0){
                Log.d(TAG, "Modified: Passed the same object -- really modified ($before -> $after)")
                elems[index] = after
                onModify(before, after)
            }
            else {
                Log.w(TAG, "Modified: Passed dfferent object -- is insertion index wrong? $before -> $after")
            }
        }
    }

    override fun clean() {
        super.clean()
        elems = mutableListOf()
    }

    open fun removeAt(index: Int){
        Log.i(TAG, "REMOVING AT -4- ${index}")
        synchronized(elems) {
            val elem = elems[index]
            elems.removeAt(index)
            onRemove(elem)
        }
    }

    open fun remove(elem: T){
        synchronized(elems) {
            elems.remove(elem)
            onRemove(elem)
        }
    }

    open fun insertAt(index: Int, elem: T){
        synchronized(elems) {
            elems.add(index, elem)
            onAdd(elem)
        }
    }

    open fun insert(elem: T){
        synchronized(elems){
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
            it.value(allChanged)
        }
    }
}