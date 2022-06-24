package com.example.mylibrary.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibrary.CollectionManager
import com.example.mylibrary.ConflictingParametersException
import com.example.mylibrary.ObservableEvent
import com.example.mylibrary.R


open class FireViewHolder<T>(inflater: LayoutInflater, private val parent: ViewGroup, resource: Int,
                             private val providedBind: ((T, View) -> Unit)? = null, private val providedFirstBind: ((View) -> Unit)? = null,
                             private val providedLastBind: ((View) -> Unit)? = null) : RecyclerView.ViewHolder(inflater.inflate(resource, parent, false)){
    protected open val TAG = "PTItemHolder"

    open fun bindEmptyCard(image: Int?, text: String?, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?){
        itemView.setOnClickListener { onClick?.invoke() }
        itemView.setOnLongClickListener { onLongClick?.invoke(); true }
      //  image?.let { itemView.imageView?.setImageResource(it) }
       // itemView.placeholderText?.text = text
        Log.i(TAG, "IMAGE $image, text $text")
    }

    open fun bind(elem: T, onClick: ((T) -> Unit)?, onLongClick: ((T) -> Unit)?, isFirst: Boolean, isLast: Boolean){
        providedBind?.invoke(elem, itemView)
        itemView.setOnClickListener { onClick?.invoke(elem) }
        itemView.setOnLongClickListener { onLongClick?.invoke(elem); true }
    }

    open fun bindFirstCard(onClick: (() -> Unit)?, onLongClick: (() -> Unit)?){
        providedFirstBind?.invoke(itemView)
        itemView.setOnClickListener { onClick?.invoke() }
        itemView.setOnLongClickListener { onLongClick?.invoke(); true }
    }

    open fun bindLastCard(onClick: (() -> Unit)?, onLongClick: (() -> Unit)?){
        providedLastBind?.invoke(itemView)
        itemView.setOnClickListener { onClick?.invoke() }
        itemView.setOnLongClickListener { onLongClick?.invoke(); true }
    }
}

open class FireRecyclerViewAdapter<T>(protected val manager: CollectionManager<T>? = null,
                                      name: String = "PTRecyclerViewAdapter",
                                      contents: List<T>? = null,
                                      protected val maxCount: Int = 0,
                                      private val showEmptyCard: Boolean = false,
                                      private val emptyLayout: Int = R.layout.empty_card,
                                      private val layout: Int = R.layout.empty_card,
                                      protected val emptyOnClick: (() -> Unit)? = null,
                                      protected val emptyOnLongClick: (() -> Unit)? = null,
                                      protected val emptyImage: Int? = com.google.android.gms.base.R.drawable.googleg_disabled_color_18,//R.drawable.abc_ic_star_black_48dp,
                                      protected val emptyText: String? = "Call to Action!",
                                      private val showFirstCard: Boolean = false,
                                      private val showFirstWhenEmpty: Boolean = false,
                                      protected val firstOnClick: (() -> Unit)? = null,
                                      protected val firstOnLongClick: (() -> Unit)? = null,
                                      private val showLastCard: Boolean = false,
                                      private val showLastWhenEmpty: Boolean = false,
                                      protected val lastOnClick: (() -> Unit)? = null,
                                      protected val lastOnLongClick: (() -> Unit)? = null,
                                      protected var onClick: ((T, Int) -> Unit)? = null,
                                      protected var onLongClick: ((T, Int) -> Unit)? = null,
                                      private var bindFirst: ((View) -> Unit)? = null,
                                      private var bindLast: ((View) -> Unit)? = null,
                                      private var bind: ((T, View) -> Unit)? = null,
                                      protected open val TAG: String = "RecyclerViewAdapter"): RecyclerView.Adapter<FireViewHolder<T>>() where T: Comparable<T> {
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

    val isEmpty: Boolean
        get() = elems.size == 0 && managerCount == 0

    protected open val emptyCount
    get() = showFirstWhenEmpty.compareTo(false) + showLastWhenEmpty.compareTo(false) + showEmptyCard.compareTo(false)

    protected open val count: Int
    get(){
        if(isEmpty)
            return emptyCount
        else if(manager == null)
            return showFirstCard.compareTo(false) + elems.size + showLastCard.compareTo(false)
        return showFirstCard.compareTo(false) + managerCount + showLastCard.compareTo(false)
    }

    init{
        Log.i(TAG, "Initializing...")
        if(manager != null && contents != null){
            throw ConflictingParametersException("You cannot specify both a manager and a set of contents")
        }
        contents?.let{ elems = it }
        manager?.registerListener(name){ eventType, before, after ->
            var index = 0
            if(before != null)
                index = manager.insertionIndexOf(before)
            else if(after != null)
                index = manager.insertionIndexOf(after)

            var maxC = if(showFirstCard) maxCount + 1 else maxCount //NOTE: Don't need to take into account showFirstWhenEmpty as we'll have managerCount 0
            if(maxCount == 0 || index < maxC){
                when(eventType) {
                    ObservableEvent.ADD -> {
                        val wasEmpty = manager.elems.size == 1
                        if(wasEmpty){
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
                            notifyItemInserted(index)

                            // NOTE: This is only here to handle the section headers used in the Pin Traveler App
                            if (index + 1 < managerCount)
                                notifyItemChanged(index+1)
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
                                notifyItemChanged(0)
                            // Remove Main Card
                            notifyItemRemoved(if(showFirstWhenEmpty) 1 else 0)

                            // Handle the Last Card
                            if(showLastCard && !showLastWhenEmpty)
                                notifyItemRemoved(if(showFirstWhenEmpty) 1 else 0) // Remove Last Card
                            else if(!showLastCard && showLastWhenEmpty)
                                notifyItemInserted(if(showFirstWhenEmpty) 1 else 0) // Add Last Empty Card
                            else
                                notifyItemChanged(0)

                        }
                        else {
                            notifyItemRemoved(index)

                            //NOTE: This is only here to handle the section headers used in the Pin Traveler App
                            if (index < managerCount)
                                notifyItemChanged(index)
                        }
                    }
                    ObservableEvent.MODIFY -> notifyItemChanged(index) // Modify cannot affect first/last card so no need to worry
                }
            }
        }
    }

    open fun setContents(contents: List<T>){
        elems = contents
        notifyDataSetChanged()
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
        Log.i(TAG, "ITEM COUNT")
        if(isEmpty) {
            Log.i(TAG, "EMPTY $emptyCount")
            return emptyCount
        }
        Log.i(TAG, "Not Empty")
        val add = showFirstCard.compareTo(false) + showLastCard.compareTo(false)
        val c = if(manager == null) elems.size else managerCount
        if(maxCount == 0)
            return c + add
        return minOf(c, maxCount) + add
    }

    open fun bindEmptyCardInternal(holder: FireViewHolder<T>){
        holder.bindEmptyCard(emptyImage, emptyText, emptyOnClick, emptyOnLongClick)
    }

    open fun bindFirstCardInternal(holder: FireViewHolder<T>){
        holder.bindFirstCard(firstOnClick, firstOnLongClick)
    }

    open fun bindLastCardInternal(holder: FireViewHolder<T>){
        holder.bindLastCard(lastOnClick, lastOnLongClick)
    }

    open fun bindInternal(holder: FireViewHolder<T>, elem: T, position: Int){
        val index = if((showFirstWhenEmpty && isEmpty) || (showFirstCard && !isEmpty)) position -1 else position
        holder.bind(elem, {t -> onClick?.invoke(t, position)}, {t -> onLongClick?.invoke(t, position)}, isFirst(index), isLast(index))
    }

    override fun onBindViewHolder(holder: FireViewHolder<T>, position: Int) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FireViewHolder<T> {
        val inflater = LayoutInflater.from(parent.context)
        if(viewType == TYPE_EMPTY) {
            Log.i(TAG, "EMPTY VH")
            return FireViewHolder(inflater, parent, emptyLayout)
        }
        Log.i(TAG, "REGULAR VH")
        return FireViewHolder(inflater, parent, layout, bind)
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