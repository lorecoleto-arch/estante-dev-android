package com.estante.app.ui.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estante.app.data.ShelfRepository
import com.estante.app.data.preferences.UserPreferencesRepository
import com.estante.app.domain.ReadingStatus
import com.estante.app.domain.ShelfItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ShelfUiState {
    data object Loading : ShelfUiState
    data object Error : ShelfUiState
    data class Loaded(
        val filter: ReadingStatus?,
        val counts: Map<ReadingStatus, Int>,
        val total: Int,
        /** Já filtrados. Lista vazia = estado "vazio". */
        val items: List<ShelfItem>,
    ) : ShelfUiState
}

class ShelfViewModel(
    shelfRepository: ShelfRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    /** Room + DataStore são reativos: salvar um livro em outra tela atualiza esta lista sozinho. */
    val uiState: StateFlow<ShelfUiState> =
        combine(shelfRepository.observeShelf(), preferences.shelfFilter) { items, filter ->
            ShelfUiState.Loaded(
                filter = filter,
                counts = items.groupingBy { it.entry.status }.eachCount(),
                total = items.size,
                items = if (filter == null) items else items.filter { it.entry.status == filter },
            ) as ShelfUiState
        }
            .catch { emit(ShelfUiState.Error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShelfUiState.Loading)

    fun onFilterChange(status: ReadingStatus?) {
        viewModelScope.launch { preferences.setShelfFilter(status) }
    }
}
