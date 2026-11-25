package com.estante.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.estante.app.AppContainer
import com.estante.app.R
import com.estante.app.ui.detail.BookDetailScreen
import com.estante.app.ui.detail.BookDetailViewModel
import com.estante.app.ui.editor.EditorResult
import com.estante.app.ui.editor.EntryEditorScreen
import com.estante.app.ui.editor.EntryEditorViewModel
import com.estante.app.ui.navigation.BookDetailRoute
import com.estante.app.ui.navigation.EntryEditorRoute
import com.estante.app.ui.navigation.SearchRoute
import com.estante.app.ui.navigation.ShelfRoute
import com.estante.app.ui.search.SearchScreen
import com.estante.app.ui.search.SearchViewModel
import com.estante.app.ui.shelf.ShelfScreen
import com.estante.app.ui.shelf.ShelfViewModel
import kotlinx.coroutines.launch

@Composable
fun EstanteApp(container: AppContainer) {
    // A pilha de navegação é só uma lista de rotas: navegar = adicionar, voltar = remover.
    val backStack = rememberNavBackStack(SearchRoute)
    val currentTab = backStack.firstOrNull() ?: SearchRoute

    // Confirmação de sucesso: fica aqui na raiz porque a tela do formulário já fechou quando ela aparece.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.editor_saved)
    val removedMessage = stringResource(R.string.editor_removed)

    // Barra inferior no celular; trilho lateral em telas largas (tablet/paisagem).
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                selected = currentTab == SearchRoute,
                onClick = { backStack.selectTab(SearchRoute) },
                icon = {
                    Icon(if (currentTab == SearchRoute) Icons.Filled.Search else Icons.Outlined.Search, null)
                },
                label = { Text(stringResource(R.string.nav_search)) },
            )
            item(
                selected = currentTab == ShelfRoute,
                onClick = { backStack.selectTab(ShelfRoute) },
                icon = {
                    Icon(
                        if (currentTab == ShelfRoute) {
                            Icons.AutoMirrored.Filled.LibraryBooks
                        } else {
                            Icons.AutoMirrored.Outlined.LibraryBooks
                        },
                        null,
                    )
                },
                label = { Text(stringResource(R.string.nav_shelf)) },
            )
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    // Guarda o estado "rememberSaveable" de cada tela da pilha.
                    rememberSaveableStateHolderNavEntryDecorator(),
                    // Cada tela da pilha ganha seu próprio ViewModel, descartado quando ela sai da pilha.
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<SearchRoute> {
                        val vm = viewModel {
                            SearchViewModel(container.bookRepository, container.shelfRepository, container.preferences)
                        }
                        SearchScreen(
                            viewModel = vm,
                            onBookClick = { id -> backStack.add(BookDetailRoute(id)) },
                        )
                    }
                    entry<ShelfRoute> {
                        val vm = viewModel { ShelfViewModel(container.shelfRepository, container.preferences) }
                        ShelfScreen(
                            viewModel = vm,
                            onBookClick = { id -> backStack.add(BookDetailRoute(id)) },
                            onGoToSearch = { backStack.selectTab(SearchRoute) },
                        )
                    }
                    entry<BookDetailRoute> { route ->
                        val vm = viewModel {
                            BookDetailViewModel(route.bookId, container.bookRepository, container.shelfRepository)
                        }
                        BookDetailScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() },
                            onEditReading = { backStack.add(EntryEditorRoute(route.bookId)) },
                        )
                    }
                    entry<EntryEditorRoute> { route ->
                        val vm = viewModel {
                            EntryEditorViewModel(route.bookId, container.bookRepository, container.shelfRepository)
                        }
                        EntryEditorScreen(
                            viewModel = vm,
                            onClose = { backStack.removeLastOrNull() },
                            onFinished = { result ->
                                backStack.removeLastOrNull()
                                val message = when (result) {
                                    EditorResult.SAVED -> savedMessage
                                    EditorResult.REMOVED -> removedMessage
                                }
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            },
                        )
                    }
                },
            )
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Trocar de aba recomeça a pilha naquela aba (tocar de novo na aba atual volta ao topo dela). */
private fun NavBackStack<NavKey>.selectTab(tab: NavKey) {
    if (size == 1 && first() == tab) return
    clear()
    add(tab)
}
