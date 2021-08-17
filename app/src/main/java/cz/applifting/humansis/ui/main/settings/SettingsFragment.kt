package cz.applifting.humansis.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.isNetworkConnected
import cz.applifting.humansis.misc.SendLogDialogFragment
import cz.applifting.humansis.model.Country
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.BaseFragment
import cz.applifting.humansis.ui.HumansisActivity
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.launch


/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 9. 10. 2019
 */

class SettingsFragment : BaseFragment() {

    private lateinit var adapter: CountryAdapter

    private val viewModel: SettingsViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as App).appComponent.inject(this)
        setHasOptionsMenu(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as HumansisActivity).supportActionBar?.title = getString(R.string.action_settings)
        (activity as HumansisActivity).supportActionBar?.subtitle = ""

        adapter = CountryAdapter(requireContext())
        launch {
            val countries = viewModel.getCountries(context)
            adapter.setData(countries)
            spinner_country.setSelection(adapter.getCountryPositionByIso3(viewModel.getCountrySettings()))
        }
        spinner_country.adapter = adapter

        spinner_country.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val country = parent?.getItemAtPosition(position) as Country
                if ((activity as HumansisActivity).isNetworkConnected()) {
                    viewModel.updateCountrySettings(country.iso3)
                }
            }
        }

        sharedViewModel.networkStatus.observe(viewLifecycleOwner, Observer {
            spinner_country.isEnabled = it
        })

        btn_export_logs.setOnClickListener {
            SendLogDialogFragment.newInstance(
                sendEmailAddress = getString(R.string.send_email_adress),
                title = getString(R.string.logs_dialog_title),
                message = getString(R.string.logs_dialog_message),
                emailButtonText = getString(R.string.logs_dialog_email_button),
                dialogTheme = R.style.DialogTheme
            ).show(requireActivity().supportFragmentManager, "TAG")
        }

        viewModel.savedLD.observe(viewLifecycleOwner, Observer<Boolean> {
            val message = if (it) {
                sharedViewModel.forceSynchronize()
                getString(R.string.settings_country_update_success)
            } else {
                getString(R.string.settings_country_update_error)
            }

            view?.let { view ->
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
            }
        })
    }
}