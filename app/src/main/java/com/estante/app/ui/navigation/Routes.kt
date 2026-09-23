package com.estante.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Rotas do Navigation 3. Cada tela é uma chave serializável: assim a pilha (back stack)
 * sobrevive à rotação e à recriação do processo. Os argumentos são propriedades da rota.
 */
@Serializable
data object SearchRoute : NavKey

@Serializable
data object ShelfRoute : NavKey

@Serializable
data class BookDetailRoute(val bookId: String) : NavKey

@Serializable
data class EntryEditorRoute(val bookId: String) : NavKey

/** Destinos da barra de navegação principal. */
val TopLevelRoutes: List<NavKey> = listOf(SearchRoute, ShelfRoute)
