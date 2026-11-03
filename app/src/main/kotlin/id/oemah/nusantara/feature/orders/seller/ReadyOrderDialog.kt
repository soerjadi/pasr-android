package id.oemah.nusantara.feature.orders.seller

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.StoreOrder

@Composable
fun ReadyOrderDialog(
    order: StoreOrder,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf(order.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.seller_ready_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(stringResource(R.string.seller_ready_note_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(note.trim().takeIf { it.isNotEmpty() }) },
            ) {
                Text(stringResource(R.string.seller_ready_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_cancel))
            }
        },
    )
}
