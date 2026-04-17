package com.example.taller2.ui

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.taller2.R
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taller2.viewmodel.GameViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreen(viewModel: GameViewModel) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authError by viewModel.authError.collectAsState()

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("710428830847-nb1rfhgvjbrgh9gtd179phecu9mtvcqj.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { viewModel.signInWithGoogle(it) {} }
        } catch (e: ApiException) {
            Log.e("Auth", "Google sign in failed code: ${e.statusCode}")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.auth_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (authError != null) {
            Text(authError!!, color = Color.Red, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        val canAuth = email.isNotBlank() && password.length >= 6

        Button(
            onClick = { viewModel.signIn(email, password) {} },
            modifier = Modifier.fillMaxWidth(),
            enabled = canAuth
        ) {
            Text(stringResource(R.string.login_button))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.signUp(email, password) {} },
            modifier = Modifier.fillMaxWidth(),
            enabled = canAuth
        ) {
            Text(stringResource(R.string.signup_button))
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { launcher.launch(googleSignInClient.signInIntent) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
        ) {
            Text(stringResource(R.string.google_button), color = Color.White)
        }

        if (password.isNotEmpty() && password.length < 6) {
            Text(stringResource(R.string.password_too_short), fontSize = 11.sp, color = Color.Gray)
        }
    }
}
