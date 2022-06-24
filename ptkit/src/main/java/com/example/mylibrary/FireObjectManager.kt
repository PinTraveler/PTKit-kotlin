package com.example.mylibrary

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

open class FireObjectManager<T>(protected val classT: Class<T>?, protected val reference: DocumentReference, register: Boolean = false,
                                override val TAG: String = "ObjectManager"): Observable<T?>() where T: FireObject {
    var data: T? = null

    private var firestoreListener: ListenerRegistration? = null

    init {
        if(register)
            registerFirestoreListener()
    }

    override fun getObservableValue(): T? { synchronized(this){ return data } }

    open fun elemModBeforeInsertion(elem: T): T{
        return elem
    }

    fun registerFirestoreListener(){
        synchronized(this) {
            firestoreListener = reference.addSnapshotListener { snapshot, err ->
                if (err != null) {
                    Log.e(TAG, "Error listening to document.", err)
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    Log.w(TAG, "Null snapshot")
                    return@addSnapshotListener
                }
                if(classT == null){
                    initialized = true
                    onInternalModify(null, null)
                    return@addSnapshotListener
                }

                val oldData = data
                data = snapshot.toObject(classT)
                initialized = true
                var modifiedElem: T? = null
                data?.let{ modifiedElem = elemModBeforeInsertion(it) }

                onInternalModify(oldData, modifiedElem)
                //NOTE: The line above is also synchronized but this is not an issue as the sync block of this function
                //      completes before a callback is invoked
            }
        }
    }

    fun commit(merge: Boolean = false, completion: ((Exception?) -> Unit)? = null){
        //NOTE: I don't believe this needs to be synchronized as it doesn't actually modify the local data
        data?.let {
            val task = if(merge) reference.set(it as Any, SetOptions.merge()) else reference.set(it as Any)
            task.addOnSuccessListener {
                Log.d(TAG, "Successfully Committed Object")
                completion?.invoke(null)
            }
            task.addOnFailureListener {
                Log.e(TAG, "Error Committing Object", it)
                completion?.invoke(it)
            }
        } ?: completion?.invoke(NullObjectException("Committing null object"))
    }

    fun deregisterFirestoreListener(){
        synchronized(this) {
            firestoreListener?.remove()
            firestoreListener = null
        }
    }

    override fun clean(){
        super.clean()
        synchronized(this) {
            deregisterFirestoreListener()
            data = null
        }
    }
}