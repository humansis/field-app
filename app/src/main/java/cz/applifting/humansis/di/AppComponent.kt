package cz.applifting.humansis.di

import android.content.Context
import cz.applifting.humansis.misc.Utilities
import cz.applifting.humansis.synchronization.SyncWorker
import cz.applifting.humansis.ui.BaseFragment
import cz.applifting.humansis.ui.HumansisActivity
import cz.applifting.humansis.ui.login.LoginFragment
import cz.applifting.humansis.ui.main.MainFragment
import cz.applifting.humansis.ui.main.distribute.beneficiary.BeneficiaryDialog
import cz.applifting.humansis.ui.main.distribute.beneficiary.confirm.AddReferralInfoDialog
import cz.applifting.humansis.ui.main.distribute.beneficiary.confirm.ConfirmBeneficiaryDialog
import cz.applifting.humansis.ui.main.distribute.upload.UploadDialog
import cz.applifting.humansis.ui.main.settings.SettingsFragment
import cz.applifting.humansis.ui.splash.SplashFragment
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton



/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */

@Singleton
@Component(modules = [AppModule::class, ViewModelModule::class, ParamsModule::class])
interface AppComponent {

    fun inject(humansisActivity: HumansisActivity)

    fun inject(baseFragment: BaseFragment)

    fun inject(settingsFragment: SettingsFragment)

    fun inject(loginFragment: LoginFragment)

    fun inject(mainFragment: MainFragment)

    fun inject(splashActivity: SplashFragment)

    fun inject(beneficiaryDialog: BeneficiaryDialog)

    fun inject(confirmBeneficiaryDialog: ConfirmBeneficiaryDialog)

    fun inject(addReferralInfoDialog: AddReferralInfoDialog)

    fun inject(uploadDialog: UploadDialog)

    fun inject(worker: SyncWorker)

    fun inject(utilities: Utilities)

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun context(context: Context): Builder

        fun build(): AppComponent
    }
}