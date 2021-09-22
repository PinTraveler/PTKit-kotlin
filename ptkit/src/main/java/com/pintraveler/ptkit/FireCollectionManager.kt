package com.pintraveler.ptkit

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

open class FireCollectionManager<T>(classT: Class<T>, protected val reference: CollectionReference,
                                    protected var query: Query = reference, TAG: String, register: Boolean = false, private val sanityFilter: ((T) -> Boolean) = { true }):
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
                    Log.e(TAG, "Error listening to collection!")
                } else if (snap != null) {
                    firestoreInitialized = true
                    var allChanged = mutableListOf<T>()
                    snap.documentChanges.forEach {
                        val elem = it.document.toObject(classT)
                        elem._id = it.document.id
                        val modifiedElem = elemModBeforeInsertion(elem)
                        if(!sanityFilter(modifiedElem))
                            return@forEach

                        allChanged.add(modifiedElem)
                        Log.i(TAG, "ADD ${elems.size}")
                        when (it.type) {
                            DocumentChange.Type.ADDED -> onInternalAdd(modifiedElem)
                            DocumentChange.Type.MODIFIED -> onInternalModify(modifiedElem, modifiedElem)
                            DocumentChange.Type.REMOVED -> onInternalRemove(modifiedElem)
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
        reference.document(id).delete().addOnCompleteListener { completion?.invoke(it.exception) }
    }

    open fun removeAt(index: Int, completion: ((Exception?) -> Unit)? = null) {
        Log.i(TAG, "REMOVING AT -3- $index ${elems.size} ${elems[index]._id}")
        removeByID(elems[index]._id, completion)
    }

    // metamorphism.
    override fun removeAt(index: Int) {
        removeAt(index, null)
    }

    open fun insert(elem: T, withID: String? = null, completion: ((Exception?) -> Unit)? = null) {
        if(withID == null)
            reference.add(elem).addOnCompleteListener { completion?.invoke(it.exception) }
        else
            reference.document(withID).set(elem).addOnCompleteListener { completion?.invoke(it.exception) }
    }

    open fun getByID(id: String): T? {
        return elems.find{ it._id == id }
    }

    open fun onAllChanges(allChanged: List<T>){}

}