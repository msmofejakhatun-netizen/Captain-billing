package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Tables : Screen("tables")
    object OpenTable : Screen("open_table/{tableNumber}") {
        fun createRoute(tableNumber: String) = "open_table/$tableNumber"
    }
    object Menu : Screen("menu/{tableId}") {
        fun createRoute(tableId: String) = "menu/$tableId"
    }
    object Cart : Screen("cart/{tableId}") {
        fun createRoute(tableId: String) = "cart/$tableId"
    }
    object Kot : Screen("kot/{tableId}") {
        fun createRoute(tableId: String) = "kot/$tableId"
    }
    object Billing : Screen("billing/{tableId}") {
        fun createRoute(tableId: String) = "billing/$tableId"
    }
    object Settlement : Screen("settlement/{tableId}/{billId}/{grandTotal}") {
        fun createRoute(tableId: String, billId: String, grandTotal: Double) =
            "settlement/$tableId/$billId/$grandTotal"
    }
    object History : Screen("history")
    object Profile : Screen("profile")
    object Reports : Screen("reports")
}
