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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.zxing.Result
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.tryNavigate
import cz.applifting.humansis.extensions.visible
import cz.applifting.humansis.misc.NfcCardErrorMessage
import cz.applifting.humansis.misc.NfcInitializer
import cz.applifting.humansis.model.CommodityType
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.HumansisActivity
import cz.applifting.humansis.ui.components.TitledTextView
import cz.applifting.humansis.ui.main.SharedViewModel
import cz.quanti.android.nfc.exception.PINException
import cz.quanti.android.nfc.exception.PINExceptionEnum
import cz.quanti.android.nfc.logger.NfcLogger
import cz.quanti.android.nfc_io_libray.types.NfcUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_card_message.view.*
import kotlinx.android.synthetic.main.fragment_beneficiary.*
import kotlinx.android.synthetic.main.fragment_beneficiary.view.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import quanti.com.kotlinlog.Log
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 9. 9. 2019
 */

class BeneficiaryDialog : DialogFragment(), ZXingScannerView.ResultHandler {
    companion object {
        private const val CAMERA_REQUEST_CODE = 0
        val INVALID_CODE = "Invalid code"
        val ALREADY_ASSIGNED = "Already assigned"
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: BeneficiaryViewModel by viewModels { viewModelFactory }
    private lateinit var sharedViewModel: SharedViewModel
    private var disposable: Disposable? = null

    private val TAG = this.javaClass.simpleName
    val args: BeneficiaryDialogArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            setStyle(STYLE_NORMAL, R.style.FullscreenDialogDarkStatusBar)
        } else {
            setStyle(STYLE_NORMAL, R.style.FullscreenDialog)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(activity!!, theme) {
            override fun onBackPressed() {
                handleBackPressed()
            }
        }
    }

    private fun TitledTextView.setOptionalValue(value: String?) {
        visible(!value.isNullOrEmpty())
        setValue(value)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_beneficiary, container, false)
        (activity?.application as App).appComponent.inject(this)
        sharedViewModel = ViewModelProviders.of(activity as HumansisActivity, viewModelFactory)[SharedViewModel::class.java]
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.apply {
            btn_close.setImageResource(if (args.isFromList) R.drawable.ic_arrow_back else R.drawable.ic_close_black_24dp)
            btn_close.setOnClickListener {
                handleBackPressed()
            }
            tv_booklet.visible(args.isQRVoucher)
            tv_smartcard.visible(args.isSmartcard)
            tv_old_smartcard.visible(args.isSmartcard)
        }

        viewModel.initBeneficiary(args.beneficiaryId)

        sharedViewModel.shouldDismissBeneficiaryDialog.observe(viewLifecycleOwner, Observer {
            if(it) {
                leaveWithSuccess()
                sharedViewModel.shouldDismissBeneficiaryDialog.postValue(false)
            }
        })

        viewModel.beneficiaryLD.observe(viewLifecycleOwner, Observer { beneficiary ->
            // Views
            view.apply {
                tv_status.setValue(getString(if (beneficiary.distributed) R.string.distributed else R.string.not_distributed))
                tv_status.setStatus(beneficiary.distributed)
                tv_beneficiary.setValue("${beneficiary.givenName} ${beneficiary.familyName}")
                tv_distribution.setValue(args.distributionName)
                tv_project.setValue(args.projectName)
                tv_screen_title.text = getString(if (beneficiary.distributed) R.string.detail else R.string.assign)
                tv_screen_subtitle.text = getString(R.string.beneficiary_name, beneficiary.givenName, beneficiary.familyName)
                tv_referral_type.setOptionalValue(beneficiary.referralType?.textId?.let { getString(it) })
                tv_referral_note.setOptionalValue(beneficiary.referralNote)

                if(args.isSmartcard) {
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
                                btn_action.backgroundTintList = context.resources.getColorStateList(R.color.background_revert_btn, context.theme)
                            } else {
                                btn_action.backgroundTintList = context.resources.getColorStateList(R.color.background_revert_btn)
                            }
                        } else {
                            btn_action.visible(false)
                        }
                    } else {
                        btn_action.text = context.getString(if (args.isQRVoucher) R.string.confirm_distribution else R.string.assign)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            btn_action.backgroundTintList = context.resources.getColorStateList(R.color.background_confirm_btn, context.theme)
                        } else {
                            btn_action.backgroundTintList = context.resources.getColorStateList(R.color.background_confirm_btn)
                        }
                    }

                    view.btn_action?.isEnabled = true

                    btn_action.setOnClickListener { _ ->
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
                        if(!args.isSmartcard) {
                            leaveWithSuccess()
                        }
                    }
                }
            }
        })

        viewModel.scannedIdLD.observe(viewLifecycleOwner, Observer {
            viewModel.scanQRBooklet(it)
        })

        viewModel.goBackEventLD.observe(viewLifecycleOwner, Observer {
            handleBackPressed()
        })
    }

    private fun leaveWithSuccess() {
        sharedViewModel.showToast(getString(R.string.success))
        dismiss()
    }

    private val String?.isValidBooklet
        get() = (this != null && this != INVALID_CODE && this != ALREADY_ASSIGNED)

    private fun handleSmartcard(beneficiary: BeneficiaryLocal) {
        var value = 0.0
        var currency = ""
        beneficiary.commodities?.forEach {
            if (it.type == CommodityType.SMARTCARD) {
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

        btn_scan_smartcard.setOnClickListener {
            btn_scan_smartcard.isEnabled = false
            if(NfcInitializer.initNfc(requireActivity())) {
                val pin = generateRandomPin()
                writeBalanceOnCard(
                    value,
                    currency,
                    beneficiary,
                    pin,
                    showScanCardDialog()
                )
            } else {
                btn_scan_smartcard.text = getString(R.string.no_nfc_available)
                btn_scan_smartcard.isEnabled = false
            }
        }

        btn_change_pin.setOnClickListener {
            btn_change_pin.isEnabled = false
            if(NfcInitializer.initNfc(requireActivity())) {
                val pin = generateRandomPin()
                changePinOnCard(
                    beneficiary,
                    pin,
                    showScanCardDialog()
                )
            } else {
                btn_change_pin.text = getString(R.string.no_nfc_available)
                btn_change_pin.isEnabled = false
            }
        }

        if (newSmartcard == null) {
            if(beneficiary.distributed)
            {
                btn_scan_smartcard.visibility = View.GONE
                btn_scan_smartcard.isEnabled = false
                btn_change_pin.visibility = View.VISIBLE
                btn_change_pin.isEnabled = true
            } else {
                btn_scan_smartcard.visibility = View.VISIBLE
                btn_scan_smartcard.isEnabled = true
                btn_change_pin.visibility = View.GONE
                btn_change_pin.isEnabled = false
            }
        } else {
            btn_scan_smartcard.visibility = View.GONE
            btn_change_pin.visibility = View.VISIBLE
        }
    }

    private fun generateRandomPin(): String {
        val first = (0..9).random().toString()
        val second = (0..9).random().toString()
        val third = (0..9).random().toString()
        val fourth = (0..9).random().toString()

        return "${first}${second}${third}${fourth}"
    }

    private fun handleQrVoucher(beneficiary: BeneficiaryLocal) {
        val booklet = beneficiary.qrBooklets?.firstOrNull()

        if (!beneficiary.distributed) {
            tv_booklet.setRescanActionListener {
                viewModel.scanQRBooklet(null)
            }
        }

        tv_booklet.setStatus(beneficiary.distributed)
        tv_booklet.setValue(
            when (booklet) {
                INVALID_CODE -> getString(R.string.invalid_code)
                ALREADY_ASSIGNED -> getString(R.string.already_assigned)
                else -> booklet
            }
        )

        if(view?.btn_action?.isEnabled == true) {
            view?.btn_action?.isEnabled = booklet.isValidBooklet
        }

        if (booklet == null) {
            qr_scanner_holder.visibility = View.VISIBLE
            startScanner(view!!)
        } else {
            qr_scanner_holder.visibility = View.GONE
        }

        if (!isCameraPermissionGranted() && !beneficiary.distributed) {
            requestCameraPermission()
        }
    }

    private fun showScanCardDialog(): AlertDialog {
        val scanCardDialog = AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setMessage(getString(R.string.scan_the_card))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog?.dismiss()
                btn_scan_smartcard?.visibility = View.VISIBLE
                btn_scan_smartcard?.isEnabled = true
                disposable?.dispose()
                disposable = null
            }
            .create()

        scanCardDialog.show()

        return scanCardDialog
    }

    private fun showCardUpdatedDialog(beneficiary: BeneficiaryLocal, pin: String, message: String?) {
        val cardResultDialog = AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setTitle(getString((R.string.card_updated)))
                .setView(layoutInflater.inflate(R.layout.dialog_card_message, null).apply {
                    this.pin.text = pin
                    if(message != null){
                        this.message.text = message
                    } else {
                        this.message.visibility = View.GONE
                    }
                })
                .setCancelable(true)
                .setPositiveButton(getString(R.string.add_referral)) { _, _ ->
                    showAddReferralInfoDialog(beneficiary)
                }
                .setNegativeButton(getString(R.string.close)){ _, _ ->
                    sharedViewModel.shouldDismissBeneficiaryDialog.postValue(true)
                    dismiss()
                }
                .create()
        cardResultDialog.show()
    }

    private fun writeBalanceOnCard(
        balance: Double,
        currency: String,
        beneficiary: BeneficiaryLocal,
        pin: String,
        scanCardDialog: AlertDialog
    ) {
        NfcLogger.d(
                TAG,
                "writeBalanceOnCard: pin: ${pin}, balance: ${balance}, beneficiaryId: ${beneficiary.beneficiaryId}, currencyCode: $currency"
        )
        disposable?.dispose()
        disposable = viewModel.depositMoneyToCard(balance, currency, pin, beneficiary.beneficiaryId)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                {   info ->
                    val tag = info.first
                    val cardContent = info.second
                    val id = tag.id
                    var cardId: String? = null
                    id?.let {
                        cardId = NfcUtil.toHexString(id).toUpperCase(Locale.US)
                    }
                    viewModel.saveCard(cardId, convertTimeForApiRequestBody(Date()))
                    btn_scan_smartcard.visibility = View.GONE
                    scanCardDialog.dismiss()

                    showCardUpdatedDialog(
                        beneficiary,
                        getString(
                            R.string.scanning_card_pin,
                            cardContent.pin
                        ),
                        getString(
                            R.string.scanning_card_balance,
                            "${cardContent.balance} ${cardContent.currencyCode}"
                        )
                    )
                    NfcLogger.d(
                            TAG,
                            "writtenBalanceOnCard: pin: ${cardContent.pin}, balance: ${cardContent.balance}, beneficiaryId: ${beneficiary.beneficiaryId}, currencyCode: ${cardContent.currencyCode}"
                    )
                },
                {
                    var ex = it
                    if (ex is CompositeException && ex.exceptions.isNotEmpty()) {
                        ex = ex.exceptions.get(0)
                    }
                    when (ex) {
                        is PINException -> {
                            NfcLogger.e(this.javaClass.simpleName, ex.pinExceptionEnum.name)
                            if (ex.pinExceptionEnum == PINExceptionEnum.CARD_INITIALIZED) {
                                Log.e(this.javaClass.simpleName,ex)
                                val cardInitializedDialog = AlertDialog.Builder(requireContext(), R.style.DialogTheme)
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
                                if(NfcInitializer.initNfc(requireActivity())) {
                                    writeBalanceOnCard(
                                        balance,
                                        currency,
                                        beneficiary,
                                        pin,
                                        cardInitializedDialog
                                    )
                                }
                            }else {
                                Log.e(this.javaClass.simpleName, ex.pinExceptionEnum.name)
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
                        else -> {
                            Log.e(this.javaClass.simpleName,ex)
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

    private fun changePinOnCard(beneficiary: BeneficiaryLocal, pin: String, scanCardDialog: AlertDialog) {
        disposable?.dispose()
        disposable = viewModel.changePinForCard(pin, beneficiary.beneficiaryId)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                { info ->
                    val cardContent = info.second

                    btn_change_pin.visibility = View.GONE
                    scanCardDialog.dismiss()

                    showCardUpdatedDialog(

                        beneficiary,
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
                            Log.e(this.javaClass.simpleName, ex.pinExceptionEnum.name)
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
                            Log.e(this.javaClass.simpleName,ex)
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

    override fun onResume() {
        super.onResume()
        if (qr_scanner_holder.visibility == View.VISIBLE) {
            startScanner(view!!)
        }
    }

    override fun handleResult(rawResult: Result?) {
        qr_scanner_holder?.visibility = View.GONE
        val scannedId = rawResult.toString()
        viewModel.checkScannedId(scannedId)
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

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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

    private fun handleBackPressed() {
        if (viewModel.beneficiaryLD.value?.hasUnsavedQr == true) {
            showDismissConfirmDialog()
        } else {
            dismiss()
        }
    }

    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (activity as HumansisActivity).checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun showConfirmBeneficiaryDialog(beneficiaryLocal: BeneficiaryLocal) {
        tryNavigate(
            R.id.beneficiaryDialog,
            BeneficiaryDialogDirections.actionBeneficiaryDialogToConfirmBeneficiaryDialog(
                beneficiaryLocal.id
            )
        )
    }

    private fun showAddReferralInfoDialog(beneficiaryLocal: BeneficiaryLocal) {
        tryNavigate(
            R.id.beneficiaryDialog,
            BeneficiaryDialogDirections.actionBeneficiaryDialogToAddReferralInfoDialog(
                beneficiaryLocal.id
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
            .setNegativeButton(R.string.dont_save) { _, _ -> dismiss() }
            .show()
    }

    private fun convertTimeForApiRequestBody(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                .format(date)
    }
}