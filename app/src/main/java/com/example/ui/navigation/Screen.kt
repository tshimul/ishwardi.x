package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Categories : Screen("categories")
    object Favorites : Screen("favorites")
    object About : Screen("about")
    object AdminPortal : Screen("admin_portal")
    object ProviderDetail : Screen("provider_detail/{providerId}") {
        fun createRoute(providerId: String) = "provider_detail/$providerId"
    }
    object CategoryProviders : Screen("category_providers/{categoryName}") {
        fun createRoute(categoryName: String) = "category_providers/$categoryName"
    }
}
