package com.pintraveler.ptkit

import android.util.Log
import com.google.firebase.firestore.*

open class FireCollectionManager<T>(classT: Class<T>, protected val reference: CollectionReference,
                                    protected var query: Query = reference, TAG: String, register: Boolean = false,
                                    private val sanityFilter: ((T) -> Boolean) = { true }):
    CollectionManager<T>(classT, TAG) where T: FireObject {

    protected var collectionListener: ListenerRegistration? = null
    protected var firestoreInitialized = false

    init {
        if(register)
            registerFirestoreListener()
    }

    open fun elemModBeforeInsertion(elem: T): T{
        return elem
    }

    fun registerFirestoreListener() {
        synchronized(this) {
            collectionListener = query.addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "Error listening to collection!", err)
                } else if (snap != null) {
                    firestoreInitialized = true
                    var allChanged = mutableListOf<CollectionChange<T>>()
                    snap.documentChanges.forEach {
                        try {
                            val elem = it.document.toObject(classT)
                            elem._id = it.document.id
                            val before = getByID(elem._id)
                            val modifiedElem = elemModBeforeInsertion(elem)
                            if (!sanityFilter(modifiedElem))
                                return@forEach

                            Log.i(TAG, "ADD ${elems.size}")
                            when (it.type) {
                                DocumentChange.Type.ADDED -> {
                                    onInternalAdd(modifiedElem)
                                    allChanged.add(CollectionChange(ObservableEvent.ADD, before, elem))
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    if(before == null)
                                        Log.w(TAG, "BEFORE IS NULL for after $modifiedElem")
                                    onInternalModify(before ?: modifiedElem, modifiedElem)
                                    allChanged.add(CollectionChange(ObservableEvent.MODIFY, before, elem))
                                }
                                DocumentChange.Type.REMOVED -> {
                                    onInternalRemove(modifiedElem)
                                    allChanged.add(CollectionChange(ObservableEvent.REMOVE, before, null))
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing document ${it.document.id}! ${it.document.data}", e)
                        }
                    }
                    onAllChanges(allChanged)
                }
            }
            initialized = true
        }
    }

    fun deregisterFirestoreListener(){
        synchronized(this){
            collectionListener?.remove()
            collectionListener = null
        }
    }

    override fun clean() {
        super.clean()
        deregisterFirestoreListener()
        synchronized(elems){
            elems.forEach {
                elems.remove(it)
                onRemove(it)
            }
        }
    }

    open fun removeByID(id: String, completion: ((Exception?) -> Unit)? = null) {
        Log.i(TAG, "Removing by id $id")
        reference.document(id).delete().addOnCompleteListener {
            Log.i(TAG, "Remove ${it.isSuccessful} exception ? ${it.exception}")
            completion?.invoke(it.exception)
        }
    }

    open fun removeAt(index: Int, completion: ((Exception?) -> Unit)? = null) {
        Log.i(TAG, "REMOVING AT -3- $index ${elems.size} ${elems[index]._id}")
        removeByID(elems[index]._id, completion)
    }

    // metamorphism.
    override fun removeAt(index: Int) {
        removeAt(index, null)
    }

    override fun remove(elem: T) {
        removeByID(elem._id)
    }

    open fun insert(elem: T, withID: String? = null, completion: ((Exception?) -> Unit)? = null) {
        if(withID == null)
            reference.add(elem).addOnCompleteListener { completion?.invoke(it.exception) }
        else
            reference.document(withID).set(elem).addOnCompleteListener { completion?.invoke(it.exception) }
    }

    override fun insert(elem: T) {
        insert(elem, null)
    }

    open fun getByID(id: String): T? {
        return elems.find{ it._id == id }
    }

    open fun update(id: String, updates: Map<String, Any>) {
        reference.document(id).set(updates, SetOptions.merge())
    }

    open fun update(elem: T, updates: Map<String, Any>) {
        update(elem._id, updates)
    }

    override fun onAllChanges(allChanged: List<CollectionChange<T>>) {
        super.onAllChanges(allChanged)
    }
}