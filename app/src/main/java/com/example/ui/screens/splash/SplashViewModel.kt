package com.example.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.seed.ContentLoader
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Trạng thái giao diện màn hình khởi động (Splash)
 */
sealed interface SplashUiState {
    data object Loading : SplashUiState
    data class Success(val isGradeSelected: Boolean, val selectedGrade: Int) : SplashUiState
    data class Error(val message: String) : SplashUiState
}

/**
 * ViewModel cho màn hình Splash:
 * - Gọi ContentLoader.loadIfEmpty() trên luồng nền
 * - Quản lý trạng thái nạp dữ liệu (Loading/Success/Error)
 * - Hỗ trợ nạp lại (retry) khi gặp sự cố
 * - Kiểm tra trạng thái đã chọn lớp học chưa để điều hướng phù hợp
 */
class SplashViewModel(
    private val contentLoader: ContentLoader,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _uiState.value = SplashUiState.Loading
        viewModelScope.launch {
            val result = contentLoader.loadIfEmpty()
            if (result.isSuccess) {
                val isGradeSelected = settingsRepository.isGradeSelected.first()
                val selectedGrade = settingsRepository.selectedGrade.first()
                _uiState.value = SplashUiState.Success(
                    isGradeSelected = isGradeSelected,
                    selectedGrade = selectedGrade
                )
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage
                    ?: "Không thể tải dữ liệu học tập. Vui lòng thử lại!"
                _uiState.value = SplashUiState.Error(errorMsg)
            }
        }
    }

    fun retryLoading() {
        loadData()
    }
}
