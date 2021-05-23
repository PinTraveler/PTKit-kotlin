package com.pintraveler.ptkit.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pintraveler.ptkit.CollectionManager
import com.pintraveler.ptkit.ConflictingParametersException
import com.pintraveler.ptkit.ObservableEvent
import com.pintraveler.ptkit.R

open class FireViewHolder<T>(inflater: LayoutInflater, private val parent: ViewGroup, resource: Int,
                             private val providedBind: ((T, View) -> Unit)? = null, private val providedFirstBind: ((View) -> Unit)? = null,
                             private val providedLastBind: ((View) -> Unit)? = null) : RecyclerView.ViewHolder(inflater.inflate(resource, parent, false)){
    protected open val TAG = "PTItemHolder"

    open fun bindEmptyCard(image: Int, text: String, layoutParams: LinearLayout.LayoutParams?, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, extraVars: Map<String,Any> = mapOf()){
        if(layoutParams != null)
            itemView.layoutParams = layoutParams
        itemView.setOnClickListener { onClick?.invoke() }
        itemView.setOnLongClickListener { onLongClick?.invoke(); true }
        itemView.findViewById<ImageView>(R.id.imageView)?.setImageResource(image)
        itemView.findViewById<TextView>(R.id.placeholderText)?.text = text
    }

    open fun bind(elem: T, layoutParams: LinearLayout.LayoutParams?, onClick: ((T) -> Unit)?, onLongClick: ((T) -> Unit)?, isFirst: Boolean, isLast: Boolean, extraVars: Map<String,Any> = mapOf()){
        if(layoutParams != null)
            itemView.layoutParams = layoutParams
        providedBind?.invoke(elem, itemView)
        itemView.setOnClickListener { onClick?.invoke(elem) }
        itemView.setOnLongClickListener { onLongClick?.invoke(elem); true }
    }

    open fun bindFirstCard(layoutParams: LinearLayout.LayoutParams?, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, extraVars: Map<String,Any> = mapOf()){
        if(layoutParams != null)
            itemView.layoutParams = layoutParams
        providedFirstBind?.invoke(itemView)
        itemView.setOnClickListener { onClick?.invoke() }
        itemView.setOnLongClickListener { onLongClick?.invoke(); true }
    }

    open fun bindLastCard(layoutParams: LinearLayout.LayoutParams?, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?, extraVars: Map<String,Any> = mapOf()){
        if(layoutParams != null)
            itemView.layoutParams = layoutParams
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
                                      private val emptyOnClick: (() -> Unit)? = null,
                                      private val emptyOnLongClick: (() -> Unit)? = null,
                                      private val emptyImage: Int? = R.drawable.abc_ic_star_black_48dp,
                                      private val emptyText: String? = "Call to Action!",
                                      private val showFirstCard: Boolean = false,
                                      private val showFirstWhenEmpty: Boolean = true,
                                      private val firstOnClick: (() -> Unit)? = null,
                                      private val firstOnLongClick: (() -> Unit)? = null,
                                      private val showLastCard: Boolean = false,
                                      private val showLastWhenEmpty: Boolean = true,
                                      private val lastOnClick: (() -> Unit)? = null,
                                      private val lastOnLongClick: (() -> Unit)? = null,
                                      protected var onClick: ((T, Int) -> Unit)? = null,
                                      protected var onLongClick: ((T, Int) -> Unit)? = null,
                                      private val layoutParams: LinearLayout.LayoutParams? = null,
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
    get() = showFirstWhenEmpty.compareTo(false) + showLastWhenEmpty.compareTo(false)

    protected open val count: Int
    get(){
        if(manager == null)
            return elems.size
        return if(managerCount == 0) emptyCount else showFirstCard.compareTo(false) + managerCount + showLastCard.compareTo(false)
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
        if(isEmpty) {
            return emptyCount
        }
        val add = showFirstCard.compareTo(false) + showLastCard.compareTo(false)
        val c = if(manager == null) elems.size else managerCount
        if(maxCount == 0)
            return c + add
        return minOf(c, maxCount) + add
    }

    override fun onBindViewHolder(holder: FireViewHolder<T>, position: Int) {
        if(position == 0 && ((isEmpty && showFirstWhenEmpty) || (!isEmpty && showFirstCard)))
            holder.bindFirstCard(layoutParams, firstOnClick, lastOnLongClick)
        else if(position == 0 && isEmpty && !showFirstWhenEmpty && showEmptyCard)
            holder.bindEmptyCard(emptyImage!!, emptyText!! ,layoutParams, emptyOnClick, emptyOnLongClick)
        else if(position == 0 && isEmpty && showLastWhenEmpty)
            holder.bindLastCard(layoutParams, lastOnClick, lastOnLongClick)
        else if(position == 0 && showFirstCard)
            holder.bindFirstCard(layoutParams, firstOnClick, firstOnLongClick)
        else if(position == 1 && isEmpty && showFirstWhenEmpty && showEmptyCard)
            holder.bindEmptyCard(emptyImage!!, emptyText!!, layoutParams, emptyOnClick, emptyOnLongClick)
        else if(position == itemCount-1 && ((isEmpty && showLastWhenEmpty) || (!isEmpty && showLastCard)))
            holder.bindLastCard(layoutParams, lastOnClick, lastOnLongClick)
        else{
            val index = if((showFirstWhenEmpty && isEmpty) || (showFirstCard && !isEmpty)) position -1 else position
            val elem = if(manager == null) elems[index] else manager.elems[index]
            holder.bind(elem, layoutParams, {t -> onClick?.invoke(t, position)}, {t -> onLongClick?.invoke(t, position)}, isFirst(index), isLast(index))
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
        if(viewType == TYPE_EMPTY)
            return FireViewHolder(inflater, parent, emptyLayout)
        return FireViewHolder(inflater, parent, layout, bind)
    }


    open fun getItemAt(i: Int): T?{
        if(isEmpty)
            return null
        val add = (if(showFirstCard) 1 else 0)
        val index = i - showFirstCard.compareTo(false)
        if(showFirstCard && i == 0)
            return null
        else if(manager == null)
            return if(index >= elems.size) null else elems[index]
        return if(index >= managerCount) null else manager.elems[index]
    }

}