package com.example.tasktimer

import android.annotation.SuppressLint
import android.database.Cursor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer

class TaskViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView),
        LayoutContainer {//we are using override to use layoutContainer

        lateinit var task: Task

        fun bind(task: Task, listener: CursorRecyclerViewAdapter.OnTaskClickListener) {

            this.task = task

            containerView.findViewById<TextView>(R.id.tli_name).text = task.name
            containerView.findViewById<TextView>(R.id.tli_description).text = task.description
            containerView.findViewById<ImageView>(R.id.tli_edit).visibility = View.VISIBLE

            containerView.findViewById<ImageView>(R.id.tli_edit).setOnClickListener {
                listener.onEditClick(task)
            }

            containerView.setOnLongClickListener {
                listener.onTaskLongClick(task)
                true
            }
        }
}

private const val TAG = "CursorRecyclerViewAdapt"

class CursorRecyclerViewAdapter(private var cursor: Cursor?, private val listener: OnTaskClickListener) :
        RecyclerView.Adapter<TaskViewHolder>() {//calls the recycler view when it need a new view to display

    interface OnTaskClickListener {
        fun onEditClick(task: Task)
        fun onTaskLongClick(task: Task)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        Log.d(TAG, "onCreateViewHolder: new view requested")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_list_items, parent, false)
        return TaskViewHolder(view)
    }

    @SuppressLint("Range")
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {//this is called when a new data is to be displayed and re-use the existing view

        val cursor = cursor     // avoid problems with smart cast

        if(cursor == null || cursor.count == 0) {
            Log.d(TAG, "onBindViewHolder: providing instructions")
            holder.containerView.findViewById<TextView>(R.id.tli_name).setText(R.string.instruction_heading)
            holder.containerView.findViewById<TextView>(R.id.tli_description).setText(R.string.instructions)
            holder.containerView.findViewById<ImageView>(R.id.tli_edit).visibility = View.GONE
        } else {
            if (!cursor.moveToPosition(position)) {
                throw java.lang.IllegalStateException("Could not move cursor to position $position")
            }

            // Create a Task object from the data in the cursor
            with(cursor) {
                val task = Task(
                    getString(getColumnIndex(TasksContract.Columns.TASK_NAME)),
                    getString(getColumnIndex(TasksContract.Columns.TASK_DESCRIPTION)),
                    getInt(getColumnIndex(TasksContract.Columns.TASK_SORT_ORDER))
                )
                //remember that the id isn't set in the constructor
                task.id = getLong(getColumnIndex(TasksContract.Columns.ID))

                holder.bind(task, listener)
            }
        }
    }

    override fun getItemCount(): Int {
        val cursor = cursor
        return if (cursor == null || cursor.count == 0) {
            1   // fib, because we populate a single ViewHolder with instructions
        } else {
            cursor.count
        }
    }

    /**
     * Swap in a new Cursor, returning the old Cursor.
     * The returned old Cursor is *not* closed.
     *
     * @param newCursor The new cursor to be used
     * @return Returns the previously set Cursor, or null if there wasn't one.
     * If the given new Cursor is the same instance as the previously set
     * Cursor, null is also returned.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun swapCursor(newCursor: Cursor?): Cursor? {
        if (newCursor === cursor) {
            return null
        }

        val numItems = itemCount

        val oldCursor = cursor
        cursor = newCursor
        if (newCursor != null) {
            // notify the observers about the new cursor
            notifyDataSetChanged()
        } else {
            // notify the observers about the lack of a data set
            notifyItemRangeRemoved(0, numItems)
        }
        return oldCursor
    }
}
