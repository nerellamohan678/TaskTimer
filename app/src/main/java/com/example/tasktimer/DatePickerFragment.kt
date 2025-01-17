package com.example.tasktimer

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatDialogFragment
import java.util.Date
import java.util.GregorianCalendar

const val DATE_PICKER_ID = "ID"
const val DATE_PICKER_TITLE = "TITLE"
const val DATE_PICKER_DATE = "DATE"
const val DATE_PICKER_FDOW = "FIRST DAY OF WEEK"

private const val TAG = "DatePickerFragment"

class DatePickerFragment : AppCompatDialogFragment(), DatePickerDialog.OnDateSetListener
{
    private var dialogId = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        //use the current data initially
        val cal  = GregorianCalendar()
        var title: String? = null

        val arguments = arguments// avoid problems with smart cast
        if(arguments!=null) {
            dialogId = arguments.getInt(DATE_PICKER_ID)
            title = arguments.getString(DATE_PICKER_TITLE)

            //if we were passed a date, use it; otherwise leave cal set to the current date.
            val givenData = arguments.getSerializable(DATE_PICKER_DATE) as Date?
            if (givenData != null) {
                cal.time = givenData
                Log.d(TAG, "in onCreateDialog, retrieved data $givenData")
            }

        }
        val year = cal.get(GregorianCalendar.YEAR)
        val month = cal.get(GregorianCalendar.MONTH)
        val day = cal.get(GregorianCalendar.DAY_OF_MONTH)

//        val dpd = UnbuggyDatePickerDialog(context!!,this,year,month,day)
        val dpd = DatePickerDialog(context!!,this,year,month,day)
        if(title!=null){
            dpd.setTitle(title)
        }

        //set the date picker's first day of the week, on API 21 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val firstDayOfTheWeek =
                arguments?.getInt(DATE_PICKER_FDOW, cal.firstDayOfWeek) ?: cal.firstDayOfWeek
            dpd.datePicker.firstDayOfWeek = firstDayOfTheWeek
        }
        return dpd
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        //Activities using this dialog must implement its callbacks
        if(context !is DatePickerDialog.OnDateSetListener){
            throw ClassCastException("$context must implement DatePickerDialog.OnDateSetListener interface")
        }
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMoth: Int) {
        Log.d(TAG,"Entering onDataSet")

        //Notify caller of the user-selected values
        view.tag = dialogId //pass the id back in the tag, to save the caller storing their own copy.

        (context as DatePickerDialog.OnDateSetListener?)?.onDateSet(view, year , month , dayOfMoth)
        Log.d(TAG,"Exiting onDataSet")
    }
}