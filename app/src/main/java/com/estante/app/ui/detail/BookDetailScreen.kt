package com.estante.app.ui.detail

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estante.app.R
import com.estante.app.domain.Book
import com.estante.app.domain.ReadingStatus
import com.estante.app.domain.ShelfEntry
import com.estante.app.ui.common.BookCover
import com.estante.app.ui.common.EmptyState
import com.estante.app.ui.common.ErrorKind
import com.estante.app.ui.common.ErrorState
import com.estante.app.ui.common.LoadingState
import com.estante.app.ui.common.MaxContentWidth
import com.estante.app.ui.common.RatingBar
import com.estante.app.ui.common.StatusBadge
import com.estante.app.ui.common.pagesText

@Composable
fun BookDetailScreen(
    viewModel: BookDetailViewModel,
    onBack: () -> Unit,
    onEditReading: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BookDetailScreen(
        state = state,
        onBack = onBack,
        onEditReading = onEditReading,
        onRetry = viewModel::onRetry,
        onRetryDescription = viewModel::onRetryDescription,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    state: BookDetailUiState,
    onBack: () -> Unit,
    onEditReading: () -> Unit,
    onRetry: () -> Unit,
    onRetryDescription: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (state is BookDetailUiState.Content) {
                        val book = state.book
                        val shareText = stringResource(
                            R.string.share_text,
                            book.title,
                            book.authors.firstOrNull() ?: stringResource(R.string.unknown_author),
                            book.shareUrl,
                        )
                        val chooserTitle = stringResource(R.string.share_chooser)
                        IconButton(onClick = {
                            // Integração com o celular: compartilhamento via Intent implícita.
                            val send = Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, shareText)
                            context.startActivity(Intent.createChooser(send, chooserTitle))
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                BookDetailUiState.Loading -> LoadingState(stringResource(R.string.detail_loading))
                BookDetailUiState.Error -> ErrorState(kind = ErrorKind.UNKNOWN, onRetry = onRetry)
                BookDetailUiState.NotFound -> EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.error_title),
                    message = stringResource(R.string.detail_not_found),
                )
                is BookDetailUiState.Content -> DetailContent(state, onEditReading, onRetryDescription)
            }
        }
    }
}

@Composable
private fun DetailContent(
    state: BookDetailUiState.Content,
    onEditReading: () -> Unit,
    onRetryDescription: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BookHeader(state.book)
            ReadingCard(state.book, state.entry, onEditReading)
            DescriptionSection(state.description, onRetryDescription)
            if (state.book.subjects.isNotEmpty()) {
                Section(title = stringResource(R.string.detail_subjects)) {
                    Text(state.book.subjects.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun BookHeader(book: Book) {
    Row {
        BookCover(book = book, width = 112.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = book.authors.joinToString().ifEmpty { stringResource(R.string.unknown_author) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = book.firstPublishYear?.toString() ?: stringResource(R.string.unknown_year),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(text = pagesText(book.pageCount), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ReadingCard(book: Book, entry: ShelfEntry?, onEditReading: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        // Animação implícita: o card cresce/encolhe suavemente quando o conteúdo muda.
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.detail_my_reading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (entry == null) {
                Text(stringResource(R.string.detail_not_on_shelf), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onEditReading, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_add_to_shelf))
                }
            } else {
                StatusBadge(entry.status)
                if (entry.status == ReadingStatus.READING && entry.pagesRead != null) {
                    ReadingProgress(pagesRead = entry.pagesRead, totalPages = book.pageCount)
                }
                if (entry.status == ReadingStatus.FINISHED) {
                    RatingBar(rating = entry.rating)
                }
                entry.note?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                OutlinedButton(onClick = onEditReading, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_edit_reading))
                }
            }
        }
    }
}

@Composable
fun ReadingProgress(pagesRead: Int, totalPages: Int?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (totalPages != null && totalPages > 0) {
            LinearProgressIndicator(
                progress = { (pagesRead.toFloat() / totalPages).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.progress_pages, pagesRead, totalPages), style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(stringResource(R.string.progress_pages_no_total, pagesRead), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DescriptionSection(description: DescriptionUi, onRetry: () -> Unit) {
    Section(title = stringResource(R.string.detail_description)) {
        AnimatedContent(targetState = description, contentKey = { it::class }, label = "description") { current ->
            when (current) {
                DescriptionUi.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.detail_description_loading))
                }
                is DescriptionUi.Loaded -> Text(
                    text = current.text ?: stringResource(R.string.detail_description_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
                DescriptionUi.Error -> Column {
                    Text(
                        text = stringResource(R.string.detail_description_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}
