package com.pintraveler.ptkit

import android.provider.Settings.Global
import android.util.Log
import com.google.firebase.firestore.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

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

    suspend fun onDocumentChange(doc: DocumentChange): CollectionChange<T>? {
        val elem = doc.document.toObject(classT)
        elem._id = doc.document.id
        val before = getByID(elem._id)
        val modifiedElem = elemModBeforeInsertion(elem)
        if (!sanityFilter(modifiedElem)) {
            Log.w(TAG, "Sanity filter failed for $modifiedElem")
            return null
        }

        Log.i(TAG, "ADD ${elems.size}")
        when (doc.type) {
            DocumentChange.Type.ADDED -> {
                onInternalAddNoLock(modifiedElem)
                return CollectionChange(ObservableEvent.ADD, before, elem)
            }
            DocumentChange.Type.MODIFIED -> {
                if(before == null)
                    Log.w(TAG, "BEFORE IS NULL for after $modifiedElem")
                onInternalModifyNoLock(before ?: modifiedElem, modifiedElem)
                return CollectionChange(ObservableEvent.MODIFY, before, elem)
            }
            DocumentChange.Type.REMOVED -> {
                onInternalRemoveNoLock(modifiedElem)
                return CollectionChange(ObservableEvent.REMOVE, before, null)
            }
        }
    }

    fun registerFirestoreListener() {
        collectionListener = query.addSnapshotListener { snap, err ->
            firestoreInitialized = true
            if (err != null) {
                Log.e(TAG, "Error listening to collection!", err)
                return@addSnapshotListener
            } else if (snap == null) {
                Log.e(TAG, "Null snapshot!")
                return@addSnapshotListener
            }
            val allChanged = mutableListOf<CollectionChange<T>>()
            GlobalScope.launch {//NOTE: RISKY. I DONT KNOW WHAT IM DOING HERE
                snap.documentChanges.forEach {
                    try {
                        val change = onDocumentChange(it)
                        if (change != null)
                            allChanged.add(change)
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error parsing document ${it.document.id}! ${it.document.data}",
                            e
                        )
                    }
                }
                onAllChanges(allChanged)
            }
        }
        initialized = true
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
        GlobalScope.launch {
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
    override suspend fun removeAt(index: Int) {
        removeAt(index, null)
    }

    override suspend fun remove(elem: T) {
        removeByID(elem._id)
    }

    open fun insert(elem: T, withID: String? = null, completion: ((Exception?) -> Unit)? = null) {
        if(withID == null)
            reference.add(elem).addOnCompleteListener { completion?.invoke(it.exception) }
        else
            reference.document(withID).set(elem).addOnCompleteListener { completion?.invoke(it.exception) }
    }

    override suspend fun insert(elem: T) {
        insert(elem, null)
    }

    open suspend fun getByID(id: String): T? {
        dataMutex.withLock {
            return elems.find { it._id == id }
        }
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