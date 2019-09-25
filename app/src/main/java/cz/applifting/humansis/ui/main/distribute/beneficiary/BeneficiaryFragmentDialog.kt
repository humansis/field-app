package cz.applifting.humansis.ui.main.distribute.beneficiary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.visible
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.HumansisActivity
import kotlinx.android.synthetic.main.fragment_beneficiary.*
import kotlinx.android.synthetic.main.fragment_beneficiary.view.*
import javax.inject.Inject


/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 9. 9. 2019
 */

class BeneficiaryFragmentDialog : DialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: BeneficiaryViewModel by viewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_beneficiary, container, false)
        (activity?.application as App).appComponent.inject(this)

        view.btn_close.setOnClickListener { dismiss() }
        view.btn_confirm_distribution.setOnClickListener { viewModel.confirm() }

        viewModel.distributedLD.observe(viewLifecycleOwner, Observer { isDistributed ->
            tv_status.setValue(getString(if (isDistributed) R.string.distributed else R.string.not_distributed))
            tv_status.setStatus(isDistributed)
//            tv_beneficiary.setValue(args.beneficiaryName)
//            tv_distribution.setValue(args.distributionName)
//            tv_project.setValue(args.projectName)

//            args.bookletId?.let {
//                tv_booklet.setValue(it)
//                tv_booklet.setAction(getString(R.string.rescan_qr), View.OnClickListener {
//                    findNavController().navigateUp()
//                })
//            }

            (activity as HumansisActivity).invalidateOptionsMenu()
        })

        viewModel.refreshingLD.observe(viewLifecycleOwner, Observer {
            pb_loading.visible(it)
            tv_status.visible(!it)
            tv_beneficiary.visible(!it)
            tv_distribution.visible(!it)
            tv_project.visible(!it)
            //tv_booklet.visible(!it && args.bookletId != null)
        })

        //viewModel.loadBeneficiary(args.beneficiaryId)

        return view
    }
}