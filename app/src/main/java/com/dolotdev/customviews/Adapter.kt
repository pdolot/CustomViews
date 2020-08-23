package com.dolotdev.customviews

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.dolotdev.customviewslib.swipeLayout.SwipeLayout
import kotlinx.android.synthetic.main.item_swipe.view.*

class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

    private var items: List<Any>? = null
    private var selected: Int = -1

    fun setData(items: List<Any>){
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_swipe, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.apply {
            swipeLayout.fullExpandLeft()
            swipeLayout.setOnSwipeListener(
                onSwipe = { progress, direction ->
                    Log.v("MainActivitySwipe", "Swipe $progress ${direction.name}")
                },
                onFullSwiped = {
                    Log.v("MainActivitySwipe", "Full swiped ${it.name}")
                },
                onSideViewSwiped = {
                    Log.v("MainActivitySwipe", "Side swiped ${it.name}")
                },
                onCollapse = {
                    Log.v("MainActivitySwipe", "Collapse")
                }
            )

            reset.setOnClickListener {
                swipeLayout.reset(true)
            }

            swipeLayout.setOnClickListener {
                selected = -1
                notifyItemChanged(position)
            }

            swipeLayout.setOnLongClickListener {
                selected = position
                notifyItemChanged(position)
            }

            if (position == selected){
                surfaceView.bgColor = Color.MAGENTA
            }else{
                surfaceView.bgColor = Color.WHITE
            }
        }
    }

    inner class ViewHolder internal constructor(view: View) :
        RecyclerView.ViewHolder(view)
}