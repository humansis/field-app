package cz.applifting.humansis.ui.main.distribute.beneficiary.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import cz.applifting.humansis.R
import cz.applifting.humansis.model.ReferralType
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.HumansisActivity
import cz.applifting.humansis.ui.main.SharedViewModel
import kotlinx.android.synthetic.main.fragment_add_referral_info.*
import javax.inject.Inject

class AddReferralInfoDialog : DialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: ConfirmBeneficiaryViewModel by lazy{ ViewModelProviders.of(this, viewModelFactory)[ConfirmBeneficiaryViewModel::class.java] }
    private lateinit var sharedViewModel: SharedViewModel
    private val args: AddReferralInfoDialogArgs by navArgs()

    private var shouldEnableConfirmLD = MutableLiveData<Pair<Boolean, Boolean>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (requireActivity().application as App).appComponent.inject(this)
        val view = inflater.inflate(R.layout.fragment_add_referral_info, container, false)
        sharedViewModel = ViewModelProviders.of(activity as HumansisActivity, viewModelFactory)[SharedViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setupViews()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false

        viewModel.initBeneficiary(args.beneficiaryId)
        viewModel.isReferralVisibleLD.postValue(true)

        viewModel.referralTypeLD.observe(viewLifecycleOwner, {
            spinner_referral_type_referral?.apply {
                val spinnerPos = it.toSpinnerPos()
                if (selectedItemPosition != spinnerPos) {
                    setSelection(spinnerPos)
                }
                viewModel.errorLD.value = null
            }
        })

        viewModel.referralNoteLD.observe(viewLifecycleOwner, {
            tv_referral_note_referral?.apply {
                if (text.toString() != it) {
                    setText(it)
                }
                setUpConfirmButton()
                viewModel.errorLD.value = null
            }
        })

        viewModel.errorLD.observe(viewLifecycleOwner, {
            tv_error_referral?.visibility = if (it == null) View.GONE else View.VISIBLE
            tv_error_referral?.text = it?.let { getString(it) }
        })

        shouldEnableConfirmLD.observe(viewLifecycleOwner, {
            btn_confirm_referral.isEnabled = it.first && it.second
        })

        super.onViewCreated(view, savedInstanceState)
    }

    private fun setupViews() {
        val spinnerOptions = viewModel.referralTypes
            .map { getString(it) }
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, 0, spinnerOptions).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner_referral_type_referral?.adapter = adapter
        }
        spinner_referral_type_referral?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setUpConfirmButton()
                viewModel.referralTypeLD.value = position.toReferralType()
            }
        }
        spinner_referral_type_referral?.showFloatingLabel() // needed when nothing is initially selected

        tv_referral_note_referral?.addTextChangedListener {
            setUpConfirmButton()
            viewModel.referralNoteLD.postValue(tv_referral_note_referral.text?.toString())
        }

        btn_cancel_referral?.setOnClickListener {
            sharedViewModel.shouldDismissBeneficiaryDialog.call()
            dismiss()
        }
        btn_confirm_referral?.setOnClickListener {
            if (viewModel.tryConfirm(true)) {
                sharedViewModel.shouldDismissBeneficiaryDialog.call()
                dismiss()
            }
        }
    }

    private fun setUpConfirmButton() {
        var first: Boolean
        spinner_referral_type_referral.apply {
            first = selectedItemPosition.toReferralType() != null
        }
        val second: Boolean = !tv_referral_note_referral.text.isNullOrEmpty()
        shouldEnableConfirmLD.postValue(Pair(first, second))

    }

    // +-1 for the "none" value which is not in the enum
    private fun ReferralType?.toSpinnerPos() = this?.let { it.ordinal + 1 } ?: 0
    private fun Int.toReferralType() = if (this == 0) null else ReferralType.values()[this - 1]
}