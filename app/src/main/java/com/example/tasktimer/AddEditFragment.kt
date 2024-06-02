package com.example.tasktimer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import kotlinx.android.*
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels

private const val TAG = "AddEditFragment"
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_TASK = "task"

/**
 * A simple [Fragment] subclass.
 * Use the [AddEditFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@Suppress("DEPRECATION")
class AddEditFragment : Fragment() {
    private var task: Task? = null
    private var listener : OnSaveClicked? = null
//    private val viewModel by lazy { ViewModelProvider(activity!!).get(TaskTimerViewModel::class.java) }
    private val viewModel: TaskTimerViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG,"onCreate: starts")
        super.onCreate(savedInstanceState)
        task = arguments?.getParcelable(ARG_TASK)

        setHasOptionsMenu(true)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d(TAG,"onCreateView: starts")
        return inflater.inflate(R.layout.fragment_add_edit, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated: called")
        val task = task
        val addedit_name = view.findViewById<EditText>(R.id.addedit_name)
        val addedit_description = view.findViewById<EditText>(R.id.addedit_description)
        val addedit_sortorder = view.findViewById<EditText>(R.id.addedit_sortorder)
        if(savedInstanceState == null) {
            if (task != null) {
                Log.d(TAG, "onViewCreated: Task details found, editing task ${task.id}")
                addedit_name?.setText(task.name)//if this does not work use setText instead of text
                addedit_description?.setText(task.description)
                addedit_sortorder?.setText(task.sortOrder.toString())

                viewModel.startEditing(task.id)
            } else {
                //no task, so we must be adding a new task, and not editing an existing pone
                Log.d(TAG, "onViewCreated: No arguments, adding new record")

            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        Log.d(TAG,"onPrepareOptionsMenu: called")
        menu.clear()
    }

    fun isDirty(): Boolean {
        val newTask = taskFromUi()
        return ((newTask != task) &&
                (newTask.name.isNotBlank()
                        || newTask.description.isNotBlank()
                        || newTask.sortOrder != 0)
                )
    }

    private  fun saveTask(){
        //create a newTask object with the details to be saved, then
        //call the viewModel's saveTask fuction to save it.
        //Task is now a data class, so we can compare the new details withj the original task,
        //and only save if they are different

        val newTask = taskFromUi()
        if(newTask != task){
            Log.d(TAG,"saveTask: saving task, id is${newTask.id}")
            task = viewModel.saveTask(newTask)
            Log.d(TAG,"saveTask: saving task, id is${task?.id}")
        }
    }
    private fun taskFromUi(): Task{
        val addedit_sortorder = view?.findViewById<EditText>(R.id.addedit_sortorder)
        val addedit_name = view?.findViewById<EditText>(R.id.addedit_name)
        val addedit_description = view?.findViewById<EditText>(R.id.addedit_description)

        val sortOrder = if(addedit_sortorder!!.text.isNotEmpty()){
            Integer.parseInt(addedit_sortorder.text.toString())
        }else{
            0
        }

        val newTask = Task(addedit_name!!.text.toString(),addedit_description!!.text.toString(),sortOrder)
        newTask.id = task?.id?:0

        return newTask
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.d(TAG,"onActivityCreated: starts")
        super.onActivityCreated(savedInstanceState)

        if(activity is AppCompatActivity) {
            val actionBar = (activity as AppCompatActivity?)?.supportActionBar
            actionBar?.setDisplayHomeAsUpEnabled(true)
        }

        val addedit_save = view?.findViewById<Button>(R.id.addedit_save)
        addedit_save?.setOnClickListener {
            saveTask()//to save out data when it is changed and also when the data is entered for first time
            listener?.onSaveClicked()
        }

    }

    override fun onAttach(context: Context) {
        Log.d(TAG,"onAttach: starts")
        super.onAttach(context)
        if(context is OnSaveClicked){
            listener = context
        }else{
            throw RuntimeException("$context must implement OnSaveClicked")
        }
    }

    override fun onDetach() {
        Log.d(TAG,"onDetach: starts")
        super.onDetach()
        listener = null

        viewModel.stopEditing()
    }


    interface OnSaveClicked{
        fun onSaveClicked()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param task The task to be edited, or null to add new task.
         * @return A new instance of fragment AddEditFragment.
         */
        @JvmStatic
        fun newInstance(task: Task?) =
            AddEditFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_TASK, task)
                }
            }
    }
    // Fragment Lifecycle callback events - added for logging only
// Paste into the end of MainActivityFragment and AddEditFragment, then
// delete duplicates from AddEditFragment

    //TODO: Delete all these functions before releasing the app

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewStateRestored: called")
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onStart() {
        Log.d(TAG, "onStart: called")
        super.onStart()
    }

    override fun onResume() {
        Log.d(TAG, "onResume: called")
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause: called")
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState: called")
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        Log.d(TAG, "onStop: called")
        super.onStop()
    }
    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView: called")
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: called")
        super.onDestroy()
    }
}