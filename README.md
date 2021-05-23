# FireKit
FireKit is a utility library that provides an Observable framework and a direct observable interface to Firebase functionality. 

# Observable
Every FireKit class that directly interfaces with Firebase functionality is an "Observable" object. Observable objects have an internal state that can be of any datatype (a boolean, integer, complex data class, list of classes, etc.). Each observable is an observable of this datatype that is internally called `T`.
```kotlin
abstract class Observable<T>{ ... }
```

## State
Each observable holds an internal state of type `T?`, i.e. the state is either null or an object of type `T`. 


### Accessing State
At any point,without any event being fired _or_ in an event hook, you can access the current state of an observable object by calling the function `getObservableValue`. This function by default returns `null` in the abstract base class `Observable` and must be overriden by classes inheriting from `Observable`.
> NOTE: It migh be a good idea to make this method abstract _OR_ provide a default state holding mechanism for the abstract base class
```kotlin
val myObservable = object: Observable<Int>{ ... }
val myValue: Int = myObservable.getObservableValue()
```

### Holding State
The default abstract class `Observable` does not have any way to explicitly hold this state and subclasses are supposed to implement a way to hold state. Most simply this can be something like

```kotlin
class SimpleObservable<T>(private var value: T): Observable<T>{
    override fun getObservableValue(): T? { return value }
}
```

### Modifying State
Since there are a variety of ways to hold state, there are multiple ways to modify the state as well. For any changes to trigger a change event, any class inheriting from the abstract base class `Observable` must call one of `onInternalModify`, `onInternalAdd`, or `onInternalRemove` after a change in the underlying state.
```kotlin
class SimpleObservable<T>(private var value: T): Observable<T>{
    override fun getObservableValue(): T? { return value }
    fun setObservableValue(newVal: T){
        val oldVal = getObservableValue()
        value = newVal
        onInternalModify(oldVal, value)
    }
}
```
## Listeners
Observable objects expose an event hook that you can register listeners to, to be called every time a change happens to the internal state. Every function you register to an observable object will get called once on every change event with an eventType, the state before the change and the state after the change. You can read more about event types below. These functions can be defined like
```kotlin
fun onObservableChange(eventType: ObservableEvent, before: T?, after: T?){
    when(eventType){
        ADD -> { ... }
        REMOVE -> { ... }
        MODIFY -> { ... }
    }
}
```

### Registering & Deregistering
You can register a listener to an observable object at any time by specifying a `name` for the listener you are registering, and an actual listener function to be called on changes. Each listener _must_ have a unique name. If you try to register a second listener with the same name, the first one will be overriden.
> NOTE: Maybe this should throw an error instead?

You can register a listener to an observable object by calling the `registerListener` function, and remove that listener by calling the `deregisterListener` function.
```kotlin
val myObservable = object: Observable<Int>{ ... }
myObservable.registerListener("listener1"){ event, before, after -> 
  ...
}
fun listener2(event: ObservableEvent, before: Int?, after: Int?){ ... }
myObservable.registerListener("listener2", listener2)
myObservable.deregisterListener("listener1")
```

### Initial Registration
On initial registration, every listener function is called with the `MODIFY` type and current state as both the before and after values.

## Observable Event Types
There are three observable event types that are specified as the enum `ObservableEvent`. These are `ADD`, `REMOVE`, and `MODIFY`. Admittedly `REMOVE` and `MODIFY` don't serve any purpose for simple data types like `Int`, `Bool`, etc. For these, every event should fire as `MODIFY`. For more complex state types like lists or maps, it may make sense to define the observable type `T` as the type held by the list and propagate list/map additions/removals as `ADD` or `REMOVE` events for the individual elements.

# FireKit Auth
## Auth Manager
FireKit Auth is a wrapper around FirebaseAuth that exposes auth functionality in an event-driven fashion. `AuthManager` holds an enum `AuthState` state `UNINITIALIZED`, `AUTHENTICATED` or `UNAUTHENTICATED`. Every time an auth state change occurs, for exaple when a user logs in and state changes from `UNAUTHENTICATED` to `AUTHENTICATED`, the listener functions are called.

### Sample Usage
AuthManager is intended to be used in a reactive-like fashion to build Authentication Workflows. One can for example have a LandingActivity be the entrypoint to the app and handle auth changes as follows:
```kotlin
class LandingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?){
        val authManager = AuthManager()
        authManager.startAuthListener()
        authManager.registerListener("landingActivity"){ _, _, s ->
            when(s){
                AUTHENTICATED -> { /* Present Main App */ }
                UNAUTHENTICATED -> { /* Present Login/Signup Workflow */
                UNINITIALIZED -> { /* Present Loading Screen */ }
                }
            }
        }
    }
}
```
# FireKit Firestore
FireKit provides two observable classes `FireObjectManager` and `FireCollectionManager` that are intended to mirror the state of a firestore document or collection as a kotlin class and propagate any change events via the `Observable` framework. Both classes require  a data type to be defined as a data class inheriting from the base class `FireObject`

## FireObject
FireObject is the abstract base class that provides holders for the database ID and collection index. It is defined as
```kotlin
abstract class FireObject: Comparable<FireObject> {
    @get:Exclude open var _id: String = ""
    @get:Exclude open var index: Int = 0
    override fun compareTo(other: FireObject): Int { return _id.compareTo(other._id) }
}
```
The `index` variable and comparison functions are only relevant to the FireCollectionManager use case and can be ignored if you are only going to be using this object to monitor documents and not collections. The parameter `_id` holds the database id of each document, `index` holds the index of the document in the list of documents held by `FireCollectionManager`, `compareTo` decides this index. You can create data classes to mimick your firestore objects:
```kotlin
data class Profile(
    val username: String = "",
    val appOpenCounter: Long = 0L,
    val lastLogin: Timestamp = Timestamp(),
    val isPremium: Boolean = false
): FireObject()
```

## FireObjectManager
FireObjectManager simply mirrors the state of a single database object in a kotlin class. You can initialize a FireObjectManager as follows:
```kotlin
val userID = authManager.currentUser.uid ?: return
val profileManager = FireObjectManger<Profile>(Firebase.firestore.collection("profiles").document(userID))
profileManager.registerFirestoreListener()
profileManager.registerListener("myListener"){ _, _, after ->
    Log.i("TEST", "$after")
}
```
Note that you need to manually register the actual firestore listener after creation.
# FireKit UI Elements
