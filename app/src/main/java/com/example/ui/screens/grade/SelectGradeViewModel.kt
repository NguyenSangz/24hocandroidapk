package com.example.ui.screens.grade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Trạng thái giao diện màn hình chọn lớp
 */
data class SelectGradeUiState(
    val selectedGrade: Int = 6,
    val isSaving: Boolean = false,
    val hasConfirmed: Boolean = false
)

/**
 * ViewModel cho màn hình Chọn lớp (6, 7, 8, 9):
 * - Theo dõi và cập nhật lựa chọn khối lớp
 * - Lưu lựa chọn vào DataStore thông qua SettingsRepository
 */
class SelectGradeViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectGradeUiState())
    val uiState: StateFlow<SelectGradeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val initialGrade = settingsRepository.selectedGrade.first()
            _uiState.value = _uiState.value.copy(selectedGrade = initialGrade)
        }
    }

    fun onGradeSelected(grade: Int) {
        if (grade in 6..9) {
            _uiState.value = _uiState.value.copy(selectedGrade = grade)
        }
    }

    fun confirmGradeSelection(onSuccess: () -> Unit) {
        val currentGrade = _uiState.value.selectedGrade
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            settingsRepository.setGrade(currentGrade)
            settingsRepository.setFirstLaunchCompleted()
            _uiState.value = _uiState.value.copy(isSaving = false, hasConfirmed = true)
            onSuccess()
        }
    }
}
