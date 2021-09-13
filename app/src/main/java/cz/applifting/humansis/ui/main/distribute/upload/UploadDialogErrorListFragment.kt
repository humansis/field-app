package cz.applifting.humansis.ui.main.distribute.upload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.tryNavigate
import cz.applifting.humansis.ui.BaseFragment
import kotlinx.android.synthetic.main.fragment_dialog_upload_status_error_info.*
import kotlinx.coroutines.launch
import quanti.com.kotlinlog.Log


/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 27, November, 2019
 */
class UploadDialogErrorListFragment : BaseFragment() {

    private lateinit var uploadDialogViewModel: UploadDialogViewModel

    private var isOpeningBeneficiary = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dialog_upload_status_error_info, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        uploadDialogViewModel = ViewModelProviders.of(requireParentFragment(), viewModelFactory)[UploadDialogViewModel::class.java]

        val adapter = ErrorListAdapter {
            it.beneficiaryId?.let { beneficiaryId ->
                if (!isOpeningBeneficiary) {
                    openBeneficiaryDialog(beneficiaryId)
                }
            }
        }
        rl_erros.adapter = adapter
        rl_erros.layoutManager = LinearLayoutManager(context)

        val dividerItemDecoration = DividerItemDecoration(
            rl_erros.context,
            (rl_erros.layoutManager as LinearLayoutManager).orientation
        )
        rl_erros.addItemDecoration(dividerItemDecoration)

        uploadDialogViewModel.syncErrorListLD.observe(viewLifecycleOwner, {
            adapter.update(it)
        })

        btn_back.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            uploadDialogViewModel.changeScreen(Screen.MAIN)
        }
    }

    private fun openBeneficiaryDialog(beneficiaryId: Int) {
        isOpeningBeneficiary = true
        launch {
            val (projectName, distribution, beneficiary) = uploadDialogViewModel.getRelatedEntities(beneficiaryId)
            if (projectName == null || distribution == null || beneficiary == null) {
                return@launch
            }
            tryNavigate(
                R.id.uploadDialog,
                UploadDialogDirections.actionUploadDialogToBeneficiaryDialog(
                    beneficiaryId = beneficiary.id,
                    distributionName = distribution.name,
                    projectName = projectName,
                    isQRVoucher = distribution.isQRVoucherDistribution,
                    isSmartcard = distribution.isSmartcardDistribution
                )
            )
        }.invokeOnCompletion {
            isOpeningBeneficiary = false
        }
    }

    companion object {
        private val TAG = UploadDialogErrorListFragment::class.java.simpleName
    }
}