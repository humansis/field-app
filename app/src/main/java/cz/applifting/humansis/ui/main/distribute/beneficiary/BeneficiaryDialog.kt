package cz.applifting.humansis.ui.main.distribute.beneficiary

import android.Manifest
import android.app.Dialog
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.tryNavigate
import cz.applifting.humansis.extensions.visible
import cz.applifting.humansis.model.CommodityType
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.HumansisActivity
import cz.applifting.humansis.ui.components.TitledTextView
import cz.applifting.humansis.ui.main.SharedViewModel
import cz.quanti.android.nfc.exception.PINException
import cz.quanti.android.nfc.exception.PINExceptionEnum
import cz.quanti.android.nfc_io_libray.types.NfcUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_beneficiary.*
import kotlinx.android.synthetic.main.fragment_beneficiary.view.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
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
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var dialog: AlertDialog? = null
    private var disposable: Disposable? = null

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

                if (beneficiary.distributed) {
                    if(args.isSmartcard && beneficiary.newSmartcard == null && !beneficiary.edited) {
                        // it was distributed and already synced with the server
                        tv_smartcard.visibility = View.GONE
                    }

                    if (beneficiary.edited) {
                        btn_action.text = context.getString(R.string.revert)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            btn_action.setBackgroundTintList( context.resources.getColorStateList(R.color.background_revert_btn,context.theme) )
                        } else {
                            btn_action.setBackgroundTintList( context.resources.getColorStateList(R.color.background_revert_btn) )
                        }
                    } else {
                        btn_action.visible(false)
                    }
                } else {
                    btn_action.text = context.getString(if (args.isQRVoucher) R.string.confirm_distribution else R.string.assign)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        btn_action.setBackgroundTintList( context.resources.getColorStateList(R.color.background_confirm_btn,context.theme) )
                    } else {
                        btn_action.setBackgroundTintList( context.resources.getColorStateList(R.color.background_confirm_btn) )
                    }
                }

                view?.btn_action?.isEnabled = true

                // Handle QR voucher
                if (args.isQRVoucher) {
                    handleQrVoucher(beneficiary)
                }

                if (args.isSmartcard) {
                    handleSmartcard(beneficiary)
                }

                btn_action.setOnClickListener { _ ->
                    if(beneficiary.newSmartcard != null
                            && beneficiary.distributed
                            && beneficiary.edited) {
                        Toast.makeText(requireContext(), getString(R.string.delete_card_first), Toast.LENGTH_LONG).show()
                    } else {
                        if(args.isSmartcard
                                && beneficiary.smartcard != null
                                && beneficiary.newSmartcard != null
                                && beneficiary.smartcard != beneficiary.newSmartcard) {
                            val cardMismatchDialogView: View = layoutInflater.inflate(R.layout.dialog_card_mismatch, null)

                            AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                                .setView(cardMismatchDialogView)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    showConfirmBeneficiaryDialog(beneficiary)
                                }
                                .setNegativeButton(android.R.string.cancel) { _, _ ->
                                }
                                .show()
                        }


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

                when (viewModel.previousEditState) {
                    null -> viewModel.previousEditState = beneficiary.edited
                    beneficiary.edited -> {}
                    else -> {
                        // edit state changed
                        // Close dialog and notify shareViewModel after beneficiary is saved to db
                        sharedViewModel.showToast(getString(R.string.success))
                        dismiss()
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

    private val String?.isValidBooklet
        get() = (this != null && this != INVALID_CODE && this != ALREADY_ASSIGNED)

    private fun handleSmartcard(beneficiary: BeneficiaryLocal) {
        var value: Int = 0
        var currency: String = ""
        beneficiary.commodities?.forEach {
            if (it.type == CommodityType.SMARTCARD) {
                value = it.value
                currency = it.unit
            }
        }

        val newSmartcard = beneficiary.newSmartcard

        if(view?.btn_action?.isEnabled == true) {
            view?.btn_action?.isEnabled = newSmartcard.isValidSmartcard || btn_action.text.equals(getString(R.string.revert))
        }

        tv_smartcard.setStatus(beneficiary.distributed)
        tv_old_smartcard.setStatus(beneficiary.distributed)
        tv_smartcard.setValue(newSmartcard ?: getString(R.string.none))


        tv_old_smartcard.setValue(beneficiary.smartcard ?: getString(R.string.none))

        btn_scan_smartcard.setOnClickListener {
            btn_scan_smartcard.isEnabled = false
            if(startSmartcardScanner()) {
                if(beneficiary.smartcard != null) {
                    AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                        .setCancelable(false)
                        .setTitle(getString(R.string.scan_card))
                        .setPositiveButton(getString(R.string.old_card)) { _, _ ->
                            writeBalanceOnCard(value, currency, beneficiary, false, false, "")
                        }
                        .setNegativeButton(getString(R.string.new_card)) { _, _ ->
                            handleNewSmartcard(value, currency, beneficiary)
                        }
                        .show()
                } else {
                    handleNewSmartcard(value, currency, beneficiary)
                }

            } else {
                btn_scan_smartcard.text = getString(R.string.no_nfc_available)
                btn_scan_smartcard.isEnabled = false
            }
        }

        if (newSmartcard == null) {
            if(beneficiary.distributed)
            {
                btn_scan_smartcard.visibility = View.GONE
                btn_scan_smartcard.isEnabled = false
            } else {
                btn_scan_smartcard.visibility = View.VISIBLE
                btn_scan_smartcard.isEnabled = true
            }
            btn_remove_card.visibility = View.GONE
        } else {
            btn_scan_smartcard.visibility = View.GONE
            btn_remove_card.visibility = View.VISIBLE
        }

        btn_remove_card.setOnClickListener{
            btn_remove_card.isEnabled = false
            if(startSmartcardScanner()) {
                writeBalanceOnCard(-value, currency, beneficiary, true, false, "")
            } else {
                btn_scan_smartcard.text = getString(R.string.no_nfc_available)
                btn_scan_smartcard.isEnabled = false
            }
        }
    }

    private fun handleNewSmartcard(value: Int, currency: String, beneficiary: BeneficiaryLocal) {
        val cardPinDialogView: View = layoutInflater.inflate(R.layout.dialog_card_pin, null)
        AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setCancelable(false)
            .setView(cardPinDialogView)
            .setPositiveButton(getString(R.string.ok)){ _, _ ->
                val pinEditTextView =
                    cardPinDialogView.findViewById<TextInputEditText>(R.id.pinEditText)
                val pin = pinEditTextView.text.toString()
                writeBalanceOnCard(value, currency, beneficiary, false, true, pin)
            }
            .setNegativeButton(getString(R.string.cancel)){ _, _ ->
                btn_scan_smartcard.visibility = View.VISIBLE
                btn_scan_smartcard.isEnabled = true
            }
            .show()
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

    private fun initNfc(): Boolean {
        nfcAdapter = NfcAdapter.getDefaultAdapter(requireActivity())

        if (nfcAdapter == null) {
            // NFC is not available on this device
            return false
        }

        pendingIntent = PendingIntent.getActivity(
            requireActivity(), 0,
            Intent(requireActivity(), requireActivity().javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )

        nfcAdapter?.let { nfcAdapter ->
            if (!nfcAdapter.isEnabled) {
                showWirelessSettings()
            }
            nfcAdapter.enableForegroundDispatch(requireActivity(), pendingIntent, null, null)
        }

        return true
    }

    private fun writeBalanceOnCard(balance: Int, currency: String, beneficiary: BeneficiaryLocal, remove: Boolean, isNew: Boolean, pin: String) {

        val otherCard = if (remove) {
            beneficiary.newSmartcard
        } else {
            beneficiary.smartcard
        }

        disposable?.dispose()
        disposable = viewModel.depositMoneyToCard(balance.toDouble(), currency, otherCard, isNew, pin, beneficiary.beneficiaryId)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                { tag ->
                    if(remove) {
                        btn_remove_card.visibility = View.GONE
                        viewModel.saveCard(null)
                        btn_scan_smartcard.visibility = View.VISIBLE
                        btn_scan_smartcard.isEnabled = true
                    } else {
                        val id = tag?.id
                        var cardId: String? = null
                        id?.let {
                            cardId = NfcUtil.toHexString(id).toUpperCase(Locale.US)
                        }
                        viewModel.saveCard(cardId)
                        btn_scan_smartcard.visibility = View.GONE
                        btn_remove_card.visibility = View.VISIBLE
                        btn_remove_card.isEnabled = true
                    }

                    dialog?.dismiss()
                },
                { ex ->
                    when(ex) {
                        is PINException -> {
                            Log.e(this.javaClass.simpleName, ex.pinExceptionEnum.name)
                            Toast.makeText(
                                requireContext(),
                                getNfcCardErrorMessage(ex.pinExceptionEnum),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is CardMismatchException -> {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.card_mismatch),
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

                    if(remove) {
                        btn_remove_card.isEnabled = true
                    } else
                    {
                        btn_scan_smartcard.isEnabled = true
                    }
                    dialog?.dismiss()
                }
            )

        dialog = AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setMessage(getString(R.string.scan_the_card))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.cancel)) { dialog, id ->
                dialog?.dismiss()
                if(remove) {
                    btn_scan_smartcard?.visibility = View.GONE
                    btn_scan_smartcard?.isEnabled = false
                    btn_remove_card?.visibility = View.VISIBLE
                    btn_remove_card?.isEnabled = true
                } else {
                    btn_scan_smartcard?.visibility = View.VISIBLE
                    btn_scan_smartcard?.isEnabled = true
                    btn_remove_card?.visibility = View.GONE
                    btn_remove_card?.isEnabled = false
                }
                disposable?.dispose()
                disposable = null
            }
            .create()
        dialog?.show()
    }

    private fun getNfcCardErrorMessage(pinExceptionEnum: PINExceptionEnum): String {
        return when (pinExceptionEnum) {
            PINExceptionEnum.CARD_LOCKED -> getString(R.string.card_locked)
            PINExceptionEnum.INCORRECT_PIN -> getString(R.string.incorrect_pin)
            PINExceptionEnum.INVALID_DATA -> getString(R.string.invalid_data)
            PINExceptionEnum.UNSUPPORTED_VERSION -> getString(R.string.invalid_version)
            PINExceptionEnum.DIFFERENT_CURRENCY -> getString(R.string.currency_mismatch)
            PINExceptionEnum.TAG_LOST -> getString(R.string.tag_lost_card_error)
            PINExceptionEnum.DIFFERENT_USER -> getString(R.string.different_user_card_error)
            else -> getString(R.string.card_error)
        }
    }

    private fun startSmartcardScanner(): Boolean {
        return initNfc()
    }

    private fun showWirelessSettings() {
        Toast.makeText(
            requireContext(),
            getString(R.string.you_need_to_enable_nfc),
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
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
            nfcAdapter?.disableForegroundDispatch(requireActivity())
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
}