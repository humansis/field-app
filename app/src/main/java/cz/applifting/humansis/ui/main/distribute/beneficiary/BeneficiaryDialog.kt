package cz.applifting.humansis.ui.main.distribute.beneficiary

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.getCommodityString
import cz.applifting.humansis.extensions.tryNavigate
import cz.applifting.humansis.extensions.visible
import cz.applifting.humansis.misc.DateUtil
import cz.applifting.humansis.misc.DateUtil.convertTimeForApiRequestBody
import cz.applifting.humansis.misc.NfcCardErrorMessage
import cz.applifting.humansis.misc.NfcInitializer
import cz.applifting.humansis.misc.SmartcardUtilities.getExpirationDateAsString
import cz.applifting.humansis.misc.SmartcardUtilities.getLimitsAsText
import cz.applifting.humansis.model.CommodityType
import cz.applifting.humansis.model.api.NationalCardIdType
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.HumansisActivity
import cz.applifting.humansis.ui.components.TitledTextView
import cz.applifting.humansis.ui.main.SharedViewModel
import cz.quanti.android.nfc.dto.v2.Deposit
import cz.quanti.android.nfc.exception.PINException
import cz.quanti.android.nfc.exception.PINExceptionEnum
import cz.quanti.android.nfc_io_libray.types.NfcUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.schedulers.Schedulers
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.android.synthetic.main.dialog_card_message.view.message
import kotlinx.android.synthetic.main.dialog_card_message.view.pin
import kotlinx.android.synthetic.main.fragment_beneficiary.btn_action
import kotlinx.android.synthetic.main.fragment_beneficiary.btn_change_pin
import kotlinx.android.synthetic.main.fragment_beneficiary.btn_scan_smartcard
import kotlinx.android.synthetic.main.fragment_beneficiary.qr_scanner
import kotlinx.android.synthetic.main.fragment_beneficiary.qr_scanner_holder
import kotlinx.android.synthetic.main.fragment_beneficiary.tv_booklet
import kotlinx.android.synthetic.main.fragment_beneficiary.tv_old_smartcard
import kotlinx.android.synthetic.main.fragment_beneficiary.tv_smartcard
import kotlinx.android.synthetic.main.fragment_beneficiary.view.btn_action
import kotlinx.android.synthetic.main.fragment_beneficiary.view.btn_close
import kotlinx.android.synthetic.main.fragment_beneficiary.view.qr_scanner
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_amount
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_beneficiary
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_birth_certificate
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_booklet
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_camp_id
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_distribution
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_drivers_license
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_family_registration
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_humansis_id
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_national_id
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_old_smartcard
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_other
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_passport
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_project
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_referral_note
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_referral_type
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_screen_subtitle
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_screen_title
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_smartcard
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_social_service_card
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_status
import kotlinx.android.synthetic.main.fragment_beneficiary.view.tv_tax_number
import me.dm7.barcodescanner.zxing.ZXingScannerView
import quanti.com.kotlinlog.Log

/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 9. 9. 2019
 */

class BeneficiaryDialog : DialogFragment(), ZXingScannerView.ResultHandler {
    companion object {
        private const val CAMERA_REQUEST_CODE = 0
        const val INVALID_CODE = "Invalid code"
        const val ALREADY_ASSIGNED = "Already assigned"
        private val TAG = this::class.java.simpleName
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: BeneficiaryViewModel by viewModels { viewModelFactory }
    private lateinit var sharedViewModel: SharedViewModel

    private var displayedScanCardDialog: AlertDialog? = null
    private var disposable: Disposable? = null

    private val args: BeneficiaryDialogArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            setStyle(STYLE_NORMAL, R.style.FullscreenDialogDarkStatusBar)
        } else {
            setStyle(STYLE_NORMAL, R.style.FullscreenDialog)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            override fun onBackPressed() {
                handleBackPressed()
            }
        }
    }

    private fun TitledTextView.setOptionalValue(value: String?) {
        visible(!value.isNullOrEmpty())
        value?.let { setValue(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_beneficiary, container, false)
        (activity?.application as App).appComponent.inject(this)
        sharedViewModel = ViewModelProviders.of(
            activity as HumansisActivity,
            viewModelFactory
        )[SharedViewModel::class.java]
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.apply {
            btn_close.setImageResource(if (args.isFromList) R.drawable.ic_arrow_back else R.drawable.ic_close_black_24dp)
            btn_close.setOnClickListener {
                Log.d(TAG, "Close button clicked")
                handleBackPressed()
            }
            tv_booklet.visible(args.isQRVoucher)
            tv_smartcard.visible(args.isSmartcard)
            tv_old_smartcard.visible(args.isSmartcard)
        }

        viewModel.initBeneficiary(args.beneficiaryId)

        sharedViewModel.shouldDismissBeneficiaryDialog.observe(viewLifecycleOwner) {
            leaveWithSuccess()
        }

        viewModel.beneficiaryLD.observe(viewLifecycleOwner) { beneficiary ->
            // Views
            view.apply {
                tv_screen_title.text =
                    getString(if (beneficiary.distributed) R.string.detail else R.string.assign)
                tv_screen_subtitle.text = getString(
                    R.string.beneficiary_name,
                    beneficiary.givenName,
                    beneficiary.familyName
                )
                tv_status.setValue(getString(if (beneficiary.distributed) R.string.distributed else R.string.not_distributed))
                tv_status.setStatus(beneficiary.distributed)
                tv_humansis_id.setValue("${beneficiary.beneficiaryId}")

                beneficiary.nationalIds.forEach {
                    when (it.type) {
                        NationalCardIdType.NATIONAL_ID -> {
                            tv_national_id.setValue(it.number)
                            tv_national_id.visible(true)
                        }
                        NationalCardIdType.TAX_NUMBER -> {
                            tv_tax_number.setValue(it.number)
                            tv_tax_number.visible(true)
                        }
                        NationalCardIdType.PASSPORT -> {
                            tv_passport.setValue(it.number)
                            tv_passport.visible(true)
                        }
                        NationalCardIdType.FAMILY -> {
                            tv_family_registration.setValue(it.number)
                            tv_family_registration.visible(true)
                        }
                        NationalCardIdType.BIRTH_CERTIFICATE -> {
                            tv_birth_certificate.setValue(it.number)
                            tv_birth_certificate.visible(true)
                        }
                        NationalCardIdType.DRIVERS_LICENSE -> {
                            tv_drivers_license.setValue(it.number)
                            tv_drivers_license.visible(true)
                        }
                        NationalCardIdType.CAMP_ID -> {
                            tv_camp_id.setValue(it.number)
                            tv_camp_id.visible(true)
                        }
                        NationalCardIdType.SOCIAL_SERVICE_ID -> {
                            tv_social_service_card.setValue(it.number)
                            tv_social_service_card.visible(true)
                        }
                        NationalCardIdType.OTHER -> {
                            tv_other.setValue(it.number)
                            tv_other.visible(true)
                        }
                        else -> { /* Type.NONE */ }
                    }
                }

                tv_beneficiary.setValue("${beneficiary.givenName} ${beneficiary.familyName}")
                tv_distribution.setValue(args.distributionName)
                tv_project.setValue(args.projectName)
                tv_amount.setOptionalValue(
                    beneficiary.commodities?.first()?.let { commodity ->
                        context.getCommodityString(commodity.value, commodity.unit)
                    }
                )
                tv_referral_type.setOptionalValue(beneficiary.referralType?.textId?.let {
                    getString(
                        it
                    )
                })
                tv_referral_note.setOptionalValue(beneficiary.referralNote)

                if (args.isSmartcard) {
                    if (beneficiary.distributed && args.isSmartcard && beneficiary.newSmartcard == null && !beneficiary.edited) {
                        // it was distributed and already synced with the server
                        tv_smartcard.visibility = View.GONE
                    }
                    view.btn_action?.isEnabled = false
                    view.btn_action?.visibility = View.GONE
                } else {
                    if (beneficiary.distributed) {

                        if (beneficiary.edited) {
                            btn_action.text = context.getString(R.string.revert)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                btn_action.backgroundTintList = context.resources.getColorStateList(
                                    R.color.background_revert_btn,
                                    context.theme
                                )
                            } else {
                                btn_action.backgroundTintList =
                                    AppCompatResources.getColorStateList(
                                        context,
                                        R.color.background_revert_btn
                                    )
                            }
                        } else {
                            btn_action.visible(false)
                        }
                    } else {
                        btn_action.text =
                            context.getString(if (args.isQRVoucher) R.string.confirm_distribution else R.string.assign)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            btn_action.backgroundTintList = context.resources.getColorStateList(
                                R.color.background_confirm_btn,
                                context.theme
                            )
                        } else {
                            btn_action.backgroundTintList = AppCompatResources.getColorStateList(
                                context,
                                R.color.background_confirm_btn
                            )
                        }
                    }

                    view.btn_action?.isEnabled = true

                    btn_action.setOnClickListener {
                        Log.d(TAG, "Action button clicked")
                        if (beneficiary.edited) {
                            if (viewModel.isAssignedInOtherDistribution) {
                                showConfirmBeneficiaryDialog(beneficiary)
                            } else {
                                viewModel.revertBeneficiary()
                            }
                        } else {
                            showConfirmBeneficiaryDialog(beneficiary)
                        }
                    }
                }

                // Handle QR voucher
                if (args.isQRVoucher) {
                    handleQrVoucher(beneficiary)
                }

                if (args.isSmartcard) {
                    handleSmartcard(beneficiary)
                }

                when (viewModel.previousEditState) {
                    null -> viewModel.previousEditState = beneficiary.edited
                    beneficiary.edited -> {
                    }
                    else -> {
                        // edit state changed
                        // Close dialog and notify shareViewModel after beneficiary is saved to db
                        if (!args.isSmartcard) {
                            leaveWithSuccess()
                        }
                    }
                }
            }
        }

        viewModel.scannedIdLD.observe(viewLifecycleOwner) {
            viewModel.scanQRBooklet(it)
        }

        viewModel.goBackEventLD.observe(viewLifecycleOwner) {
            handleBackPressed()
        }
    }

    private fun leaveWithSuccess() {
        sharedViewModel.beneficiaryDialogDissmissedOnSuccess.call()
        sharedViewModel.showToast(getString(R.string.success))
        dismiss()
    }

    private val String?.isValidBooklet
        get() = (this != null && this != INVALID_CODE && this != ALREADY_ASSIGNED)

    private fun handleSmartcard(beneficiary: BeneficiaryLocal) {
        var value = 0.0
        var currency = ""
        beneficiary.commodities?.forEach {
            if (it.type == CommodityType.SMARTCARD) { // TODO use .find instead of forEach with if?
                value = it.value
                currency = it.unit
            }
        }

        val newSmartcard = beneficiary.newSmartcard

        btn_action?.isEnabled = false
        btn_action?.visibility = View.INVISIBLE

        tv_smartcard.setStatus(beneficiary.distributed)
        tv_old_smartcard.setStatus(beneficiary.distributed)
        tv_smartcard.setValue(newSmartcard ?: getString(R.string.none))
        tv_old_smartcard.setValue(beneficiary.smartcard ?: getString(R.string.none))

        if (newSmartcard == null) {
            if (beneficiary.distributed) {
                btn_scan_smartcard.visibility = View.GONE
                btn_scan_smartcard.isEnabled = false
                btn_change_pin.visibility = View.VISIBLE
                btn_change_pin.isEnabled = true

                setChangePinOnClickListener(beneficiary)
            } else {
                btn_scan_smartcard.visibility = View.VISIBLE
                btn_scan_smartcard.isEnabled = true
                btn_change_pin.visibility = View.GONE
                btn_change_pin.isEnabled = false

                setScanSmartcardOnClickListener(beneficiary, value, currency)
            }
        } else {
            btn_scan_smartcard.visibility = View.GONE
            btn_change_pin.visibility = View.VISIBLE
            setChangePinOnClickListener(beneficiary)
        }
    }

    private fun setScanSmartcardOnClickListener(
        beneficiary: BeneficiaryLocal,
        value: Double,
        currency: String
    ) {
        if (NfcInitializer.initNfc(requireActivity())) {
            btn_scan_smartcard.setOnClickListener {
                Log.d(TAG, "Scan smartcard button clicked")
                btn_scan_smartcard.isEnabled = false
                val pin = generateRandomPin()
                writeBalanceOnCard(
                    pin,
                    beneficiary.remote,
                    beneficiary.id,
                    Deposit(
                        amount = value,
                        beneficiaryId = beneficiary.beneficiaryId,
                        currency = currency,
                        assistanceId = beneficiary.assistanceId,
                        expirationDate = DateUtil.stringToDate(beneficiary.dateExpiration),
                        limits = beneficiary.getLimits()
                    ),
                    showScanCardDialog(btn_scan_smartcard)
                )
            }
        } else {
            btn_scan_smartcard.text = getString(R.string.no_nfc_available)
            btn_scan_smartcard.isEnabled = false
        }
    }

    private fun setChangePinOnClickListener(beneficiary: BeneficiaryLocal) {
        if (NfcInitializer.initNfc(requireActivity())) {
            btn_change_pin.setOnClickListener {
                Log.d(TAG, "Change pin button clicked")
                btn_change_pin.isEnabled = false
                val pin = generateRandomPin()
                changePinOnCard(
                    beneficiary,
                    pin,
                    showScanCardDialog(btn_change_pin)
                )
            }
        } else {
            btn_change_pin.text = getString(R.string.no_nfc_available)
            btn_change_pin.isEnabled = false
        }
    }

    private fun enableButtons() {
        if (btn_scan_smartcard.visibility == View.VISIBLE) {
            btn_scan_smartcard.isEnabled = true
        }
        if (btn_change_pin.visibility == View.VISIBLE) {
            btn_change_pin.isEnabled = true
        }
    }

    private fun generateRandomPin(): String {
        val first = (0..9).random()
        val second = (0..9).random()
        val third = (0..9).random()
        val fourth = (0..9).random()

        return "${first}${second}${third}$fourth"
    }

    private fun handleQrVoucher(beneficiary: BeneficiaryLocal) {
        val booklet = beneficiary.qrBooklets?.firstOrNull()

        if (!beneficiary.distributed) {
            tv_booklet.setRescanActionListener {
                viewModel.scanQRBooklet(null)
            }
        }

        tv_booklet.setStatus(beneficiary.distributed)
        tv_booklet.setOptionalValue(
            when (booklet) {
                INVALID_CODE -> getString(R.string.invalid_code)
                ALREADY_ASSIGNED -> getString(R.string.already_assigned)
                else -> booklet
            }
        )

        if (view?.btn_action?.isEnabled == true) {
            view?.btn_action?.isEnabled = booklet.isValidBooklet
        }

        if (booklet == null) {
            qr_scanner_holder.visibility = View.VISIBLE
            startScanner(requireView())
        } else {
            qr_scanner_holder.visibility = View.GONE
        }

        if (!isCameraPermissionGranted() && !beneficiary.distributed) {
            requestCameraPermission()
        }
    }

    private fun showScanCardDialog(clickedButton: MaterialButton): AlertDialog {
        val scanCardDialog = AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setMessage(getString(R.string.scan_the_card))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog?.dismiss()
            }
            .setOnDismissListener {
                clickedButton.visibility = View.VISIBLE
                clickedButton.isEnabled = true
                disposable?.dispose()
                disposable = null
            }
            .create()

        scanCardDialog.show()
        displayedScanCardDialog = scanCardDialog

        return scanCardDialog
    }

    private fun showCardInitializedDialog(): AlertDialog {
        val cardInitializedDialog =
            AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setTitle(getString(R.string.card_initialized))
                .setMessage(getString(R.string.scan_card_again))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog?.dismiss()
                    btn_scan_smartcard?.visibility = View.VISIBLE
                    btn_scan_smartcard?.isEnabled = true
                    disposable?.dispose()
                    disposable = null
                }
                .create()
        cardInitializedDialog.show()

        return cardInitializedDialog
    }

    private fun showCardUpdatedDialog(beneficiaryLocalId: Int, pin: String, message: String?) {
        AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setTitle(getString((R.string.card_updated)))
            .setView(layoutInflater.inflate(R.layout.dialog_card_message, null).apply {
                this.pin.text = pin
                if (message != null) {
                    this.message.text = message
                } else {
                    this.message.visibility = View.GONE
                }
            })
            .setCancelable(true)
            .setPositiveButton(getString(R.string.add_referral)) { _, _ ->
                showAddReferralInfoDialog(beneficiaryLocalId)
            }
            .setNegativeButton(getString(R.string.close)) { _, _ ->
                sharedViewModel.shouldDismissBeneficiaryDialog.call()
                dismiss()
            }
            .create()
            .show()
    }

    private fun writeBalanceOnCard(
        pin: String,
        remote: Boolean,
        beneficiaryLocalId: Int,
        deposit: Deposit,
        scanCardDialog: AlertDialog
    ) {
        Log.d(
            TAG,
            "writeBalanceOnCard: pin: $pin, remote: $remote, deposit: $deposit"
        )
        disposable?.dispose()
        disposable = viewModel.depositMoneyToCard(pin, remote, deposit, ::tagFoundCallBack)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                { info ->
                    val tag = info.first
                    val cardContent = info.second
                    val cardId = NfcUtil.toHexString(tag.id).toUpperCase(Locale.US)
                    viewModel.saveCard(
                        cardId,
                        convertTimeForApiRequestBody(Date()),
                        cardContent.originalBalance,
                        cardContent.balance
                    )
                    btn_scan_smartcard.visibility = View.GONE
                    scanCardDialog.dismiss()
                    showCardUpdatedDialog(
                        beneficiaryLocalId,
                        getString(
                            R.string.scanning_card_pin,
                            cardContent.pin
                        ),
                        getString(
                            R.string.scanning_card_balance,
                            "${cardContent.balance} ${cardContent.currencyCode}" +
                                    if (cardContent.balance != 0.0) {
                                        getExpirationDateAsString(
                                            cardContent.expirationDate,
                                            requireContext()
                                        ) +
                                                getLimitsAsText(
                                                    cardContent.limits,
                                                    cardContent.currencyCode,
                                                    requireContext()
                                                )
                                    } else {
                                        String()
                                    }
                        )
                    )
                    Log.d(
                        TAG,
                        "writtenBalanceOnCard: cardContent: $cardContent"
                    )
                },
                {
                    var ex = it
                    if (ex is CompositeException && ex.exceptions.isNotEmpty()) {
                        ex = ex.exceptions[0]
                    }
                    when (ex) {
                        is PINException -> {
                            when (ex.pinExceptionEnum) {
                                PINExceptionEnum.CARD_INITIALIZED -> {
                                    if (NfcInitializer.initNfc(requireActivity())) {
                                        writeBalanceOnCard(
                                            pin,
                                            remote,
                                            beneficiaryLocalId,
                                            deposit,
                                            showCardInitializedDialog()
                                        )
                                    }
                                }
                                else -> {
                                    Toast.makeText(
                                        requireContext(),
                                        NfcCardErrorMessage.getNfcCardErrorMessage(
                                            ex.pinExceptionEnum,
                                            requireActivity()
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                        else -> {
                            Log.e(this.javaClass.simpleName, ex)
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.card_error),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    btn_scan_smartcard.isEnabled = true
                    scanCardDialog.dismiss()
                }
            )
    }

    private fun changePinOnCard(
        beneficiary: BeneficiaryLocal,
        pin: String,
        scanCardDialog: AlertDialog
    ) {
        disposable?.dispose()
        disposable = viewModel.changePinForCard(pin, beneficiary.beneficiaryId, ::tagFoundCallBack)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                { info ->
                    val cardContent = info.second

                    btn_change_pin.visibility = View.GONE
                    scanCardDialog.dismiss()

                    showCardUpdatedDialog(
                        beneficiary.id,
                        getString(
                            R.string.changing_pin_result,
                            cardContent.pin
                        ),
                        null
                    )
                },
                {
                    var ex = it
                    if (ex is CompositeException && ex.exceptions.isNotEmpty()) {
                        ex = ex.exceptions[0]
                    }
                    when (ex) {
                        is PINException -> {
                            Toast.makeText(
                                requireContext(),
                                NfcCardErrorMessage.getNfcCardErrorMessage(
                                    ex.pinExceptionEnum,
                                    requireActivity()
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.card_error),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    btn_change_pin.isEnabled = true
                    scanCardDialog.dismiss()
                }
            )
    }

    private fun tagFoundCallBack() {
        requireActivity().runOnUiThread {
            displayedScanCardDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.let {
                it.isEnabled = false
            }
        }
    }

    override fun handleResult(rawResult: Result?) {
        qr_scanner_holder?.visibility = View.GONE
        val scannedId = rawResult.toString()
        viewModel.checkScannedId(scannedId)
    }

    override fun onResume() {
        super.onResume()
        if (qr_scanner_holder.visibility == View.VISIBLE) {
            startScanner(requireView())
        }
    }

    override fun onPause() {
        if (args.isQRVoucher) {
            qr_scanner.stopCamera()
        }
        if (args.isSmartcard) {
            NfcInitializer.disableForegroundDispatch(requireActivity())
        }

        super.onPause()
    }

    override fun onStop() {
        displayedScanCardDialog?.dismiss()
        disposable?.dispose()
        enableButtons()
        super.onStop()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (CAMERA_REQUEST_CODE == requestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.beneficiaryLD.postValue(viewModel.beneficiaryLD.value)
            } else {
                // permission not granted, go to previous screen
                findNavController().navigateUp()
            }
        }
    }

    private val String?.isValidSmartcard
        get() = (this != null)

    private fun startScanner(view: View) {
        view.apply {
            qr_scanner.setResultHandler(this@BeneficiaryDialog)
            qr_scanner.startCamera()
            qr_scanner.setAutoFocus(true)
            qr_scanner.setSquareViewFinder(true)
            qr_scanner.setFormats(mutableListOf(BarcodeFormat.QR_CODE))
            // for HUAWEI phones, according to docs
            qr_scanner.setAspectTolerance(0.1f)
        }
    }

    private val BeneficiaryLocal.hasUnsavedQr
        get() = args.isQRVoucher && qrBooklets?.firstOrNull().isValidBooklet && !distributed

    private val BeneficiaryLocal.hasInvalidQR
        get() = args.isQRVoucher && !qrBooklets.isNullOrEmpty() && !qrBooklets.firstOrNull().isValidBooklet

    private fun handleBackPressed() {
        when {
            viewModel.beneficiaryLD.value?.hasUnsavedQr == true -> {
                showDismissConfirmDialog()
            }
            viewModel.beneficiaryLD.value?.hasInvalidQR == true -> {
                viewModel.revertBeneficiary()
                dismiss()
            }
            else -> {
                dismiss()
            }
        }
    }

    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (activity as HumansisActivity).checkSelfPermission(
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showConfirmBeneficiaryDialog(beneficiaryLocal: BeneficiaryLocal) {
        tryNavigate(
            R.id.beneficiaryDialog,
            BeneficiaryDialogDirections.actionBeneficiaryDialogToConfirmBeneficiaryDialog(
                beneficiaryLocal.id
            )
        )
    }

    private fun showAddReferralInfoDialog(beneficiaryLocalId: Int) {
        tryNavigate(
            R.id.beneficiaryDialog,
            BeneficiaryDialogDirections.actionBeneficiaryDialogToAddReferralInfoDialog(
                beneficiaryLocalId
            )
        )
    }

    private fun showDismissConfirmDialog() {
        AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setTitle(R.string.confirm_scan_title)
            .setMessage(R.string.confirm_scan_question)
            .setPositiveButton(R.string.confirm_distribution) { _, _ ->
                viewModel.beneficiaryLD.value?.let { showConfirmBeneficiaryDialog(it) } ?: dismiss()
            }
            .setNegativeButton(R.string.dont_save) { _, _ ->
                viewModel.revertBeneficiary()
                dismiss()
            }
            .show()
    }
}