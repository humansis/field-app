package cz.applifting.humansis.misc

import android.annotation.SuppressLint
import android.content.Context
import cz.applifting.humansis.di.LOGFILE_PATH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import quanti.com.kotlinlog.Log
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Named


/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 13, November, 2019
 */

class Logger @Inject constructor(@param:Named(LOGFILE_PATH) val logFilePath: String) {

    val maxLines = 30

    @SuppressLint("SimpleDateFormat")
    suspend fun logToFile(tag: String, ctx: Context, message: String) {
        Log.d(tag, message)
        withContext(Dispatchers.IO) {
            val logs = readLogs(ctx)

            val fos = ctx.openFileOutput(logFilePath, Context.MODE_PRIVATE)
            val outputStreamWriter = OutputStreamWriter(fos)

            val newDate = Date()
            val dateFormat = SimpleDateFormat("dd-MM kk:mm:ss.SS")
            val dateStr = dateFormat.format(newDate)

            val log = "$dateStr:$message"
            logs.add(log)


            val startIdx = if (logs.size > maxLines) {
                logs.size - maxLines
            } else {
                0
            }

            val sb = StringBuilder()
            for (i in startIdx until logs.size) {
                sb.append(logs[i]).append('\n')
            }

            outputStreamWriter.write(sb.toString())
            outputStreamWriter.flush()
            outputStreamWriter.close()
        }
    }

    suspend fun readLogs(ctx: Context): LinkedList<String> {
        return withContext(Dispatchers.IO) {
            try {
                val fIn = ctx.openFileInput(logFilePath)
                val inputStreamReader = InputStreamReader(fIn)
                val ll = LinkedList<String>(inputStreamReader.readLines())
                ll
            } catch (e: IOException) {
                LinkedList<String>()
            }
        }
    }


}