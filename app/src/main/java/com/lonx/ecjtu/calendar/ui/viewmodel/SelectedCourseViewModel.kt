package com.lonx.ecjtu.calendar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lonx.ecjtu.calendar.data.datasource.local.LocalDataSource
import com.lonx.ecjtu.calendar.domain.usecase.course.GetSelectedCoursesUseCase
import com.lonx.ecjtu.calendar.domain.usecase.settings.GetUserConfigUseCase
import com.lonx.ecjtu.calendar.ui.screen.course.SelectedCourseUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SelectedCourseViewModel(
    private val getSelectedCoursesUseCase: GetSelectedCoursesUseCase,
    private val getUserConfigUseCase: GetUserConfigUseCase,
    private val localDataSource: LocalDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(SelectedCourseUiState())
    val uiState: StateFlow<SelectedCourseUiState> = _uiState.asStateFlow()

    private var currentWeiXinID: String? = null

    init {
        observeUserConfig()
        observeTermRefresh()
    }

    private fun observeTermRefresh() {
        viewModelScope.launch {
            uiState.map { it.currentTerm }
                .distinctUntilChanged()
                .collectLatest { term ->
                    if (term.isNotBlank()) {
                        localDataSource.getSelectedCourseLastRefresh(term).collect { ts ->
                            _uiState.update { it.copy(lastRefreshMillis = ts) }
                        }
                    } else {
                        _uiState.update { it.copy(lastRefreshMillis = 0L) }
                    }
                }
        }
    }

    private fun observeUserConfig() {
        viewModelScope.launch {
            getUserConfigUseCase().distinctUntilChanged().collect { newWeiXinID ->
                currentWeiXinID = newWeiXinID
                if (newWeiXinID.isNotBlank()) {
                    loadCourses()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = "用户未配置，请先在设置中绑定账号"
                        )
                    }
                }
            }
        }
    }

    fun loadCourses(term: String? = null, refresh: Boolean = false, showToast: Boolean = false) {

        viewModelScope.launch {
            val hasExistingCourses = _uiState.value.courses.isNotEmpty()
            _uiState.update {
                if (refresh && hasExistingCourses) {
                    it.copy(isLoading = false, isRefreshing = true, error = null)
                } else {
                    it.copy(isLoading = true, isRefreshing = false, error = null)
                }
            }

            val result = if (refresh) {
                getSelectedCoursesUseCase(term)
            } else {
                getSelectedCoursesUseCase.getFromLocal(term)
            }

            result.onSuccess { coursePage ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        courses = coursePage.courses,
                        availableTerms = coursePage.availableTerms,
                        currentTerm = coursePage.currentTerm,
                        toastMessage = if (refresh || showToast) "找到了 ${coursePage.courses.size} 门课程" else null
                    )
                }
            }.onFailure { exception ->
                val hasExistingCourses = _uiState.value.courses.isNotEmpty()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (hasExistingCourses) null else (exception.message ?: "加载失败，请重试"),
                        toastMessage = if (hasExistingCourses) (exception.message ?: "刷新失败，请重试") else it.toastMessage
                    )
                }
            }
        }
    }

    fun onTermSelected(newTerm: String) {
        loadCourses(term = newTerm, showToast = true)
    }

    fun onToastShown() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}