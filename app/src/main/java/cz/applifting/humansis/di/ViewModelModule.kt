package cz.applifting.humansis.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cz.applifting.humansis.misc.ViewModelFactory
import cz.applifting.humansis.ui.login.LoginViewModel
import cz.applifting.humansis.ui.main.MainViewModel
import cz.applifting.humansis.ui.main.SharedViewModel
import cz.applifting.humansis.ui.main.distribute.beneficiaries.BeneficiariesViewModel
import cz.applifting.humansis.ui.main.distribute.beneficiary.BeneficiaryViewModel
import cz.applifting.humansis.ui.main.distribute.beneficiary.confirm.ConfirmBeneficiaryViewModel
import cz.applifting.humansis.ui.main.distribute.assistances.AssistancesViewModel
import cz.applifting.humansis.ui.main.distribute.projects.ProjectsViewModel
import cz.applifting.humansis.ui.main.distribute.upload.UploadDialogViewModel
import cz.applifting.humansis.ui.main.settings.SettingsViewModel
import cz.applifting.humansis.ui.splash.SplashViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import kotlinx.coroutines.FlowPreview

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */

@Module
abstract class ViewModelModule {

    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @FlowPreview
    @Binds
    @IntoMap
    @ViewModelKey(ProjectsViewModel::class)
    internal abstract fun bindProjectsViewModel(viewModel: ProjectsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AssistancesViewModel::class)
    internal abstract fun bindAssistancesViewModel(viewModel: AssistancesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    internal abstract fun bindLoginViewModel(viewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    internal abstract fun bindMainActivityViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BeneficiariesViewModel::class)
    internal abstract fun bindBeneficiariesViewModel(viewModel: BeneficiariesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BeneficiaryViewModel::class)
    internal abstract fun bindBeneficiaryViewModel(viewModel: BeneficiaryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConfirmBeneficiaryViewModel::class)
    internal abstract fun bindConfirmBeneficiaryViewModel(viewModel: ConfirmBeneficiaryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SharedViewModel::class)
    internal abstract fun bindSharedViewModel(viewModel: SharedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    internal abstract fun bindSplashViewModel(viewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    internal abstract fun bindSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UploadDialogViewModel::class)
    internal abstract fun bindUploadDialogViewModel(viewModel: UploadDialogViewModel): ViewModel

    // Add more ViewModels here
}
