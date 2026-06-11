package org.dietai.project.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object Auth : Screen()

    @Serializable
    data object Main : Screen()

    @Serializable
    data object DietitianHome : Screen()

    @Serializable
    data object Splash : Screen()

    @Serializable
    data object Onboarding : Screen()

    @Serializable
    data object ForgotPassword : Screen()

    @Serializable
    data object Chat : Screen()



    @Serializable
    data class ClientDetail(val userId: String) : Screen()

    @Serializable
    data class DirectMessage(val chatId: String, val otherUserName: String) : Screen()
}

sealed class MainTab {
    @Serializable
    data object Home : MainTab()

    @Serializable
    data object Profile : MainTab()

    @Serializable
    data object MealLog : MainTab()

    @Serializable
    data object ChatTab : MainTab()

    @Serializable
    data object ExerciseLog : MainTab()
    @Serializable
    data object Clock : MainTab()
}
