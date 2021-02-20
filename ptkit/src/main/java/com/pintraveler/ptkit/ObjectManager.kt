package com.pintraveler.ptkit

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class ObjectManager<T>(protected val classT: Class<T>, protected val reference: DocumentReference,
                       override val TAG: String = "ObjectManager"): Observable<T?>() {
    var data: T? = null

    private var firestoreListener: ListenerRegistration? = null

    override fun getObservableValue(): T? { return data }

    fun registerFirestoreListener(){
        firestoreListener = reference.addSnapshotListener { snapshot, err ->
            if(err != null){
                Log.e(TAG, "Error listening to document.", err)
                return@addSnapshotListener
            }
            if(snapshot == null){
                Log.w(TAG, "Null snapshot")
                return@addSnapshotListener
            }
            val oldData = data
            data = snapshot.toObject(classT)
            initialized = true
            onInternalModify(oldData, data)
        }
    }

    fun commit(merge: Boolean = false, completion: ((Exception?) -> Unit)? = null){
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
        } ?: throw NullObjectException("Committing null object")
    }

    fun deregisterFirestoreListener(){
        firestoreListener?.remove()
        firestoreListener = null
    }

    override fun clean(){
        super.clean()
        deregisterFirestoreListener()
        data = null
    }
}