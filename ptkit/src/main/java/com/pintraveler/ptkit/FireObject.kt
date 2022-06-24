package com.pintraveler.ptkit

import com.google.firebase.firestore.Exclude

abstract class FireObject: Comparable<FireObject> {
    @get:Exclude open var _id: String = ""
    @get:Exclude open var _index: Int = 0
    override fun compareTo(other: FireObject): Int { return _id.compareTo(other._id) }
}