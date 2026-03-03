package com.thecityandthebike.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.thecityandthebike.R
import com.thecityandthebike.ui.viewmodel.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    state: AuthState,
    onRegister: (username: String, password: String) -> Unit,
    onNavigateBack: () -> Unit,
    onClearError: () -> Unit,
    onClearRegistrationSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var readUnderstood by remember { mutableStateOf(false) }
    var agreedToLicense by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.registrationSuccess) {
        if (state.registrationSuccess) {
            onClearRegistrationSuccess()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Form card
            Card(
                modifier = Modifier.widthIn(max = 400.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Join the community",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val usernameValidationErrorRes = usernameErrorRes(username)
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            onClearError()
                        },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.NewUsername },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !state.isLoading,
                        isError = usernameValidationErrorRes != null,
                        supportingText = usernameValidationErrorRes?.let { resId -> { Text(stringResource(resId)) } }
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            onClearError()
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.NewPassword },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        enabled = !state.isLoading
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            onClearError()
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.NewPassword },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (isFormValid(username, password, confirmPassword, readUnderstood, agreedToLicense)) {
                                    onRegister(username, password)
                                }
                            }
                        ),
                        enabled = !state.isLoading,
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                        supportingText = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                            { Text("Passwords don't match") }
                        } else null
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Privacy & Copyright title
            Text(
                text = stringResource(R.string.onboarding_privacy_copyright_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Privacy section
            Text(
                text = stringResource(R.string.onboarding_privacy_subtitle),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_privacy_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Copyright section
            Text(
                text = stringResource(R.string.onboarding_copyright_subtitle),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_copyright_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // CC link
            val linkColor = MaterialTheme.colorScheme.primary
            val ccLinkLabel = stringResource(R.string.onboarding_cc_link)
            val ccLinkA11y = stringResource(R.string.onboarding_cc_link_a11y)
            val linkText = buildAnnotatedString {
                val linkStyle = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                )
                pushLink(LinkAnnotation.Url(
                    url = "https://creativecommons.org/licenses/by-nc/4.0/",
                    styles = linkStyle
                ))
                append(ccLinkLabel)
                pop()
            }
            Text(
                text = linkText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = ccLinkA11y
                    }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Checkbox: read and understood
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = readUnderstood,
                        onValueChange = { readUnderstood = it },
                        role = Role.Checkbox
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = readUnderstood,
                    onCheckedChange = null
                )
                Text(
                    text = stringResource(R.string.register_checkbox_read_understood),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Checkbox: agree to license
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = agreedToLicense,
                        onValueChange = { agreedToLicense = it },
                        role = Role.Checkbox
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreedToLicense,
                    onCheckedChange = null
                )
                Text(
                    text = stringResource(R.string.register_checkbox_agree_license),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Create Account button
            Button(
                onClick = { onRegister(username, password) },
                modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
                enabled = !state.isLoading && isFormValid(username, password, confirmPassword, readUnderstood, agreedToLicense)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Account")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private val usernamePattern = Regex("^[a-zA-Z0-9_]+$")

internal fun usernameErrorRes(username: String): Int? {
    if (username.isEmpty()) return null
    if (username.length < 3) return R.string.username_error_too_short
    if (username.length > 50) return R.string.username_error_too_long
    if (!usernamePattern.matches(username)) return R.string.username_error_invalid_chars
    return null
}

private fun isFormValid(
    username: String,
    password: String,
    confirmPassword: String,
    readUnderstood: Boolean,
    agreedToLicense: Boolean
): Boolean {
    return username.isNotBlank() &&
            usernameErrorRes(username) == null &&
            password.isNotBlank() &&
            password.length >= 8 &&
            password == confirmPassword &&
            readUnderstood &&
            agreedToLicense
}
