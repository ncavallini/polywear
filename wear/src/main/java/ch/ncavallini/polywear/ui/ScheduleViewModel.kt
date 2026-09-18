package ch.ncavallini.polywear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.ncavallini.polywear.auth.TokenStore
import ch.ncavallini.polywear.data.ScheduleRepository
import ch.ncavallini.polywear.data.ScheduleResult
import ch.ncavallini.polywear.data.model.DaySchedule
import ch.ncavallini.polywear.di.AppContainer
import ch.ncavallini.polywear.util.SemesterUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

sealed interface ScheduleUiState {
    data object Loading : ScheduleUiState
    data object NeedsAuth : ScheduleUiState
    data class Content(
        val days: List<DaySchedule>,
        val stale: Boolean,
        val weekLabel: String,
    ) : ScheduleUiState
    data class Error(val message: String) : ScheduleUiState
}

class ScheduleViewModel(
    private val repository: ScheduleRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {

    private val _state = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    private val zone = ZoneId.of("Europe/Zurich")

    /** Any date within the week currently shown. Shifted by week navigation. */
    private var refDate: LocalDate = LocalDate.now(zone)

    /** `semkez` last successfully fetched from the network this session, if any. */
    private var freshSemkez: String? = null

    init {
        // Drives the initial load and re-loads whenever the phone delivers (or
        // clears) a credential.
        viewModelScope.launch {
            tokenStore.credential.collect { credential ->
                if (credential == null || credential.isExpired) {
                    _state.value = ScheduleUiState.NeedsAuth
                } else {
                    load()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    fun nextWeek() = navigate(1)

    fun previousWeek() = navigate(-1)

    /**
     * Moves [deltaWeeks] weeks from the current view. Weeks within a semester
     * already fetched this session are regrouped from the cache instantly; moving
     * into a not-yet-loaded semester triggers a network load.
     */
    private fun navigate(deltaWeeks: Long) {
        refDate = refDate.plusWeeks(deltaWeeks)
        viewModelScope.launch {
            if (SemesterUtil.semkezFor(refDate) == freshSemkez) {
                val days = repository.cachedWeek(refDate).orEmpty()
                _state.value = ScheduleUiState.Content(days, stale = false, weekLabel(stale = false))
            } else {
                load()
            }
        }
    }

    private suspend fun load() {
        if (tokenStore.current()?.takeUnless { it.isExpired } == null) {
            _state.value = ScheduleUiState.NeedsAuth
            return
        }
        _state.value = ScheduleUiState.Loading
        _state.value = when (val result = repository.getWeek(refDate)) {
            is ScheduleResult.Success -> {
                freshSemkez = SemesterUtil.semkezFor(refDate)
                ScheduleUiState.Content(result.days, stale = result.fromCache, weekLabel(result.fromCache))
            }

            ScheduleResult.Unauthorized -> {
                tokenStore.clear()
                ScheduleUiState.NeedsAuth
            }

            is ScheduleResult.Error ->
                if (!result.cachedDays.isNullOrEmpty()) {
                    ScheduleUiState.Content(result.cachedDays, stale = true, weekLabel(stale = true))
                } else {
                    ScheduleUiState.Error(result.throwable.message ?: "Something went wrong")
                }
        }
    }

    /** Caption for the currently viewed week, relative to today. */
    private fun weekLabel(stale: Boolean): String {
        val thisWeek = SemesterUtil.weekStart(LocalDate.now(zone))
        val viewed = SemesterUtil.weekStart(refDate)
        val base = when (ChronoUnit.WEEKS.between(thisWeek, viewed)) {
            0L -> "This week"
            1L -> "Next week"
            -1L -> "Last week"
            else -> "${viewed.format(weekLabelFormat)} – ${viewed.plusDays(6).format(weekLabelFormat)}"
        }
        return if (stale) "$base · offline" else base
    }

    companion object {
        private val weekLabelFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ScheduleViewModel(container.scheduleRepository, container.tokenStore) }
        }
    }
}
