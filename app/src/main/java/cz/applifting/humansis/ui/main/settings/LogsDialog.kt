package cz.applifting.humansis.ui.main.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import cz.applifting.humansis.R
import cz.applifting.humansis.misc.SendLogDialogFragment
import kotlinx.android.synthetic.main.dialog_logs.view.*

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 13, November, 2019
 */
class LogsDialog : DialogFragment() {

    val args: LogsDialogArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view =  inflater.inflate(R.layout.dialog_logs, container, false)
        view.tv_logs.text = args.logs
        view.tv_logs.movementMethod = ScrollingMovementMethod()

        view.btn_cancel.setOnClickListener {
            dialog?.cancel()
        }

        view.btn_export.setOnClickListener {
            SendLogDialogFragment.newInstance(
                    sendEmailAddress = getString(R.string.send_email_adress),
                    title = getString(R.string.logs_dialog_title),
                    message = getString(R.string.logs_dialog_message),
                    emailButtonText = getString(R.string.logs_dialog_email_button),
                    dialogTheme = R.style.DialogTheme
            ).show(requireActivity().supportFragmentManager, "TAG")
        }

        view.btn_copy.setOnClickListener {
            val clipboard = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText("logs", args.logs)
            clipboard?.setPrimaryClip(clip)
        }

        return view
    }

}