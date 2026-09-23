package com.estante.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.estante.app.R
import com.estante.app.domain.Book
import com.estante.app.domain.ReadingStatus

/** Largura máxima do conteúdo em telas grandes (tablet, paisagem), para o texto não esticar demais. */
val MaxContentWidth = 720.dp

/** Capa do livro vinda da internet, com um marcador quando não há imagem. */
@Composable
fun BookCover(
    book: Book,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    val sizeModifier = modifier
        .width(width)
        .height(width * 1.5f)
        .clip(shape)

    val placeholder: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }

    val url = book.coverUrl
    if (url == null) {
        val noCover = stringResource(R.string.cd_no_cover)
        Box(sizeModifier.semantics { contentDescription = noCover }) { placeholder() }
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = stringResource(R.string.cd_cover, book.title),
            contentScale = ContentScale.Crop,
            modifier = sizeModifier,
            loading = { placeholder() },
            error = { placeholder() },
        )
    }
}

/** Item de lista reutilizado na busca e na estante. */
@Composable
fun BookListItem(
    book: Book,
    status: ReadingStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    val authors = book.authors.joinToString().ifEmpty { stringResource(R.string.unknown_author) }
    val year = book.firstPublishYear?.toString() ?: stringResource(R.string.unknown_year)
    val statusLabel = status?.let { stringResource(it.labelRes) }.orEmpty()
    val description = stringResource(R.string.book_item_description, book.title, authors, statusLabel)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            // Leitor de tela lê o item inteiro numa frase só, em vez de pedaço por pedaço.
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book = book, width = 52.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = authors,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = supportingText ?: year,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (status != null) {
            Spacer(Modifier.width(8.dp))
            StatusBadge(status)
        }
    }
}

@Composable
fun StatusBadge(status: ReadingStatus, modifier: Modifier = Modifier) {
    val (container, content) = when (status) {
        ReadingStatus.WANT_TO_READ ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ReadingStatus.READING ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        ReadingStatus.FINISHED ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text = stringResource(status.labelRes),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Estrelas de 1 a 5. Sem [onRatingChange] vira só leitura. Cada estrela tem 48dp de área de toque. */
@Composable
fun RatingBar(
    rating: Int?,
    modifier: Modifier = Modifier,
    onRatingChange: ((Int) -> Unit)? = null,
    starSize: Dp = 32.dp,
) {
    val current = rating ?: 0
    if (onRatingChange == null) {
        val description = rating?.let { stringResource(R.string.cd_rating_value, it) }
            ?: stringResource(R.string.cd_rating_empty)
        Row(modifier.clearAndSetSemantics { contentDescription = description }) {
            for (star in 1..5) {
                Icon(
                    imageVector = if (star <= current) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(starSize * 0.6f),
                )
            }
        }
        return
    }

    Row(modifier) {
        for (star in 1..5) {
            val selected = star == current
            val label = stringResource(R.string.cd_rate, star)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .selectable(selected = selected, role = Role.RadioButton) { onRatingChange(star) }
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (star <= current) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(starSize),
                )
            }
        }
    }
}

// --- Estados de tela reutilizáveis: carregando, vazio e erro ---

@Composable
fun LoadingState(message: String, modifier: Modifier = Modifier) {
    CenteredColumn(modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    CenteredColumn(modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorState(
    kind: ErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.semantics { liveRegion = LiveRegionMode.Assertive }) {
        EmptyState(
            icon = Icons.Filled.CloudOff,
            title = stringResource(R.string.error_title),
            message = stringResource(kind.messageRes),
            actionLabel = stringResource(R.string.action_retry),
            onAction = onRetry,
        )
    }
}

@Composable
fun StaleDataBanner(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CloudOff, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.search_stale_banner), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun pagesText(pageCount: Int?): String =
    pageCount?.let { pluralStringResource(R.plurals.pages_count, it, it) }
        ?: stringResource(R.string.unknown_pages)

@Composable
private fun CenteredColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            // Rolável: com fonte muito grande o conteúdo não é cortado.
            modifier = Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { content() }
    }
}
