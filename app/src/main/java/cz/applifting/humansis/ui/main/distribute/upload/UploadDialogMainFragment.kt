package cz.applifting.humansis.ui.main.distribute.upload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.format
import cz.applifting.humansis.extensions.isNetworkConnected
import cz.applifting.humansis.extensions.simpleDrawable
import cz.applifting.humansis.extensions.visible
import cz.applifting.humansis.ui.BaseFragment
import kotlinx.android.synthetic.main.fragment_dialog_upload_status_main.*
import quanti.com.kotlinlog.Log

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 27, November, 2019
 */
class UploadDialogMainFragment : BaseFragment() {

    private lateinit var uploadDialogViewModel: UploadDialogViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dialog_upload_status_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        uploadDialogViewModel = ViewModelProviders.of(requireParentFragment(), viewModelFactory)[UploadDialogViewModel::class.java]

        val online = context?.isNetworkConnected() ?: false
        btn_sync.isEnabled = online

        iv_connection_status.simpleDrawable(if (online) R.drawable.ic_online else R.drawable.ic_offline)
        tv_connectoin_status.text = getString(if (online) R.string.online else R.string.offline)

        sharedViewModel.networkStatus.observe(viewLifecycleOwner) {
            btn_sync.isEnabled = it
            iv_connection_status.simpleDrawable(if (it) R.drawable.ic_online else R.drawable.ic_offline)
            tv_connectoin_status.text = getString(if (it) R.string.online else R.string.offline)
        }

        sharedViewModel.syncNeededLD.observe(viewLifecycleOwner) {
            tv_changes.text =
                getString(if (it) R.string.pending_local_changes else R.string.no_pending_changes)
            tv_changes.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (it) R.color.red else R.color.green
                )
            )
        }

        sharedViewModel.syncState.observe(viewLifecycleOwner) {
            btn_sync.visibility = if (it.isLoading) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            pb_upload.visible(it.isLoading)

            tv_sync_failed.visible(it.lastSyncFail != null)
            tv_sync_failed_date.visible(it.lastSyncFail != null)
            tv_sync_failed_date.text = it.lastSyncFail?.format()
            btn_show_error_info.visible(it.lastSyncFail != null)

            tv_current_data_date.text = it.lastDownload?.format()
        }

        uploadDialogViewModel.syncSummary.observe(viewLifecycleOwner) {
            tv_sync_summary.visible(!it.isNullOrBlank())
            tv_sync_summary.text = it
        }

        btn_sync.setOnClickListener {
            Log.d(TAG, "Sync button clicked")
            sharedViewModel.forceSynchronize()
            // TODO disable button unless the work was stuck for long time
        }

        btn_show_error_info.setOnClickListener {
            Log.d(TAG, "Show error button clicked")
            uploadDialogViewModel.changeScreen(Screen.ERROR_INFO)
        }
    }

    companion object {
        private val TAG = UploadDialogMainFragment::class.java.simpleName
    }
}