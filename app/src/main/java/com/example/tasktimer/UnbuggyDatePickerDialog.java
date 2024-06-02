package com.example.tasktimer;

import android.app.DatePickerDialog;
import android.content.Context;

public class UnbuggyDatePickerDialog extends DatePickerDialog{
    UnbuggyDatePickerDialog(Context context, OnDateSetListener callback, int year, int mothOfYear, int dayOfMonth){
        super(context,callback,year,mothOfYear,dayOfMonth);
    }

    @Override
    protected void onStop(){
        //do nothing - do not call super method
    }

}
