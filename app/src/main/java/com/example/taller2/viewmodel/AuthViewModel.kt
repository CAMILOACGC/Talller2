package com.example.taller2.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    fun signUp(email: String, pass: String, onSuccess: () -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { 
                _currentUser.value = auth.currentUser
                onSuccess() 
            }
            .addOnFailureListener { _authError.value = it.message }
    }

    fun signIn(email: String, pass: String, onSuccess: () -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { 
                _currentUser.value = auth.currentUser
                onSuccess() 
            }
            .addOnFailureListener { _authError.value = it.message }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { 
                _currentUser.value = auth.currentUser
                onSuccess() 
            }
            .addOnFailureListener { _authError.value = it.message }
    }

    fun signOut(onComplete: () -> Unit) {
        auth.signOut()
        _currentUser.value = null
        onComplete()
    }
}
