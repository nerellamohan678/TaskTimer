package com.example.tasktimer

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.GregorianCalendar

private const val TAG = "DurationsReport"

private const val DIALOG_FILTER = 1
private const val DIALOG_DELETE = 2
private const val DELETION_DATE = "Deletion date"

class DurationsReport : AppCompatActivity(),
    DatePickerDialog.OnDateSetListener,
    AppDialog.DialogEvents,
    View.OnClickListener {

//    private val viewModel by lazy { ViewModelProvider(this)[DurationsViewModel::class.java] }
    private val viewModel: DurationsViewModel by viewModels()

    private val reportAdapter by lazy { DurationsRVAdapter(this, null) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_durations_report)
        setSupportActionBar(findViewById(R.id.toolbar))



        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val td_name_heading = findViewById<TextView>(R.id.td_name_heading)
        val td_description_heading = findViewById<TextView>(R.id.td_description_heading)
        val td_start_heading = findViewById<TextView>(R.id.td_start_heading)
        val td_duration_heading = findViewById<TextView>(R.id.td_duration_heading)
        val td_list = findViewById<RecyclerView>(R.id.td_list)
        td_list.layoutManager = LinearLayoutManager(this)
        td_list.adapter = reportAdapter

        viewModel.cursor.observe(this) { cursor -> reportAdapter.swapCursor(cursor)?.close() }
        //set the listener for the buttons so we sort the report.
        td_name_heading.setOnClickListener(this)
        td_description_heading?.setOnClickListener(this)
        td_start_heading.setOnClickListener(this)
        td_duration_heading.setOnClickListener(this)
    }

    override fun onClick(v: View) {

        when(v.id){
            R.id.td_name_heading-> viewModel.sortOrder = SortColumns.NAME
            R.id.td_description_heading-> viewModel.sortOrder = SortColumns.DESCRIPTION
            R.id.td_start_heading-> viewModel.sortOrder = SortColumns.START_DATE
            R.id.td_duration_heading-> viewModel.sortOrder = SortColumns.DURATION
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_report, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.rm_filter_peroid ->{
                viewModel.toggleDisplayWeek() //was showing a week, so now show a day - or vise versa
                invalidateOptionsMenu()
                return true
            }
            R.id.rm_filter_date ->{
                showDatePickerDialog(getString(R.string.date_title_filer), DIALOG_FILTER)
            }
            R.id.rm_delete ->{
                showDatePickerDialog(getString(R.string.date_title_delete), DIALOG_DELETE)
                return true
            }
            R.id.rm_settings ->{
                val dialog  =SettingsDialog()
                dialog.show(supportFragmentManager,"settings")
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
         val item = menu.findItem(R.id.rm_filter_peroid)
        if(item!=null){
            //switch icon and title to present 7 days or 1 day, as appropriate to the future function of the menu item,
            if(viewModel.displayWeek){
                item.setIcon(R.drawable.ic_filter_1_black_24dp)
                item.setTitle(R.string.rm_title_filter_day)
            }
            else{
                item.setIcon(R.drawable.ic_filter_7_black_24dp)
                item.setTitle(R.string.rm_title_filter_week)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun showDatePickerDialog(title: String, dialogId: Int){
        val dialogFragment = DatePickerFragment()

        val arguments = Bundle()
        arguments.putInt(DATE_PICKER_ID,dialogId)
        arguments.putString(DATE_PICKER_TITLE,title)
        arguments.putSerializable(DATE_PICKER_DATE,viewModel.getFilterDate())

        arguments.putInt(DATE_PICKER_FDOW,viewModel.firstDayOfWeek)

        dialogFragment.arguments = arguments
        dialogFragment.show(supportFragmentManager, "datePicker")
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMoth: Int) {
        Log.d(TAG, "onDateSet called")

        //check teh id, spo we know what to do with result
        when(view.tag as Int){
            DIALOG_FILTER->{
                viewModel.setReportDate(year,month,dayOfMoth)
            }
            DIALOG_DELETE->{
                // we need to format the date for user's locale
                val cal = GregorianCalendar()
                cal.set(year, month, dayOfMoth,0,0,0)
                val fromDate = android.text.format.DateFormat.getDateFormat(this).format(cal.time)

                val dialog = AppDialog()
                val args = Bundle()
                args.putInt(DIALOG_ID, DIALOG_DELETE)//use the same id value
                args.putString(DIALOG_MESSAGE, getString(R.string.delete_timings_message,fromDate))

                args.putLong(DELETION_DATE,cal.timeInMillis)
                dialog.arguments = args
                dialog.show(supportFragmentManager,null)
            }
            else-> throw IllegalArgumentException("Invalid mode when receiving DatePickerDialog result")
        }
    }

    override fun onPositiveDialogResult(dialogId: Int, args: Bundle) {
        Log.d(TAG,"onPositiveDialogRequest: called with id $dialogId")
        if(dialogId == DIALOG_DELETE){
            //retrieve the date from the bundle
            val deleteDate = args.getLong(DELETION_DATE)
            viewModel.deleteRecords(deleteDate)
        }
    }
}