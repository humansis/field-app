package cz.applifting.humansis.misc

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import cz.applifting.humansis.R
import cz.applifting.humansis.databinding.DialogLogsBinding
import kotlinx.android.synthetic.main.dialog_card_message.view.*
import kotlinx.android.synthetic.main.dialog_card_message.view.message
import kotlinx.android.synthetic.main.dialog_logs.view.*
import kotlinx.coroutines.*
import quanti.com.kotlinlog.Log
import quanti.com.kotlinlog.utils.*
import java.io.File

/**
 * Created by Trnka Vladislav on 20.06.2017.
 *
 * Dialog that shows user options to save or send logs
 */

class SendLogDialogFragment : DialogFragment() {

    // TODO temporary hotfix, delete after implementing fixed version of kotlinlogger

    companion object {
        const val MESSAGE = "send_message"
        const val TITLE = "send_title"
        const val EMAIL_BUTTON_TEXT = "email_button"
        const val FILE_BUTTON_TEXT = "file_button"
        const val SEND_EMAIL_ADDRESSES = "send_address"
        const val EXTRA_FILES = "extra_files"
        const val DIALOG_THEME = "dialog_theme"
        private val TAG = SendLogDialogFragment::class.java.simpleName

        @JvmOverloads
        @JvmStatic
        fun newInstance(
            sendEmailAddress: String,
            message: String = "Would you like to send logs by email or save them to SD card?",
            title: String = "Send logs",
            emailButtonText: String = "Email",
            fileButtonText: String = "Save",
            extraFiles: List<File> = arrayListOf(),
            dialogTheme: Int? = null
        ) = newInstance(
            arrayOf(sendEmailAddress),
            message,
            title,
            emailButtonText,
            fileButtonText,
            extraFiles,
            dialogTheme
        )

        @JvmOverloads
        @JvmStatic
        fun newInstance(
            sendEmailAddress: Array<String>,
            message: String = "Would you like to send logs by email or save them to SD card?",
            title: String = "Send logs",
            emailButtonText: String = "Email",
            fileButtonText: String = "Save",
            extraFiles: List<File> = arrayListOf(),
            dialogTheme: Int? = null
        ): SendLogDialogFragment {
            val myFragment = SendLogDialogFragment()

            val args = Bundle()
            args.putString(MESSAGE, message)
            args.putString(TITLE, title)
            args.putString(EMAIL_BUTTON_TEXT, emailButtonText)
            args.putString(FILE_BUTTON_TEXT, fileButtonText)
            args.putStringArray(SEND_EMAIL_ADDRESSES, sendEmailAddress)
            args.putSerializable(EXTRA_FILES, ArrayList(extraFiles))
            if (dialogTheme != null) {
                args.putInt(DIALOG_THEME, dialogTheme)
            }

            myFragment.arguments = args

            return myFragment
        }
    }

    private var zipFile: Deferred<File>? = null

    private lateinit var dialogLogsBinding: DialogLogsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        zipFile = CoroutineScope(Dispatchers.IO).async {
            val extraFiles = requireArguments().getSerializable(EXTRA_FILES) as ArrayList<File>
            getZipOfLogs(requireActivity().applicationContext, 30, extraFiles)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogLogsBinding = DialogLogsBinding.inflate(layoutInflater)

        val hasFilePermission = requireActivity().applicationContext.hasFileWritePermission()

        val dialog = AlertDialog.Builder(requireContext(), requireArguments().getInt(DIALOG_THEME))
            .apply {
                setView(dialogLogsBinding.root.apply {
                    this.title.text = requireArguments().getString(TITLE)
                    this.message.text = requireArguments().getString(MESSAGE)
                })
                setPositiveButton(requireArguments().getString(EMAIL_BUTTON_TEXT)) { _, _ -> }
                if (hasFilePermission) {
                    setNeutralButton(requireArguments().getString(FILE_BUTTON_TEXT)) { _, _ -> }
                }
            }.create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            Log.d(TAG, "Positive Button Clicked")
            updateDialog(dialog)
            shareLogs(dialog)
        }

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            Log.d(TAG, "Neutral button clicked")
            updateDialog(dialog)
            saveLogs(dialog)
        }

        return dialog
    }

    /**
     * On button click
     * Update dialog UI to show progress
     */
    private fun showProgress(dialog: AlertDialog) {
        dialog.setCancelable(false)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
        dialogLogsBinding.message.text = requireContext().getString(R.string.preparing_logs)
        dialogLogsBinding.progressBar.isVisible = true
    }

    /**
     * On positive button click
     * Create zip of all logs and open email client to send
     */
    private fun shareLogs(dialog: AlertDialog) = CoroutineScope(Dispatchers.Main).launch {

        val appContext = this@SendLogDialogFragment.requireContext().applicationContext

        val addresses = requireArguments().getStringArray(SEND_EMAIL_ADDRESSES)
        val subject = getString(R.string.logs_email_subject) + " " + getFormattedFileNameDayNow()
        val bodyText = getString(R.string.logs_email_text)

        // await non block's current thread
        val zipFileUri = zipFile?.await()?.getUriForFile(appContext)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822" // email
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_EMAIL, addresses)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, bodyText)
            putExtra(Intent.EXTRA_STREAM, zipFileUri)
        }

        appContext.packageManager?.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.let {
                appContext.grantUriPermission(
                    it.activityInfo.packageName,
                    zipFileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        dialog.dismiss()

        try {
            startActivity(Intent.createChooser(intent, "Send mail..."))
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(
                appContext,
                getString(R.string.logs_email_no_client_installed),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * On neutral button click
     * Copy ZIP of all logs to sd card
     */
    private fun saveLogs(dialog: AlertDialog) = CoroutineScope(Dispatchers.Main).launch {

        val appContext = this@SendLogDialogFragment.requireContext().applicationContext

        val file = zipFile?.await()?.copyLogsTOSDCard(requireContext())

        dialog.dismiss()

        Toast.makeText(
            appContext,
            "File successfully copied" + "\n" + file?.absolutePath,
            Toast.LENGTH_LONG
        ).show()
    }
}
