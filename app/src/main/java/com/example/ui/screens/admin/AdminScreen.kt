package com.example.ui.screens.admin

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.LocationAreaEntity
import com.example.data.model.ProviderEntity
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.VerifiedGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val isFirstTimeSetup by viewModel.isFirstTimeSetup.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Portal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isAuthenticated) {
                            Text(
                                text = "Authorized Session",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAuthenticated) {
                        IconButton(
                            onClick = {
                                viewModel.logout()
                                onShowSnackbar("Signed out of administrator session")
                            },
                            modifier = Modifier.testTag("admin_logout_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Sign Out")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (!isAuthenticated) {
            AdminLoginGate(
                onFirebaseLogin = { email, pass ->
                    viewModel.loginWithEmail(email, pass) { success, msg ->
                        onShowSnackbar(msg)
                    }
                },
                onLocalPasscodeLogin = { passcode ->
                    viewModel.loginWithPasscode(passcode) { success, msg ->
                        onShowSnackbar(msg)
                    }
                },
                isFirstTime = isFirstTimeSetup,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == AdminTab.PROVIDERS,
                        onClick = { viewModel.setSelectedTab(AdminTab.PROVIDERS) },
                        text = { Text("Providers") }
                    )
                    Tab(
                        selected = selectedTab == AdminTab.ADD_EDIT,
                        onClick = { viewModel.setSelectedTab(AdminTab.ADD_EDIT) },
                        text = { Text("Add / Edit") }
                    )
                    Tab(
                        selected = selectedTab == AdminTab.CSV_SYNC,
                        onClick = { viewModel.setSelectedTab(AdminTab.CSV_SYNC) },
                        text = { Text("CSV & Sync") }
                    )
                    Tab(
                        selected = selectedTab == AdminTab.CATEGORIES_LOCALITIES,
                        onClick = { viewModel.setSelectedTab(AdminTab.CATEGORIES_LOCALITIES) },
                        text = { Text("Categories & Areas") }
                    )
                }

                when (selectedTab) {
                    AdminTab.PROVIDERS -> AdminProvidersTab(
                        viewModel = viewModel,
                        onAddNew = { viewModel.initNewProviderForm() },
                        onShowSnackbar = onShowSnackbar
                    )
                    AdminTab.ADD_EDIT -> AdminAddEditTab(
                        viewModel = viewModel,
                        onShowSnackbar = onShowSnackbar
                    )
                    AdminTab.CSV_SYNC -> AdminCsvSyncTab(
                        viewModel = viewModel,
                        onShowSnackbar = onShowSnackbar
                    )
                    AdminTab.CATEGORIES_LOCALITIES -> AdminCategoriesAreasTab(
                        viewModel = viewModel,
                        onShowSnackbar = onShowSnackbar
                    )
                }
            }
        }
    }
}

@Composable
fun AdminLoginGate(
    onFirebaseLogin: (String, String) -> Unit,
    onLocalPasscodeLogin: (String) -> Unit,
    isFirstTime: Boolean,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passcode by remember { mutableStateOf("") }
    var isLocalMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isLocalMode) "Offline Admin Passkey" else "Firebase Administrator Sign In",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isLocalMode) {
                "Sign in using your local passkey for offline administrative actions."
            } else {
                "Sign in with your registered Firebase Administrator email and password to synchronize Cloud Firestore."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!isLocalMode) {
            // Firebase Email & Password Form
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Firebase Admin Email") },
                placeholder = { Text("admin@ishwardi.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_email_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_password_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onFirebaseLogin(email, password) },
                enabled = email.isNotBlank() && password.length >= 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_firebase_login_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In with Firebase")
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = { isLocalMode = true }
            ) {
                Text("Use Local Offline Passkey Instead", fontSize = 13.sp)
            }
        } else {
            // Local Passkey Mode
            OutlinedTextField(
                value = passcode,
                onValueChange = { passcode = it },
                label = { Text(if (isFirstTime) "Create Local Passkey" else "Local Passkey") },
                placeholder = { Text("Minimum 4 characters") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_passcode_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onLocalPasscodeLogin(passcode) },
                enabled = passcode.trim().length >= 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_login_submit_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFirstTime) "Set Passkey & Enter" else "Authenticate Locally")
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = { isLocalMode = false }
            ) {
                Text("Back to Firebase Email Login", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AdminProvidersTab(
    viewModel: AdminViewModel,
    onAddNew: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val allProviders by viewModel.allProviders.collectAsState()
    val searchQuery by viewModel.adminSearchQuery.collectAsState()

    var providerToDelete by remember { mutableStateOf<ProviderEntity?>(null) }

    val filtered = remember(allProviders, searchQuery) {
        if (searchQuery.isBlank()) allProviders
        else {
            val q = searchQuery.trim().lowercase()
            allProviders.filter {
                it.name.lowercase().contains(q) ||
                    it.contactNumber.contains(q) ||
                    it.primaryCategory.lowercase().contains(q) ||
                    it.locationArea.lowercase().contains(q)
            }
        }
    }

    val hasDemoData = allProviders.any { it.isDemoData }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search and Actions bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setAdminSearchQuery(it) },
                    placeholder = { Text("Search providers by name, phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = onAddNew,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("admin_add_provider_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            if (hasDemoData) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Demo data is currently loaded in database",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    TextButton(
                        onClick = {
                            viewModel.deleteDemoData {
                                onShowSnackbar("Sample demonstration data removed")
                            }
                        }
                    ) {
                        Text("Remove Demo Data", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { provider ->
                AdminProviderCard(
                    provider = provider,
                    onEdit = { viewModel.editProvider(provider) },
                    onDelete = { providerToDelete = provider },
                    onToggleActive = { isActive -> viewModel.toggleProviderStatus(provider.id, isActive) },
                    onToggleVerified = { isVerified -> viewModel.toggleVerifiedStatus(provider.id, isVerified) }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (providerToDelete != null) {
        val p = providerToDelete!!
        AlertDialog(
            onDismissRequest = { providerToDelete = null },
            title = { Text("Confirm Deletion") },
            text = {
                Text("Are you sure you want to permanently delete '${p.name}' (${p.primaryCategory}) from the Ishwardi directory?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProvider(p.id) { success, msg ->
                            onShowSnackbar(msg)
                            if (success) {
                                providerToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { providerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminProviderCard(
    provider: ProviderEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onToggleVerified: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (provider.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (provider.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = VerifiedGreen, modifier = Modifier.size(16.dp))
                        }
                        if (provider.isDemoData) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text("DEMO", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                    }
                    Text(
                        text = "${provider.primaryCategory} • ${provider.locationArea}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Phone: ${provider.contactNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Active Switch
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = provider.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.testTag("switch_active_${provider.id}")
                    )
                    Text(
                        text = if (provider.isActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (provider.isActive) VerifiedGreen else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action row: Verify toggle, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onToggleVerified(!provider.isVerified) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (provider.isVerified) Icons.Default.Check else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (provider.isVerified) "Verified" else "Mark Verified", fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_provider_${provider.id}")) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Provider", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_provider_${provider.id}")) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Provider", tint = EmergencyRed)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddEditTab(
    viewModel: AdminViewModel,
    onShowSnackbar: (String) -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val duplicates by viewModel.duplicateWarnings.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val locationAreas by viewModel.allLocationAreas.collectAsState()

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var locationDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (formState.isEditing) "Edit Provider Profile" else "Add New Service Provider",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Duplicate Warning Alert
        if (duplicates.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Potential Duplicate Detected!",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "A provider matching this name or contact number already exists: '${duplicates.first().name}' (${duplicates.first().contactNumber}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Required: Name
        OutlinedTextField(
            value = formState.name,
            onValueChange = { viewModel.updateFormField { s -> s.copy(name = it) } },
            label = { Text("Provider / Business Name *") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("form_name_input")
        )

        // Optional: Bangla Name
        OutlinedTextField(
            value = formState.banglaName,
            onValueChange = { viewModel.updateFormField { s -> s.copy(banglaName = it) } },
            label = { Text("Bangla Name (বাংলা নাম, ঐচ্ছিক)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Required: Primary Category (Dropdown)
        ExposedDropdownMenuBox(
            expanded = categoryDropdownExpanded,
            onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = formState.primaryCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Primary Category *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("form_category_dropdown")
            )
            ExposedDropdownMenu(
                expanded = categoryDropdownExpanded,
                onDismissRequest = { categoryDropdownExpanded = false }
            ) {
                categories.filter { it.isEnabled }.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name) },
                        onClick = {
                            viewModel.updateFormField { s -> s.copy(primaryCategory = cat.name) }
                            categoryDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Additional Categories (Comma-separated)
        OutlinedTextField(
            value = formState.additionalCategories,
            onValueChange = { viewModel.updateFormField { s -> s.copy(additionalCategories = it) } },
            label = { Text("Additional Categories (Optional, comma-separated)") },
            placeholder = { Text("e.g. Water Services, Home Appliances") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Required: Contact Number
        OutlinedTextField(
            value = formState.contactNumber,
            onValueChange = { viewModel.updateFormField { s -> s.copy(contactNumber = it) } },
            label = { Text("Primary Contact Number *") },
            placeholder = { Text("e.g. 01700-000000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("form_contact_input")
        )

        // Optional: Additional Contact Number
        OutlinedTextField(
            value = formState.additionalContact,
            onValueChange = { viewModel.updateFormField { s -> s.copy(additionalContact = it) } },
            label = { Text("Additional Contact Number (Optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Required: Location / Service Area (Dropdown + custom)
        ExposedDropdownMenuBox(
            expanded = locationDropdownExpanded,
            onExpandedChange = { locationDropdownExpanded = !locationDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = formState.locationArea,
                onValueChange = { viewModel.updateFormField { s -> s.copy(locationArea = it) } },
                label = { Text("Location / Service Area *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("form_location_dropdown")
            )
            ExposedDropdownMenu(
                expanded = locationDropdownExpanded,
                onDismissRequest = { locationDropdownExpanded = false }
            ) {
                locationAreas.filter { it.isEnabled }.forEach { loc ->
                    DropdownMenuItem(
                        text = { Text(loc.name) },
                        onClick = {
                            viewModel.updateFormField { s -> s.copy(locationArea = loc.name) }
                            locationDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Specific Address & Landmark
        OutlinedTextField(
            value = formState.specificAddress,
            onValueChange = { viewModel.updateFormField { s -> s.copy(specificAddress = it) } },
            label = { Text("Specific Address or Street") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = formState.landmark,
            onValueChange = { viewModel.updateFormField { s -> s.copy(landmark = it) } },
            label = { Text("Recognizable Landmark (Optional)") },
            placeholder = { Text("e.g. Near Rail Gate, Behind Post Office") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Approximate Charge
        OutlinedTextField(
            value = formState.chargeEstimate,
            onValueChange = { viewModel.updateFormField { s -> s.copy(chargeEstimate = it) } },
            label = { Text("Approximate Charge / Price Range") },
            placeholder = { Text("e.g. ৳300 - ৳500 per visit, or Hourly") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // WhatsApp number
        OutlinedTextField(
            value = formState.whatsappNumber,
            onValueChange = { viewModel.updateFormField { s -> s.copy(whatsappNumber = it) } },
            label = { Text("WhatsApp Number (Optional)") },
            placeholder = { Text("Leave blank to use primary contact") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Facebook URL
        OutlinedTextField(
            value = formState.facebookUrl,
            onValueChange = { viewModel.updateFormField { s -> s.copy(facebookUrl = it) } },
            label = { Text("Facebook Page or Profile URL") },
            placeholder = { Text("https://facebook.com/...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Working hours
        OutlinedTextField(
            value = formState.workingHours,
            onValueChange = { viewModel.updateFormField { s -> s.copy(workingHours = it) } },
            label = { Text("Working Hours (Optional)") },
            placeholder = { Text("e.g. 9:00 AM - 9:00 PM, 24/7 for Emergency") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Map Coordinates
        OutlinedTextField(
            value = formState.mapCoordinates,
            onValueChange = { viewModel.updateFormField { s -> s.copy(mapCoordinates = it) } },
            label = { Text("Map Coordinates or Search Pin (Optional)") },
            placeholder = { Text("e.g. 24.1528,89.0664") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Description
        OutlinedTextField(
            value = formState.description,
            onValueChange = { viewModel.updateFormField { s -> s.copy(description = it) } },
            label = { Text("Detailed Service Description") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth()
        )

        // Verified & Active Checkboxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = formState.isVerified,
                    onCheckedChange = { viewModel.updateFormField { s -> s.copy(isVerified = it) } }
                )
                Text("Verified by Administrator")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = formState.isActive,
                    onCheckedChange = { viewModel.updateFormField { s -> s.copy(isActive = it) } }
                )
                Text("Active Listing")
            }
        }

        // Save and Cancel buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.setSelectedTab(AdminTab.PROVIDERS) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    viewModel.saveProvider { success, message ->
                        onShowSnackbar(message)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("form_save_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Provider")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AdminCsvSyncTab(
    viewModel: AdminViewModel,
    onShowSnackbar: (String) -> Unit
) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val csvPreview by viewModel.csvImportPreview.collectAsState()
    val adminIdentifier by viewModel.adminIdentifier.collectAsState()
    val adminUid by viewModel.adminUid.collectAsState()
    val isAllowlistVerified by viewModel.isAllowlistVerified.collectAsState()
    val authMethod by viewModel.authMethod.collectAsState()
    val isUploadingCloud by viewModel.isUploadingCloud.collectAsState()
    var csvInputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Firebase Cloud Diagnostics & Telemetry Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cloud Diagnostics & Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAllowlistVerified) VerifiedGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isAllowlistVerified) "Admin Verified" else "Allowlist Pending",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isAllowlistVerified) VerifiedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text("• Firebase Project: ishwardi-x-directory", style = MaterialTheme.typography.bodySmall)
                Text("• Firestore DB: (default)", style = MaterialTheme.typography.bodySmall)
                Text("• Admin Session: ${adminIdentifier ?: "None"}", style = MaterialTheme.typography.bodySmall)
                if (adminUid != null) {
                    Text("• Admin UID: $adminUid", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                }
                Text("• Auth Type: $authMethod", style = MaterialTheme.typography.bodySmall)
                Text("• Sync Engine: ${syncStatus.syncMessage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.triggerSync()
                            onShowSnackbar("Sync listener refreshed.")
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh Sync", fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Manual Upload of Local Contacts to Cloud
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Upload Local Providers to Cloud",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Uploads your locally entered providers to Cloud Firestore using their existing UUIDs, making them visible to other phones. Sample demo data is automatically excluded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        viewModel.uploadLocalContacts { success, msg ->
                            onShowSnackbar(msg)
                        }
                    },
                    enabled = !isUploadingCloud,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUploadingCloud) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading...")
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Local Providers to Firestore")
                    }
                }
            }
        }

        // CSV Export Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Export Directory to CSV",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Export full provider database as standardized CSV file for backup or external reporting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        viewModel.exportCsv { csvData ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "Ishwardi.X Service Directory Export")
                                putExtra(Intent.EXTRA_TEXT, csvData)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Directory CSV"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export & Share CSV")
                }
            }
        }

        // CSV Import Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Batch Import Providers (CSV)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Paste comma-separated provider data. Required columns: Name, Category, Contact, Location.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = csvInputText,
                    onValueChange = { csvInputText = it },
                    placeholder = {
                        Text(
                            "Name,BanglaName,Category,AdditionalCategories,Contact,AltContact,Location,Address,Landmark,Charge,Description,WhatsApp,Facebook,WorkingHours,Verified\n" +
                                "Md. Rahim,,Electricians,,01711-123456,,Ishwardi Sadar,,,,,,,,true"
                        )
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            csvInputText = "Name,BanglaName,Category,AdditionalCategories,Contact,AltContact,Location,Address,Landmark,Charge,Description,WhatsApp,Facebook,WorkingHours,Verified\n" +
                                "Tariqul Islam,তারিকুল ইসলাম,Electricians,Home Appliances,01712-345678,,Rooppur,Project Road,,৳400,Expert industrial & domestic wiring,01712345678,,,true\n" +
                                "Ishwardi Express,ঈশ্বরদী এক্সপ্রেস,Transport and Vehicle Rental,,01811-987654,,Station Road,Near Rail Gate,,Fixed per trip,24/7 Car and ambulance rental,01811987654,,24/7,true"
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Load Sample CSV")
                    }

                    Button(
                        onClick = {
                            if (csvInputText.isBlank()) {
                                onShowSnackbar("Please paste CSV data first")
                            } else {
                                viewModel.previewCsvImport(csvInputText)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validate & Import")
                    }
                }

                // Import Report Preview
                if (csvPreview != null) {
                    val report = csvPreview!!
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Import Results:",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "✅ Successfully imported: ${report.validProviders.size} providers",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VerifiedGreen,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (report.rowErrors.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "❌ Validation Errors (${report.rowErrors.size}):",
                                    color = EmergencyRed,
                                    fontWeight = FontWeight.Bold
                                )
                                report.rowErrors.forEach { err ->
                                    Text(text = " • $err", style = MaterialTheme.typography.bodySmall, color = EmergencyRed)
                                }
                            }

                            if (report.duplicateWarnings.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚠️ Duplicate Warnings (${report.duplicateWarnings.size}):",
                                    fontWeight = FontWeight.Bold
                                )
                                report.duplicateWarnings.forEach { warn ->
                                    Text(text = " • $warn", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCategoriesAreasTab(
    viewModel: AdminViewModel,
    onShowSnackbar: (String) -> Unit
) {
    val categories by viewModel.allCategories.collectAsState()
    val localities by viewModel.allLocationAreas.collectAsState()

    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryBangla by remember { mutableStateOf("") }

    var newAreaName by remember { mutableStateOf("") }
    var newAreaBangla by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Categories Management Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Manage Service Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add new categories or toggle category visibility in the directory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            placeholder = { Text("New category name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newCategoryBangla,
                            onValueChange = { newCategoryBangla = it },
                            placeholder = { Text("Bangla (ঐচ্ছিক)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                viewModel.addCategory(newCategoryName, newCategoryBangla, "build")
                                onShowSnackbar("Category '$newCategoryName' added")
                                newCategoryName = ""
                                newCategoryBangla = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Custom Category")
                    }
                }
            }
        }

        items(categories, key = { it.id }) { cat ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (cat.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = cat.name,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (cat.banglaName.isNotBlank()) {
                            Text(
                                text = cat.banglaName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = cat.isEnabled,
                        onCheckedChange = { viewModel.toggleCategory(cat) }
                    )
                }
            }
        }

        // Localities Management Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Manage Ishwardi Localities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add or update recognized Ishwardi neighborhoods and service zones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newAreaName,
                            onValueChange = { newAreaName = it },
                            placeholder = { Text("New locality name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newAreaBangla,
                            onValueChange = { newAreaBangla = it },
                            placeholder = { Text("Bangla (ঐচ্ছিক)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            if (newAreaName.isNotBlank()) {
                                viewModel.addLocation(newAreaName, newAreaBangla)
                                onShowSnackbar("Locality '$newAreaName' added")
                                newAreaName = ""
                                newAreaBangla = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Locality")
                    }
                }
            }
        }

        items(localities, key = { it.id }) { loc ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (loc.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = loc.name,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (loc.banglaName.isNotBlank()) {
                            Text(
                                text = loc.banglaName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = loc.isEnabled,
                        onCheckedChange = { viewModel.toggleLocation(loc) }
                    )
                }
            }
        }
    }
}
