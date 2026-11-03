package id.oemah.nusantara.feature.orders.seller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.StoreOrder

@Composable
fun RejectOrderDialog(
    order: StoreOrder,
    onConfirm: (RejectRequest.Reason, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedReason by remember { mutableStateOf(RejectRequest.Reason.OUT_OF_STOCK) }
    var note by remember { mutableStateOf("") }

    val reasons = remember {
        listOf(
            RejectRequest.Reason.OUT_OF_STOCK to R.string.seller_reject_reason_out_of_stock,
            RejectRequest.Reason.BUSY_OR_CLOSED to R.string.seller_reject_reason_busy,
            RejectRequest.Reason.CANNOT_SERVE to R.string.seller_reject_reason_cannot_serve,
            RejectRequest.Reason.OTHER to R.string.seller_reject_reason_other,
        )
    }

    val canConfirm = selectedReason != RejectRequest.Reason.OTHER || note.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.seller_reject_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                reasons.forEach { (reason, labelRes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (reason == selectedReason),
                                onClick = { selectedReason = reason },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (reason == selectedReason),
                            onClick = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (selectedReason == RejectRequest.Reason.OTHER) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text(stringResource(R.string.seller_reject_note_hint)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        maxLines = 3,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedReason, note.trim().takeIf { it.isNotEmpty() }) },
                enabled = canConfirm,
            ) {
                Text(stringResource(R.string.seller_reject_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_cancel))
            }
        },
    )
}
