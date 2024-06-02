package com.example.tasktimer

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext



private const val TAG = "AppProvider"

const val CONTENT_AUTHORITY = "com.example.tasktimer.provider"

private const val TASKS = 100
private const val TASKS_ID = 101

private const val TIMINGS = 200
private const val TIMINGS_ID = 201

private const val CURRENT_TIMING = 300

private const val TASK_DURATIONS = 400

private const val PARAMETERS = 500
private const val PARAMETERS_ID = 501

val CONTENT_AUTHORITY_URI: Uri = Uri.parse("content://$CONTENT_AUTHORITY")

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class AppProvider: ContentProvider() {

    private val uriMatcher by lazy { buildUriMatcher() }

    private fun buildUriMatcher() : UriMatcher {//to match the uri
        Log.d(TAG, "buildUriMatcher: starts")
        val matcher = UriMatcher(UriMatcher.NO_MATCH)

        // e.g. content://com.example.tasktimer.provider/Tasks
        matcher.addURI(CONTENT_AUTHORITY,TasksContract.TABLE_NAME, TASKS)//if task table specified without an ID then the matcher will return 100.

        // e.g. content://com.example.tasktimer.provider/Tasks/8
        //if task table specified with an ID then the matcher will return 101.(similarly for both timings and duration)
        matcher.addURI(CONTENT_AUTHORITY, "${TasksContract.TABLE_NAME}/#", TASKS_ID)//below # is used to match any numerical id and we can use * to match any text which will match numbers as well as text

        matcher.addURI(CONTENT_AUTHORITY, TimingsContract.TABLE_NAME, TIMINGS)
        matcher.addURI(CONTENT_AUTHORITY, "${TimingsContract.TABLE_NAME}/#", TIMINGS_ID)

        matcher.addURI(CONTENT_AUTHORITY, CurrentTimingContract.TABLE_NAME, CURRENT_TIMING)

        matcher.addURI(CONTENT_AUTHORITY, DurationsContract.TABLE_NAME, TASK_DURATIONS)

        matcher.addURI(CONTENT_AUTHORITY,ParametersContract.TABLE_NAME,PARAMETERS)
        matcher.addURI(CONTENT_AUTHORITY,"${ParametersContract.TABLE_NAME}/#", PARAMETERS_ID)

        return matcher//is to return if route URI is matched but above as we are specifying UriMatcher.NO_MATCH because table must be specified in the URl.
    }

    override fun onCreate(): Boolean {
        Log.d(TAG, "onCreate: starts")
        return true
    }

    override fun getType(uri: Uri): String {

        return when (uriMatcher.match(uri)) {
            TASKS -> TasksContract.CONTENT_TYPE

            TASKS_ID -> TasksContract.CONTENT_ITEM_TYPE

            TIMINGS -> TimingsContract.CONTENT_TYPE

            TIMINGS_ID -> TimingsContract.CONTENT_ITEM_TYPE

            CURRENT_TIMING -> CurrentTimingContract.CONTENT_ITEM_TYPE

            TASK_DURATIONS -> DurationsContract.CONTENT_TYPE

            PARAMETERS -> ParametersContract.CONTENT_TYPE

            PARAMETERS_ID -> ParametersContract.CONTENT_ITEM_TYPE


            else -> throw IllegalArgumentException("unknown Uri: $uri")
        }
    }

    override fun query(uri: Uri,
        projection: Array<out String>?,//contains the names of the columns which want to be returned by query
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?//name of the several rows separated by commas will be returned sorted by.(will appear orderBy clause)
    ): Cursor {

        Log.d(TAG, "query: called with uri $uri")
        val match = uriMatcher.match(uri)//by this we will know which uri was passed into query method and we will know which table should be used
        Log.d(TAG, "query: match is $match")

        val context = requireContext(this)
        val queryBuilder = SQLiteQueryBuilder()

        when (match) {
            TASKS -> queryBuilder.tables = TasksContract.TABLE_NAME

            TASKS_ID -> {
                queryBuilder.tables = TasksContract.TABLE_NAME
                val taskId = TasksContract.getId(uri)
                queryBuilder.appendWhere("${TasksContract.Columns.ID} = ")//here the where clause is _id which equals to whatever the actual ID  and the id in the uri. here we can see it is not quoted(the id part)(D/SQLiteQueryBuilder: Performing query: SELECT Name, SortOrder FROM Tasks WHERE (_id = 2) ORDER BY SortOrder)
                queryBuilder.appendWhereEscapeString("$taskId")
                //therefore we used appendWhere to remove single quotes from _ID and used appendWhereEscapeString to keep the task_id in single quoted(Like this: D/SQLiteQueryBuilder: Performing query: SELECT Name, SortOrder FROM Tasks WHERE (_id = '2') ORDER BY SortOrder)
//                queryBuilder.appendWhereEscapeString("${TaskContract.Columns.ID} = $taskId")//here the where clause is _id which equals to whatever the actual ID is. Here we are getting entire in single quoted(D/SQLiteQueryBuilder: Performing query: SELECT Name, SortOrder FROM Tasks WHERE ('_id = 2') ORDER BY SortOrder)
            }

            TIMINGS -> queryBuilder.tables = TimingsContract.TABLE_NAME

            TIMINGS_ID -> {
                queryBuilder.tables = TimingsContract.TABLE_NAME
                val timingId = TimingsContract.getId(uri)
                queryBuilder.appendWhere("${TimingsContract.Columns.ID} = ")
                queryBuilder.appendWhereEscapeString("$timingId")
            }

            CURRENT_TIMING -> {
                queryBuilder.tables = CurrentTimingContract.TABLE_NAME
            }

            TASK_DURATIONS -> queryBuilder.tables = DurationsContract.TABLE_NAME

            PARAMETERS -> queryBuilder.tables = ParametersContract.TABLE_NAME

            PARAMETERS_ID -> {
                queryBuilder.tables = ParametersContract.TABLE_NAME
                val parameterId = ParametersContract.getId(uri)
                queryBuilder.appendWhere("${ParametersContract.Columns.ID} = ")
                queryBuilder.appendWhereEscapeString("$parameterId")
            }


            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

//        val context = context ?: throw NullPointerException("In query function. Context can't be null here!")
        val db = AppDatabase.getInstance(context).readableDatabase
        val cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder)//this query method from queryBuilder is similar to the query method from contentProviders(that is above method) but this queryBuilders query method has "groupBy" and "having" as extra parameters.
        Log.d(TAG, "query: rows in returned cursor = ${cursor.count}") // TODO remove this line

        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {//to insert rows and columns
        Log.d(TAG, "insert: called with uri $uri")
        val match = uriMatcher.match(uri)//by this we will know which uri was passed into query method and we will know which table should be used
        Log.d(TAG, "insert: match is $match")

        val recordID: Long
        val returnUri: Uri

        when(match) {
            TASKS -> {
                val db = AppDatabase.getInstance(context!!).writableDatabase
                recordID = db.insert(TasksContract.TABLE_NAME,null,values)
                if (recordID!= -1L) {//L means long
                    returnUri = TasksContract.buildUriFromId(recordID)
                } else {
                    throw SQLException("Failed to insert, Uri was $uri")
                }
            }

            TIMINGS -> {
                val db = AppDatabase.getInstance(context!!).writableDatabase
                recordID = db.insert(TimingsContract.TABLE_NAME,null,values)
                if (recordID!= -1L) {//L means long
                    returnUri = TimingsContract.buildUriFromId(recordID)
                } else {
                    throw SQLException("Failed to insert, Uri was $uri")
                }
            }

            else -> throw java.lang.IllegalArgumentException("Unknown uri: $uri")
        }
        if (recordID> 0) {
            // something was inserted
            Log.d(TAG, "insert: Setting notifyChange with $uri")
            context?.contentResolver?.notifyChange(uri, null)
        }

        Log.d(TAG, "Exiting insert, returning $returnUri")
        return returnUri
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectiopnArgs: Array<out String>?): Int {
        //we can update one row or many rows at a time and here we will not return cursor and we will use databases update method like insert in insert function.
        Log.d(TAG, "update: called with uri $uri")
        val match = uriMatcher.match(uri)//by this we will know which uri was passed into query method and we will know which table should be used
        Log.d(TAG, "update: match is $match")

        val count: Int
        var selectionCriteria: String

        when(match) {

            TASKS -> {
                val db = AppDatabase.getInstance(context!!).writableDatabase
                count = db.update(TasksContract.TABLE_NAME, values, selection,selectiopnArgs)//this will return the number of rows it updated
            }

            TASKS_ID -> {//if updating for a single row
                val db = AppDatabase.getInstance(context!!).writableDatabase
                val id = TasksContract.getId(uri)
                selectionCriteria = "${TasksContract.Columns.ID} = $id"

                if(selection!=null && selection.isNotEmpty()){
                    selectionCriteria += " AND ($selection)"
                }

                count = db.update(TasksContract.TABLE_NAME,values,selectionCriteria,selectiopnArgs)
            }

            TIMINGS -> {
                val db = AppDatabase.getInstance(context!!).writableDatabase
                count = db.update(TimingsContract.TABLE_NAME, values, selection, selectiopnArgs)//this will return the number of rows it updated
            }
            TIMINGS_ID -> {//if updating for a single row
                val db = AppDatabase.getInstance(context!!).writableDatabase
                val id = TimingsContract.getId(uri)
                selectionCriteria = "${TimingsContract.Columns.ID} = $id"

                if(selection != null && selection.isNotEmpty()) {
                    selectionCriteria += " AND ($selection)"
                }

                count = db.update(TimingsContract.TABLE_NAME, values, selectionCriteria,selectiopnArgs)
            }

            PARAMETERS_ID -> {//if updating for a single row
                val db = AppDatabase.getInstance(context!!).writableDatabase
                val id = ParametersContract.getId(uri)
                selectionCriteria = "${ParametersContract.Columns.ID} = $id"

                if(selection != null && selection.isNotEmpty()) {
                    selectionCriteria += " AND ($selection)"
                }

                count = db.update(ParametersContract.TABLE_NAME, values, selectionCriteria,selectiopnArgs)
            }


            else -> throw IllegalArgumentException("Unknown uri: $uri")
        }

        if (count > 0) {
            // something was updated
            Log.d(TAG, "update: Setting notifyChange with $uri")

            context?.contentResolver?.notifyChange(uri, null)
        }

        Log.d(TAG, "Exiting update, returning $count")
        return count
    }

    override fun delete(uri: Uri, selection: String?, selectiopnArgs: Array<out String>?): Int {
        Log.d(TAG, "delete: called with uri $uri")
        val match = uriMatcher.match(uri)//by this we will know which uri was passed into query method and we will know which table should be used
        Log.d(TAG, "delete: match is $match")

        val count: Int
        var selectionCriteria: String

        when(match) {

            TASKS -> {
                val db = AppDatabase.getInstance(context!!).writableDatabase
                count = db.delete(TasksContract.TABLE_NAME,selection,selectiopnArgs)//this will return the number of rows it updated
            }
            TASKS_ID->{//if updating for a single row
                val db = AppDatabase.getInstance(context!!).writableDatabase
                val id = TasksContract.getId(uri)
                selectionCriteria = "${TasksContract.Columns.ID} = $id"

                if(selection != null && selection.isNotEmpty()) {
                    selectionCriteria += " AND ($selection)"
                }

                count = db.delete(TasksContract.TABLE_NAME,selectionCriteria,selectiopnArgs)
            }

            TIMINGS -> {
                val db = AppDatabase.getInstance(context!!).writableDatabase
                count = db.delete(TimingsContract.TABLE_NAME, selection, selectiopnArgs)//this will return the number of rows it updated
            }

            TIMINGS_ID -> {//if updating for a single row
                val db = AppDatabase.getInstance(context!!).writableDatabase
                val id = TimingsContract.getId(uri)
                selectionCriteria = "${TimingsContract.Columns.ID} = $id"

                if(selection != null && selection.isNotEmpty()) {
                    selectionCriteria += " AND ($selection)"
                }

                count = db.delete(TimingsContract.TABLE_NAME, selectionCriteria,selectiopnArgs)
            }

            else -> throw IllegalArgumentException("Unknown uri: $uri")
        }

        if (count > 0) {
            // something was deleted
            Log.d(TAG, "delete: Setting notifyChange with $uri")

            context?.contentResolver?.notifyChange(uri, null)
        }

        Log.d(TAG, "Exiting delete, returning $count")
        return count
    }
}