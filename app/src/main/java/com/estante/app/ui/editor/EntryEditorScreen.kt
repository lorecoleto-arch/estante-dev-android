package com.estante.app.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estante.app.R
import com.estante.app.domain.FieldError
import com.estante.app.domain.ReadingStatus
import com.estante.app.domain.ShelfEntryValidator
import com.estante.app.ui.common.EmptyState
import com.estante.app.ui.common.ErrorKind
import com.estante.app.ui.common.ErrorState
import com.estante.app.ui.common.LoadingState
import com.estante.app.ui.common.MaxContentWidth
import com.estante.app.ui.common.RatingBar
import com.estante.app.ui.common.labelRes
import kotlinx.coroutines.Dispatchers

@Composable
fun EntryEditorScreen(
    viewModel: EntryEditorViewModel,
    onClose: () -> Unit,
    onFinished: (EditorResult) -> Unit,
) {
    // Main.immediate: os campos de texto recebem o valor na mesma hora em que o usuário digita.
    val state by viewModel.uiState.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)

    // Quando o ViewModel informa que salvou/removeu, a tela fecha e o app confirma o sucesso.
    LaunchedEffect(state.result) {
        state.result?.let(onFinished)
    }

    EntryEditorScreen(
        state = state,
        onClose = onClose,
        onStatusChange = viewModel::onStatusChange,
        onPagesChange = viewModel::onPagesChange,
        onRatingChange = viewModel::onRatingChange,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::onSave,
        onRemove = viewModel::onRemove,
        onRetryLoad = viewModel::onRetryLoad,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorScreen(
    state: EntryEditorUiState,
    onClose: () -> Unit,
    onStatusChange: (ReadingStatus) -> Unit,
    onPagesChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onRetryLoad: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isExistingEntry) R.string.editor_title_edit else R.string.editor_title_new,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(stringResource(R.string.editor_loading), Modifier.padding(padding))
            state.loadFailed -> ErrorState(ErrorKind.UNKNOWN, onRetryLoad, Modifier.padding(padding))
            state.bookNotFound -> EmptyState(
                icon = Icons.Filled.ErrorOutline,
                title = stringResource(R.string.error_title),
                message = stringResource(R.string.detail_not_found),
                modifier = Modifier.padding(padding),
            )
            else -> Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter,
            ) {
                EditorForm(state, onStatusChange, onPagesChange, onRatingChange, onNoteChange, onSave, onRemove)
            }
        }
    }
}

@Composable
private fun EditorForm(
    state: EntryEditorUiState,
    onStatusChange: (ReadingStatus) -> Unit,
    onPagesChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    val form = state.form
    val errors = state.validation.takeIf { state.showErrors }

    Column(
        modifier = Modifier
            .widthIn(max = MaxContentWidth)
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = state.bookTitle,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        // Resumo do erro no topo do formulário (e anunciado pelo leitor de tela).
        val summary = when {
            state.saveFailed -> stringResource(R.string.editor_save_failed)
            state.showErrors && !state.validation.isValid -> stringResource(R.string.editor_fix_errors)
            else -> null
        }
        AnimatedVisibility(visible = summary != null) {
            FormErrorSummary(summary.orEmpty())
        }

        // --- Situação da leitura ---
        Text(stringResource(R.string.editor_status), style = MaterialTheme.typography.titleMedium)
        val statuses = ReadingStatus.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            statuses.forEachIndexed { index, status ->
                SegmentedButton(
                    selected = form.status == status,
                    onClick = { onStatusChange(status) },
                    shape = SegmentedButtonDefaults.itemShape(index, statuses.size),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(status.labelRes))
                }
            }
        }

        // --- Páginas lidas (só aparece para "Lendo") ---
        AnimatedVisibility(visible = form.status == ReadingStatus.READING) {
            val pagesError = errors?.pagesError?.let { errorMessage(it, state.totalPages) }
            OutlinedTextField(
                value = form.pagesRead,
                onValueChange = onPagesChange,
                label = { Text(stringResource(R.string.editor_pages_label)) },
                singleLine = true,
                isError = pagesError != null,
                supportingText = {
                    Text(
                        pagesError ?: state.totalPages?.let { stringResource(R.string.editor_pages_of_total, it) }.orEmpty(),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { if (pagesError != null) error(pagesError) },
            )
        }

        // --- Nota (só aparece para "Lido") ---
        AnimatedVisibility(visible = form.status == ReadingStatus.FINISHED) {
            val ratingError = errors?.ratingError?.let { errorMessage(it, state.totalPages) }
            Column {
                Text(stringResource(R.string.editor_rating), style = MaterialTheme.typography.titleMedium)
                RatingBar(
                    rating = form.rating,
                    onRatingChange = onRatingChange,
                    modifier = Modifier.semantics { if (ratingError != null) error(ratingError) },
                )
                if (ratingError != null) {
                    Text(ratingError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // --- Anotação ---
        val noteError = errors?.noteError?.let { errorMessage(it, state.totalPages) }
        val noteLength = form.note.trim().length
        OutlinedTextField(
            value = form.note,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.editor_note_label)) },
            minLines = 3,
            isError = noteError != null || noteLength > ShelfEntryValidator.NOTE_MAX_LENGTH,
            supportingText = {
                Text(
                    noteError ?: stringResource(
                        R.string.editor_note_counter,
                        noteLength,
                        ShelfEntryValidator.NOTE_MAX_LENGTH,
                    ),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (noteError != null) error(noteError) },
        )

        Button(
            onClick = onSave,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.action_save))
            }
        }
        if (state.isExistingEntry) {
            TextButton(
                onClick = onRemove,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.action_remove_from_shelf), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun FormErrorSummary(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun errorMessage(error: FieldError, totalPages: Int?): String = when (error) {
    FieldError.PAGES_REQUIRED -> stringResource(R.string.error_pages_required)
    FieldError.PAGES_NOT_A_NUMBER -> stringResource(R.string.error_pages_not_a_number)
    FieldError.PAGES_OUT_OF_RANGE ->
        if (totalPages != null) {
            stringResource(R.string.error_pages_out_of_range, totalPages)
        } else {
            stringResource(R.string.error_pages_negative)
        }
    FieldError.RATING_REQUIRED -> stringResource(R.string.error_rating_required)
    FieldError.NOTE_TOO_LONG -> stringResource(R.string.error_note_too_long, ShelfEntryValidator.NOTE_MAX_LENGTH)
}
