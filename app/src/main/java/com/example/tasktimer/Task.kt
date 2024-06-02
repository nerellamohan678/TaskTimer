package com.example.tasktimer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Task(val name: String, val description: String, val sortOrder: Int, var id: Long = 0) : Parcelable
