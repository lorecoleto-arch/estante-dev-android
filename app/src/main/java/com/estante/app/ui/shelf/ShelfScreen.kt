package com.estante.app.ui.shelf

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.FilterListOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estante.app.R
import com.estante.app.domain.ReadingStatus
import com.estante.app.domain.ShelfEntry
import com.estante.app.domain.ShelfItem
import com.estante.app.ui.common.BookListItem
import com.estante.app.ui.common.EmptyState
import com.estante.app.ui.common.ErrorKind
import com.estante.app.ui.common.ErrorState
import com.estante.app.ui.common.LoadingState
import com.estante.app.ui.common.MaxContentWidth
import com.estante.app.ui.common.labelRes

@Composable
fun ShelfScreen(
    viewModel: ShelfViewModel,
    onBookClick: (String) -> Unit,
    onGoToSearch: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShelfScreen(
        state = state,
        onFilterChange = viewModel::onFilterChange,
        onBookClick = onBookClick,
        onGoToSearch = onGoToSearch,
        onRetry = viewModel::onRetry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(
    state: ShelfUiState,
    onFilterChange: (ReadingStatus?) -> Unit,
    onBookClick: (String) -> Unit,
    onGoToSearch: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.shelf_title)) }) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when (state) {
                ShelfUiState.Loading -> LoadingState(stringResource(R.string.shelf_loading))
                ShelfUiState.Error -> ErrorState(kind = ErrorKind.UNKNOWN, onRetry = onRetry)
                is ShelfUiState.Loaded -> Column(
                    Modifier.widthIn(max = MaxContentWidth).fillMaxSize(),
                ) {
                    if (state.total > 0) {
                        FilterRow(state, onFilterChange)
                    }
                    ShelfContent(state, onBookClick, onGoToSearch, onFilterChange)
                }
            }
        }
    }
}

@Composable
private fun FilterRow(state: ShelfUiState.Loaded, onFilterChange: (ReadingStatus?) -> Unit) {
    // LazyRow: os filtros rolam na horizontal se não couberem (fonte grande, tela estreita).
    // (O FilterChip já garante área de toque mínima de 48dp.)
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = state.filter == null,
                onClick = { onFilterChange(null) },
                label = { Text(stringResource(R.string.shelf_filter_all, state.total)) },
            )
        }
        items(ReadingStatus.entries, key = { it.name }) { status ->
            FilterChip(
                selected = state.filter == status,
                onClick = { onFilterChange(if (state.filter == status) null else status) },
                label = {
                    Text(
                        stringResource(
                            R.string.shelf_filter_status,
                            stringResource(status.labelRes),
                            state.counts[status] ?: 0,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun ShelfContent(
    state: ShelfUiState.Loaded,
    onBookClick: (String) -> Unit,
    onGoToSearch: () -> Unit,
    onFilterChange: (ReadingStatus?) -> Unit,
) {
    AnimatedContent(
        targetState = state.items.isEmpty(),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "shelf-content",
    ) { isEmpty ->
        when {
            !isEmpty -> ShelfList(state.items, onBookClick)
            state.total == 0 -> EmptyState(
                icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                title = stringResource(R.string.shelf_empty_title),
                message = stringResource(R.string.shelf_empty_message),
                actionLabel = stringResource(R.string.action_go_to_search),
                onAction = onGoToSearch,
            )
            else -> EmptyState(
                icon = Icons.Outlined.FilterListOff,
                title = stringResource(R.string.shelf_empty_filter_title),
                message = stringResource(
                    R.string.shelf_empty_filter_message,
                    state.filter?.let { stringResource(it.labelRes) }.orEmpty(),
                ),
                actionLabel = stringResource(R.string.action_show_all),
                onAction = { onFilterChange(null) },
            )
        }
    }
}

@Composable
private fun ShelfList(items: List<ShelfItem>, onBookClick: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxWidth()) {
        items(items, key = { it.book.id }) { item ->
            Column(Modifier.animateItem()) {
                BookListItem(
                    book = item.book,
                    status = item.entry.status,
                    onClick = { onBookClick(item.book.id) },
                    supportingText = item.entry.progressText(item.book.pageCount),
                )
                HorizontalDivider(Modifier.padding(start = 84.dp))
            }
        }
    }
}

@Composable
private fun ShelfEntry.progressText(totalPages: Int?): String? {
    val pages = pagesRead ?: return null
    if (status != ReadingStatus.READING) return null
    return if (totalPages != null) {
        stringResource(R.string.progress_pages, pages, totalPages)
    } else {
        stringResource(R.string.progress_pages_no_total, pages)
    }
}
