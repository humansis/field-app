package cz.applifting.humansis.ui.main.distribute.beneficiary.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.visible
import cz.applifting.humansis.model.ReferralType
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.HumansisActivity
import cz.applifting.humansis.ui.main.SharedViewModel
import kotlinx.android.synthetic.main.fragment_confirm_beneficiary.*
import kotlinx.android.synthetic.main.fragment_confirm_beneficiary.view.*
import quanti.com.kotlinlog.Log
import javax.inject.Inject


class ConfirmBeneficiaryDialog : DialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: ConfirmBeneficiaryViewModel by viewModels { viewModelFactory }
    private lateinit var sharedViewModel: SharedViewModel
    private val args: ConfirmBeneficiaryDialogArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_confirm_beneficiary, container, false)
        (activity?.application as App).appComponent.inject(this)
        sharedViewModel = ViewModelProvider(activity as HumansisActivity, viewModelFactory).get(SharedViewModel::class.java)

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        view.setupViews()

        viewModel.initBeneficiary(args.beneficiaryId)

        viewModel.beneficiaryLD.observe(viewLifecycleOwner, {
            tv_referral_title.text = getString(if (it.hasReferral) R.string.edit_referral else R.string.add_referral)
        })

        viewModel.isReferralVisibleLD.observe(viewLifecycleOwner, {
            layout_referral.visible(it) // animated by animateLayoutChanges="true"
            referral_header_indicator.animate().rotation(if (it) 90f else 0f).start()
        })

        viewModel.referralTypeLD.observe(viewLifecycleOwner, {
            spinner_referral_type.apply {
                val spinnerPos = it.toSpinnerPos()
                if (selectedItemPosition != spinnerPos) {
                    setSelection(spinnerPos)
                }
                viewModel.errorLD.value = null
            }
        })

        viewModel.referralNoteLD.observe(viewLifecycleOwner, {
            tv_referral_note.apply {
                if (text.toString() != it) {
                    setText(it)
                }
                viewModel.errorLD.value = null
            }
        })

        viewModel.errorLD.observe(viewLifecycleOwner, {
            tv_error.visibility = if (it == null) View.GONE else View.VISIBLE
            tv_error.text = it?.let { getString(it) }
        })

        return view
    }

    private fun View.setupViews() {
        header_referral.setOnClickListener {
            Log.d(TAG, "Header referral clicked")
            viewModel.toggleReferral()
        }

        val spinnerOptions = viewModel.referralTypes
            .map { getString(it) }
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, 0, spinnerOptions).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner_referral_type.adapter = adapter
        }
        spinner_referral_type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.referralTypeLD.value = position.toReferralType()
            }
        }
        spinner_referral_type.showFloatingLabel() // needed when nothing is initially selected

        tv_referral_note.addTextChangedListener {
            viewModel.referralNoteLD.postValue(tv_referral_note.text?.toString())
        }

        btn_cancel.setOnClickListener {
            Log.d(TAG, "Cancel butotn clicked")
            dismiss()
        }
        btn_confirm.setOnClickListener {
            Log.d(TAG, "Confirm button clicked")
            if (viewModel.tryConfirm(false)) {
                dismiss()
            }
        }
    }

    // +-1 for the "none" value which is not in the enum
    private fun ReferralType?.toSpinnerPos() = this?.let { it.ordinal + 1 } ?: 0
    private fun Int.toReferralType() = if (this == 0) null else ReferralType.values()[this - 1]

    companion object {
        private val TAG = ConfirmBeneficiaryDialog::class.java.simpleName
    }
}