package com.example.tasktimer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log


private const val TAG = "AppDatabase"

private const val DATABASE_NAME = "TaskTimer.db"
private const val DATABASE_VERSION = 5

internal class AppDatabase private constructor(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    init {
        Log.d(TAG, "AppDatabase: initialising")
    }

    override fun onCreate(db: SQLiteDatabase) {//this will create a database if does not exist
        //CREATE TABLE Tasks (_id INTEGER KEY NOT NULL,NAME TEXT NOTNULL,DESCRIPTION TEXT, SortOrder INTEGER);
        Log.d(TAG, "onCreate: starts")
        val sSQL = """CREATE TABLE ${TasksContract.TABLE_NAME} (
            ${TasksContract.Columns.ID} INTEGER PRIMARY KEY NOT NULL,
            ${TasksContract.Columns.TASK_NAME} TEXT NOT NULL,
            ${TasksContract.Columns.TASK_DESCRIPTION} TEXT,
            ${TasksContract.Columns.TASK_SORT_ORDER} INTEGER);""".replaceIndent(" ")//to format the spaces of the start of each line
        Log.d(TAG, sSQL)
        db.execSQL(sSQL)

        addTimingsTable(db)
        addCurrentTimingView(db)
        addDurationsView(db)
        parameteriseView(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {//it is called when the database needs to be upgraded
        Log.d(TAG, "onUpgrade: starts")
        when(oldVersion) {
            1 -> {
            addTimingsTable(db)
                addCurrentTimingView(db)
                addDurationsView(db)
                parameteriseView(db)
            }
            2 -> {
                addCurrentTimingView(db)
                addDurationsView(db)
                parameteriseView(db)
            }
            3 -> {
                addDurationsView(db)
                parameteriseView(db)
            }
            4-> parameteriseView(db)
            else -> throw IllegalStateException("onUpgrade() with unknown newVersion: $newVersion")
        }
    }

    private fun addTimingsTable(db: SQLiteDatabase) {

        val sSQLTiming = """CREATE TABLE ${TimingsContract.TABLE_NAME} (
            ${TimingsContract.Columns.ID} INTEGER PRIMARY KEY NOT NULL,
            ${TimingsContract.Columns.TIMING_TASK_ID} INTEGER NOT NULL,
            ${TimingsContract.Columns.TIMING_START_TIME} INTEGER,
            ${TimingsContract.Columns.TIMING_DURATION} INTEGER);""".replaceIndent(" ")
        Log.d(TAG, sSQLTiming)
        db.execSQL(sSQLTiming)

        val sSQLTrigger = """CREATE TRIGGER Remove_Task
            AFTER DELETE ON ${TasksContract.TABLE_NAME}
            FOR EACH ROW
            BEGIN
            DELETE FROM ${TimingsContract.TABLE_NAME}
            WHERE ${TimingsContract.Columns.TIMING_TASK_ID} = OLD.${TasksContract.Columns.ID};
            END;""".replaceIndent(" ")
        Log.d(TAG, sSQLTrigger)
        db.execSQL(sSQLTrigger)
    }

    private fun addCurrentTimingView(db: SQLiteDatabase) {
        /*
        CREATE VIEW vwCurrentTiming
             AS SELECT Timings._id,
                 Timings.TaskId,
                 Timings.StartTime,
                 Tasks.Name
             FROM Timings
             JOIN Tasks
             ON Timings.TaskId = Tasks._id
             WHERE Timings.Duration = 0
             ORDER BY Timings.StartTime DESC;
         */
        val sSQLTimingView = """CREATE VIEW ${CurrentTimingContract.TABLE_NAME}
        AS SELECT ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.ID},
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_TASK_ID},
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME},
            ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_NAME}
        FROM ${TimingsContract.TABLE_NAME}
        JOIN ${TasksContract.TABLE_NAME}
        ON ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_TASK_ID} = ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID}
        WHERE ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_DURATION} = 0
        ORDER BY ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME} DESC;
    """.replaceIndent(" ")
        Log.d(TAG, sSQLTimingView)
        db.execSQL(sSQLTimingView)
    }

    private fun addDurationsView(db: SQLiteDatabase) {
        /*
      CREATE VIEW vwTaskDurations AS
      SELECT Tasks.Name,
      Tasks.Description,
      Timings.StartTime,
      DATE(Timings.StartTime, 'unixepoch', 'localtime') AS StartDate,
      SUM(Timings.Duration) AS Duration
      FROM Tasks INNER JOIN Timings
      ON Tasks._id = Timings.TaskId
      GROUP BY Tasks._id, StartDate;
      */
        val sSQL = """CREATE VIEW ${DurationsContract.TABLE_NAME}
                AS SELECT ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_NAME},
                ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_DESCRIPTION},
                ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME},
                DATE(${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME}, 'unixepoch', 'localtime')
                AS ${DurationsContract.Columns.START_DATE},
                SUM(${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_DURATION})
                AS ${DurationsContract.Columns.DURATION}
                FROM ${TasksContract.TABLE_NAME} INNER JOIN ${TimingsContract.TABLE_NAME}
                ON ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID} =
                ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_TASK_ID}
                GROUP BY ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID}, ${DurationsContract.Columns.START_DATE}
                ;""".replaceIndent(" ")
        Log.d(TAG, sSQL)
        db.execSQL(sSQL)
    }

    private fun parameteriseView(db: SQLiteDatabase) {
        var sSQL = """CREATE TABLE ${ParametersContract.TABLE_NAME}
            (${ParametersContract.Columns.ID} INTEGER PRIMARY KEY NOT NULL,
            ${ParametersContract.Columns.VALUE} INTEGER NOT NULL);""".trimMargin()
        Log.d(TAG, sSQL)
        db.execSQL(sSQL)

        sSQL = "DROP VIEW IF EXISTS ${DurationsContract.TABLE_NAME};"
        Log.d(TAG, sSQL)
        db.execSQL(sSQL)

        /**
        CREATE VIEW vwTaskDurations AS
        SELECT Tasks.Name,
        Tasks.Description,
        Timings.StartTime,
        DATE(Timings.StartTime, 'unixepoch') AS StartDate,
        SUM(Timings.Duration) AS Duration
        FROM Tasks INNER JOIN Timings
        ON Tasks._id = Timings.TaskId
        WHERE Timings.Duration > (SELECT Parameters.value FROM Parameters WHERE Parameters._id = 1)
        GROUP BY Tasks._id, StartDate;
         **/

        sSQL = """CREATE VIEW ${DurationsContract.TABLE_NAME}
            AS SELECT ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_NAME},
            ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_DESCRIPTION},
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME},
            DATE(${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME}, 'unixepoch', 'localtime')
            AS ${DurationsContract.Columns.START_DATE},
            SUM(${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_DURATION})
            AS ${DurationsContract.Columns.DURATION}
            FROM ${TasksContract.TABLE_NAME} INNER JOIN ${TimingsContract.TABLE_NAME}
            ON ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID} =
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_TASK_ID}
            WHERE ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_DURATION} >
                (SELECT ${ParametersContract.TABLE_NAME}.${ParametersContract.Columns.VALUE}
                FROM ${ParametersContract.TABLE_NAME}
                WHERE ${ParametersContract.TABLE_NAME}.${ParametersContract.Columns.ID} = ${ParametersContract.ID_SHORT_TIMING})
            GROUP BY ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID}, ${DurationsContract.Columns.START_DATE}
            ;""".replaceIndent(" ")
        Log.d(TAG, sSQL)
        db.execSQL(sSQL)

        sSQL = """INSERT INTO ${ParametersContract.TABLE_NAME} VALUES (${ParametersContract.ID_SHORT_TIMING}, 0);"""
        Log.d(TAG, sSQL)
        db.execSQL(sSQL)
    }


    companion object : SingletonHolder<AppDatabase, Context>(::AppDatabase)
}
