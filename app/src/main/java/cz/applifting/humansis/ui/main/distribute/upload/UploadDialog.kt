package cz.applifting.humansis.ui.main.distribute.upload

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import cz.applifting.humansis.R
import cz.applifting.humansis.ui.App
import kotlinx.android.synthetic.main.fragment_dialog_upload_status.view.*
import javax.inject.Inject


class UploadDialog : DialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var uploadDialogViewModel: UploadDialogViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        (activity?.application as App).appComponent.inject(this)
        uploadDialogViewModel = ViewModelProviders.of(this, viewModelFactory)[UploadDialogViewModel::class.java]

        val rootView = inflater.inflate(R.layout.fragment_dialog_upload_status, container)
        (rootView as ViewGroup).layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        rootView.iv_cross.setOnClickListener { dismiss() }
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val fragmentManager = childFragmentManager

        uploadDialogViewModel.currentScreenLD.observe(viewLifecycleOwner, {
            val fragmentTransaction = fragmentManager.beginTransaction()
            val fragment = if (it == Screen.MAIN) {
                UploadDialogMainFragment()
            } else {
                UploadDialogErrorListFragment()
            }

            fragmentTransaction.replace(R.id.fc_container, fragment)
            fragmentTransaction.commit()
        })

        return rootView
    }
}