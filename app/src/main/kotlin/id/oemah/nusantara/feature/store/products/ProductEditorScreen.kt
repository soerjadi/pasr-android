package id.oemah.nusantara.feature.store.products

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import id.oemah.nusantara.R
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.ui.components.PrimaryButton
import id.oemah.nusantara.ui.components.ScreenHeader
import id.oemah.nusantara.ui.components.Skeleton
import kotlinx.coroutines.launch

@Composable
fun ProductEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        launch { viewModel.saved.collect { onSaved() } }
        launch { viewModel.deleted.collect { onDeleted() } }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes != null) {
                viewModel.uploadPhoto(bytes)
            }
        }
    }

    ProductEditorContent(
        state = state,
        onBack = onBack,
        onPickPhoto = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRemovePhoto = viewModel::removePhoto,
        onNameChange = viewModel::setName,
        onPriceChange = viewModel::setPrice,
        onStockChange = viewModel::setStock,
        onStepStock = viewModel::stepStock,
        onDescriptionChange = viewModel::setDescription,
        onStatusChange = viewModel::setStatus,
        onSubmit = viewModel::submit,
        onOpenDeleteDialog = viewModel::openDeleteDialog,
        onCloseDeleteDialog = viewModel::closeDeleteDialog,
        onConfirmDelete = viewModel::delete,
        onDismissError = viewModel::dismissError,
        modifier = modifier,
    )
}

@Composable
fun ProductEditorContent(
    state: ProductEditorState,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onStepStock: (Int) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onStatusChange: (SellerProduct.Status) -> Unit,
    onSubmit: () -> Unit,
    onOpenDeleteDialog: () -> Unit,
    onCloseDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        val error = state.error
        if (error != null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            launch {
                snackbarHostState.showSnackbar(error)
            }
            onDismissError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ScreenHeader(
                title = stringResource(
                    if (state.isEditMode) R.string.seller_product_form_edit_title
                    else R.string.seller_product_form_add_title
                ),
                onBack = onBack,
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(
                            if (state.isEditMode) R.string.seller_product_form_update
                            else R.string.seller_product_form_save
                        ),
                        onClick = onSubmit,
                        enabled = state.isFormValid && !state.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ProductEditor_SubmitButton"),
                    )

                    if (state.isEditMode) {
                        OutlinedButton(
                            onClick = onOpenDeleteDialog,
                            enabled = !state.isSubmitting,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ProductEditor_DeleteButton"),
                        ) {
                            Text(stringResource(R.string.seller_products_delete))
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Skeleton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Photo Section
                Text(
                    text = stringResource(R.string.seller_product_form_photo),
                    style = MaterialTheme.typography.titleMedium,
                )

                when {
                    state.isUploadingImage -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Text(
                                    text = stringResource(R.string.seller_product_form_photo_uploading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    state.imageUrl != null -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AsyncImage(
                                model = state.imageUrl,
                                contentDescription = stringResource(R.string.seller_product_form_photo),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(onClick = onPickPhoto) {
                                    Text(stringResource(R.string.seller_product_form_photo_change))
                                }
                                IconButton(onClick = onRemovePhoto) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Foto",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        OutlinedCard(
                            onClick = onPickPhoto,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = stringResource(R.string.seller_product_form_photo_add),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                // Name field
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.product_form_name)) },
                    placeholder = { Text(stringResource(R.string.product_form_name_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ProductEditor_NameField"),
                )

                // Price field
                OutlinedTextField(
                    value = state.price,
                    onValueChange = onPriceChange,
                    label = { Text(stringResource(R.string.product_form_price)) },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ProductEditor_PriceField"),
                )

                // Stock field with stepper buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { onStepStock(-1) },
                        enabled = (state.stock.toIntOrNull() ?: 0) > 0,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("−")
                    }

                    OutlinedTextField(
                        value = state.stock,
                        onValueChange = onStockChange,
                        label = { Text(stringResource(R.string.product_form_stock)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ProductEditor_StockField"),
                    )

                    OutlinedButton(
                        onClick = { onStepStock(1) },
                        enabled = (state.stock.toIntOrNull() ?: 0) < ProductEditorState.MAX_STOCK,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("+")
                    }
                }

                // Description field
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.seller_product_form_description)) },
                    placeholder = { Text(stringResource(R.string.seller_product_form_description_hint)) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ProductEditor_DescriptionField"),
                )

                // Status field (only in edit mode)
                if (state.isEditMode) {
                    Text(
                        text = stringResource(R.string.seller_product_form_status),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.status == SellerProduct.Status.AVAILABLE,
                            onClick = { onStatusChange(SellerProduct.Status.AVAILABLE) },
                            label = { Text(stringResource(R.string.seller_products_tab_available)) },
                        )
                        FilterChip(
                            selected = state.status == SellerProduct.Status.OUT_OF_STOCK,
                            onClick = { onStatusChange(SellerProduct.Status.OUT_OF_STOCK) },
                            label = { Text(stringResource(R.string.seller_products_tab_out_of_stock)) },
                        )
                        FilterChip(
                            selected = state.status == SellerProduct.Status.HIDDEN,
                            onClick = { onStatusChange(SellerProduct.Status.HIDDEN) },
                            label = { Text(stringResource(R.string.seller_products_tab_hidden)) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (state.showDeleteDialog) {
        DeleteProductDialog(
            productName = state.name,
            onConfirm = onConfirmDelete,
            onDismiss = onCloseDeleteDialog,
        )
    }
}
