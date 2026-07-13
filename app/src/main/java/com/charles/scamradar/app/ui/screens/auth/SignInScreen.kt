package com.charles.scamradar.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.auth.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(onBack: () -> Unit, onSignedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AuthRepository(context) }

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resetSent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSignUp) "Create account" else "Sign in") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Family pods are tied to your account so membership survives reinstalls and device changes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = {
                    error = null
                    busy = true
                    scope.launch {
                        when (val outcome = repo.signInWithGoogle(context)) {
                            is AuthRepository.AuthOutcome.Success -> onSignedIn()
                            is AuthRepository.AuthOutcome.Failed -> error = outcome.message
                        }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Google")
            }

            Text(
                "or",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(
                    error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (resetSent) {
                Text(
                    "If an account exists for that email, a reset link was sent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = {
                    error = null
                    if (email.isBlank() || password.length < 6) {
                        error = "Enter a valid email and a password of at least 6 characters."
                        return@Button
                    }
                    busy = true
                    scope.launch {
                        val outcome = if (isSignUp) {
                            repo.signUpWithEmail(email, password)
                        } else {
                            repo.signInWithEmail(email, password)
                        }
                        when (outcome) {
                            is AuthRepository.AuthOutcome.Success -> onSignedIn()
                            is AuthRepository.AuthOutcome.Failed -> error = outcome.message
                        }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text(if (isSignUp) "Create account" else "Sign in")
                }
            }

            TextButton(onClick = { isSignUp = !isSignUp; error = null }) {
                Text(
                    if (isSignUp) "Already have an account? Sign in"
                    else "New here? Create an account"
                )
            }

            if (!isSignUp) {
                TextButton(
                    onClick = {
                        if (email.isBlank()) {
                            error = "Enter your email above first."
                            return@TextButton
                        }
                        scope.launch {
                            repo.sendPasswordReset(email)
                            resetSent = true
                        }
                    }
                ) {
                    Text("Forgot password?")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
