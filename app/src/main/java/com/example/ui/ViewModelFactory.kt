package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.CaptainRepository
import com.example.ui.login.LoginViewModel
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.tables.TablesViewModel
import com.example.ui.menu.MenuViewModel
import com.example.ui.cart.CartViewModel
import com.example.ui.kot.KotViewModel
import com.example.ui.billing.BillingViewModel
import com.example.ui.settlement.SettlementViewModel
import com.example.ui.history.HistoryViewModel
import com.example.ui.profile.ProfileViewModel
import com.example.ui.splash.SplashViewModel

class ViewModelFactory(
    private val repository: CaptainRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> SplashViewModel(repository) as T
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(repository) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(TablesViewModel::class.java) -> TablesViewModel(repository) as T
            modelClass.isAssignableFrom(MenuViewModel::class.java) -> MenuViewModel(repository) as T
            modelClass.isAssignableFrom(CartViewModel::class.java) -> CartViewModel(repository) as T
            modelClass.isAssignableFrom(KotViewModel::class.java) -> KotViewModel(repository) as T
            modelClass.isAssignableFrom(BillingViewModel::class.java) -> BillingViewModel(repository) as T
            modelClass.isAssignableFrom(SettlementViewModel::class.java) -> SettlementViewModel(repository) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(repository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
