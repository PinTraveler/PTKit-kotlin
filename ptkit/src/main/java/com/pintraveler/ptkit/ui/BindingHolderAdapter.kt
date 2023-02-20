package com.pintraveler.ptkit.ui

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.pintraveler.ptkit.CollectionManager
import com.pintraveler.ptkit.ConflictingParametersException
import com.pintraveler.ptkit.ObservableEvent

enum class HolderType { EMPTY, ITEM, FIRST, LAST }
open class FireBindingViewHolder<T>(private val binding: ViewBinding,
                                    private val bindFn: ((T, Int, ViewBinding) -> Unit)?,
                                    private val firstBindFn: ((ViewBinding) -> Unit)? = null,
                                    private val lastBindFn: ((ViewBinding) -> Unit)? = null,
                                    private val emptyBindFn: ((ViewBinding) -> Unit)? = null) : RecyclerView.ViewHolder(binding.root){
    protected open val TAG = "PTItemHolder"
    open fun bind(elem: T?, position: Int, type: HolderType) {
        when(type) {
            HolderType.EMPTY -> emptyBindFn?.invoke(binding)
            HolderType.ITEM -> bindFn?.invoke(elem ?: return, position, binding)
            HolderType.FIRST -> firstBindFn?.invoke(binding)
            HolderType.LAST -> lastBindFn?.invoke(binding)
        }
    }
}

open class FireBindingRecyclerViewAdapter<T>(
                                      private val createViewBinding: (ViewGroup) -> ViewBinding,
                                      private val createEmptyViewBinding: ((ViewGroup) -> ViewBinding)? = null,
                                      protected val manager: CollectionManager<T>? = null,
                                      name: String = "PTRecyclerViewAdapter",
                                      contents: List<T>? = null,
                                      protected val maxCount: Int = 0,
                                      private val showEmptyCard: Boolean = false,
                                      protected val emptyImage: Int? = com.google.android.gms.base.R.drawable.googleg_disabled_color_18,//R.drawable.abc_ic_star_black_48dp,
                                      protected val emptyText: String? = "Call to Action!",
                                      private val showFirstCard: Boolean = false,
                                      private val showFirstWhenEmpty: Boolean = false,
                                      private val showLastCard: Boolean = false,
                                      private val showLastWhenEmpty: Boolean = false,
                                      protected var onLongClick: ((T, Int) -> Unit)? = null,
                                      private var bindFirst: ((ViewBinding) -> Unit)? = null,
                                      private var bindLast: ((ViewBinding) -> Unit)? = null,
                                      private var bindEmpty: ((ViewBinding) -> Unit)? = null,
                                      private var bind: ((T, Int, ViewBinding) -> Unit)? = null,
                                      protected open val TAG: String = "RecyclerViewAdapter",
                                      private val registerIndividualChanges: Boolean = false): RecyclerView.Adapter<FireBindingViewHolder<T>>() where T: Comparable<T> {
    companion object {
        var TYPE_EMPTY = 1
        var TYPE_NORMAL = 0
    }
    var elems: List<T> = listOf()
        set(value){
            field = value
            notifyDataSetChanged()
        }

    protected open val managerCount
        get() = manager?.elems?.size ?: 0

    private val isEmpty: Boolean
        get() = elems.isEmpty() && managerCount == 0

    protected open val emptyCount
        get() = showFirstWhenEmpty.compareTo(false) + showLastWhenEmpty.compareTo(false) + showEmptyCard.compareTo(false)

    protected open val count: Int
        get() {
            if(isEmpty)
                return emptyCount
            else if(manager == null)
                return showFirstCard.compareTo(false) + elems.size + showLastCard.compareTo(false)
            return showFirstCard.compareTo(false) + managerCount + showLastCard.compareTo(false)
        }

    init {
        Log.i(TAG, "Initializing...")
        if(manager != null && contents != null) {
            throw ConflictingParametersException("You cannot specify both a manager and a set of contents")
        }
        contents?.let{ elems = it }
        manager?.registerAllChangeListener(name) { changeList ->
            val add = changeList.filter { it.event == ObservableEvent.ADD }.size
            val change = changeList.filter { it.event == ObservableEvent.MODIFY }.size
            val remove = changeList.filter { it.event == ObservableEvent.REMOVE }.size
            Log.i(TAG, "Received ${changeList.size} $add, $change, $remove")
            if(!registerIndividualChanges && changeList.size > 3) {
                Log.i(TAG, "Doing a bulk change")
                notifyDataSetChanged()
                return@registerAllChangeListener
            }
            changeList.forEach {
                var index = if(it.after != null ) manager.insertionIndexOf(it.after) else if(it.before != null) manager.insertionIndexOf(it.before) else 0
                val maxC = maxCount + if(showFirstCard) 1 else 0
                if(maxCount > 0 && index >= maxC) {
                    Log.i(TAG, "Returning for maxcount as $maxCount, $index, $maxC")
                    return@forEach
                }
                when(it.event) {
                    ObservableEvent.ADD -> {
                        if(manager.elems.size == 1) { // was empty
                            // Handle the First Card
                            if(showFirstCard && !showFirstWhenEmpty)
                                notifyItemInserted(0) // Add First Card
                            else if(!showFirstCard && showFirstWhenEmpty)
                                notifyItemRemoved(0) // Remove First Empty Card
                            else
                                notifyItemChanged(0) // Replace First Empty Card with First Card

                            // Add the main card or change the empty card into the main card
                            if(showEmptyCard)
                                notifyItemChanged(if(showFirstCard) 1 else 0)
                            else
                                notifyItemInserted(if(showFirstCard) 1 else 0)

                            // Handle the Last Card
                            if(showLastCard && !showLastWhenEmpty)
                                notifyItemInserted(if(showFirstCard) 2 else 1) // Add Last Card
                            else if(!showLastCard && showLastWhenEmpty)
                                notifyItemRemoved(if(showFirstCard) 2 else 1) // Remove Last Empty Card
                            else
                                notifyItemChanged(if(showFirstCard) 2 else 1) // Replace Last Empty Card with Last Card
                        }
                        else {
                            Log.i(TAG, "Individual insert")
                            notifyItemInserted(index)
                        }
                    }

                    ObservableEvent.REMOVE -> {
                        if(managerCount == 0){ // Last Card has been removed
                            // Handle the First Card
                            if(showFirstCard && !showFirstWhenEmpty) // Remove First Card
                                notifyItemRemoved(0)
                            else if(!showFirstCard && showFirstWhenEmpty) // Add First Empty Card
                                notifyItemInserted(0)
                            else
                                notifyItemChanged(0) // change first card to first empty card

                            // Remove Main Card
                            if(showEmptyCard) {
                                Log.i(TAG, "Individual remove -empty change- $index")
                                notifyItemChanged(if (showFirstWhenEmpty) 1 else 0) // change last card to empty card
                            }
                            else {
                                Log.i(TAG, "Individual remove -empty change- $index")
                                notifyItemRemoved(if (showFirstWhenEmpty) 1 else 0)
                            }

                            // Handle the Last Card
                            if(showLastCard && !showLastWhenEmpty)
                                notifyItemRemoved(if(showFirstWhenEmpty) 1 else 0) // Remove Last Card
                            else if(!showLastCard && showLastWhenEmpty)
                                notifyItemInserted(if(showFirstWhenEmpty) 1 else 0) // Add Last Empty Card
                            else
                                notifyItemChanged(0)
                        }
                        else {
                            Log.i(TAG, "Individual remove $index")
                            notifyItemRemoved(index)
                        }
                    }
                    ObservableEvent.MODIFY -> {
                        Log.i(TAG, "Individual modify")
                        notifyItemChanged(index)
                    }
                }
            }

        }
    }

    open fun setContents(contents: List<T>){
        elems = contents
    }

    open fun isFirst(index: Int): Boolean{
        return index == 0
    }

    open fun isLast(index: Int): Boolean{
        if(manager == null)
            return index >= elems.size
        return if (maxCount == 0) managerCount -1 == index else maxCount - 1 == index
    }

    override fun getItemCount(): Int {
        if(isEmpty) {
            Log.i(TAG, "EMPTY $emptyCount")
            return emptyCount
        }
        Log.i(TAG, "Not Empty")
        val add = showFirstCard.compareTo(false) + showLastCard.compareTo(false)
        val c = if(manager == null) elems.size else managerCount
        val result = if(maxCount == 0) c + add else minOf(c, maxCount) + add
        Log.i(TAG, "ITEM COUNT $result")
        return result
    }

    open fun bindEmptyCardInternal(holder: FireBindingViewHolder<T>){
        holder.bind(null, 0, HolderType.EMPTY)
    }

    open fun bindFirstCardInternal(holder: FireBindingViewHolder<T>){
        holder.bind(null, 0, HolderType.FIRST)
    }

    open fun bindLastCardInternal(holder: FireBindingViewHolder<T>){
        holder.bind(null, 0, HolderType.LAST)
    }

    open fun bindInternal(holder: FireBindingViewHolder<T>, elem: T, position: Int){
        val index = if((showFirstWhenEmpty && isEmpty) || (showFirstCard && !isEmpty)) position -1 else position
        Log.i(TAG, "Internal Bind $index")
        holder.bind(elem, index, HolderType.ITEM)
    }

    override fun onBindViewHolder(holder: FireBindingViewHolder<T>, position: Int) {
        Log.i(TAG, "On Bind $position")
        if(position == 0 && ((isEmpty && showFirstWhenEmpty) || (!isEmpty && showFirstCard)))
            bindFirstCardInternal(holder)
        else if(position == 0 && isEmpty && !showFirstWhenEmpty && showEmptyCard)
            bindEmptyCardInternal(holder)
        else if(position == 0 && isEmpty && showLastWhenEmpty)
            bindLastCardInternal(holder)
        else if(position == 0 && showFirstCard)
            bindFirstCardInternal(holder)
        else if(position == 1 && isEmpty && showFirstWhenEmpty && showEmptyCard)
            bindEmptyCardInternal(holder)
        else if(position == itemCount-1 && ((isEmpty && showLastWhenEmpty) || (!isEmpty && showLastCard)))
            bindLastCardInternal(holder)
        else{
            val index = if((showFirstWhenEmpty && isEmpty) || (showFirstCard && !isEmpty)) position -1 else position
            val elem = if(manager == null) elems[index] else manager.elems[index]
            bindInternal(holder, elem, index)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if(!isEmpty) {
            return TYPE_NORMAL
        }
        if((position == 0 && showEmptyCard && !showFirstWhenEmpty) || (position == 1 && showEmptyCard && showFirstWhenEmpty)) {
            return TYPE_EMPTY
        }
        return TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FireBindingViewHolder<T> {
        Log.i(TAG, "On Create VH")
        val emptyFN = createEmptyViewBinding ?: createViewBinding
        val binding = if(viewType == TYPE_EMPTY && createEmptyViewBinding != null) emptyFN(parent) else createViewBinding(parent)
        return FireBindingViewHolder(binding, bind, bindFirst, bindLast, bindEmpty)
    }


    open fun getItemAt(i: Int): T?{
        if(isEmpty)
            return null
        if(showFirstCard && i == 0)
            return null
        val index = i - showFirstCard.compareTo(false)
        if(manager == null)
            return if(index >= elems.size) null else elems[index]
        return if(index >= managerCount) null else manager.elems[index]
    }
}