package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    val selectedGrade: StateFlow<Int> = userPreferencesManager.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    val userName: StateFlow<String> = userPreferencesManager.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Học sinh THCS")

    val isDarkMode: StateFlow<Boolean> = userPreferencesManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<String> = userPreferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val isFirstLaunch: StateFlow<Boolean> = userPreferencesManager.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val streakDays: StateFlow<Int> = userPreferencesManager.streakDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val reduceMotion: StateFlow<Boolean> = userPreferencesManager.reduceMotion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setGrade(grade: Int) {
        viewModelScope.launch {
            userPreferencesManager.setSelectedGrade(grade)
            userPreferencesManager.setFirstLaunchCompleted()
        }
    }

    fun completeFirstLaunch() {
        viewModelScope.launch {
            userPreferencesManager.setFirstLaunchCompleted()
        }
    }
}
