package com.estante.app.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estante.app.R
import com.estante.app.domain.Book
import com.estante.app.domain.ReadingStatus
import com.estante.app.ui.common.BookListItem
import com.estante.app.ui.common.EmptyState
import com.estante.app.ui.common.ErrorState
import com.estante.app.ui.common.LoadingState
import com.estante.app.ui.common.MaxContentWidth
import com.estante.app.ui.common.StaleDataBanner
import kotlinx.coroutines.Dispatchers

/** Versão "com ViewModel": só coleta o estado e repassa os eventos. */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBookClick: (String) -> Unit,
) {
    // collectAsStateWithLifecycle para de coletar quando o app vai para segundo plano.
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Main.immediate: o campo de texto recebe o valor na mesma hora em que o usuário digita.
    val query by viewModel.query.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    SearchScreen(
        query = query,
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onTopicClick = viewModel::onTopicSelected,
        onClear = viewModel::onClearQuery,
        onRetry = viewModel::onRetry,
        onBookClick = onBookClick,
    )
}

/** Versão "sem estado": recebe tudo por parâmetro — fácil de testar e de pré-visualizar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onBookClick: (String) -> Unit,
    onTopicClick: (String) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                actions = {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_label)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cd_clear_search))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    onSearch()
                }),
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            TopicChips(
                currentQuery = query,
                onTopicClick = { topic ->
                    focusManager.clearFocus()
                    onTopicClick(topic)
                },
            )

            // Animação implícita: troca suave entre carregando / conteúdo / vazio / erro.
            AnimatedContent(
                targetState = state.results,
                contentKey = { it::class },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "search-results",
                modifier = Modifier.weight(1f),
            ) { results ->
                when (results) {
                    SearchResultsState.Idle -> EmptyState(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = stringResource(R.string.search_idle_title),
                        message = stringResource(R.string.search_idle_message),
                    )
                    SearchResultsState.Loading -> LoadingState(stringResource(R.string.search_loading))
                    is SearchResultsState.Empty -> EmptyState(
                        icon = Icons.Filled.SearchOff,
                        title = stringResource(R.string.search_empty_title),
                        message = stringResource(R.string.search_empty_message, results.query),
                    )
                    is SearchResultsState.Error -> ErrorState(kind = results.kind, onRetry = onRetry)
                    is SearchResultsState.Content -> BookResultsList(
                        books = results.books,
                        isStale = results.isStale,
                        shelfStatus = state.shelfStatus,
                        onBookClick = onBookClick,
                    )
                }
            }
        }
    }
}

/** Temas de programação sugeridos: (texto do botão, termo buscado na API). */
private val SearchTopics = listOf(
    "Clean Code" to "clean code",
    "Algoritmos" to "algoritmos",
    "Lógica de programação" to "logica de programacao",
    "Design Patterns" to "design patterns",
    "Engenharia de software" to "engenharia de software",
    "Kotlin" to "kotlin",
    "Arquitetura" to "software architecture",
)

@Composable
private fun TopicChips(currentQuery: String, onTopicClick: (String) -> Unit) {
    // LazyRow: os temas rolam na horizontal. O FilterChip já tem área de toque de 48dp.
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.widthIn(max = MaxContentWidth),
    ) {
        items(SearchTopics, key = { it.second }) { (label, query) ->
            val description = stringResource(R.string.cd_topic, label)
            FilterChip(
                selected = currentQuery.trim().equals(query, ignoreCase = true),
                onClick = { onTopicClick(query) },
                label = { Text(label) },
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

@Composable
private fun BookResultsList(
    books: List<Book>,
    isStale: Boolean,
    shelfStatus: Map<String, ReadingStatus>,
    onBookClick: (String) -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
            // Fora da lista: assim o aviso aparece sempre, mesmo com a lista rolada.
            AnimatedVisibility(visible = isStale) { StaleDataBanner() }
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "count") {
                    Text(
                        text = pluralStringResource(R.plurals.search_results_count, books.size, books.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                items(books, key = { it.id }) { book ->
                    Column(Modifier.animateItem()) {
                        BookListItem(
                            book = book,
                            status = shelfStatus[book.id],
                            onClick = { onBookClick(book.id) },
                        )
                        HorizontalDivider(Modifier.padding(start = 84.dp))
                    }
                }
            }
        }
    }
}
