package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelRose

@Composable
fun AuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (emailOrUsername: String, pass: String) -> Unit,
    onSignUp: (username: String, email: String, pass: String) -> Unit,
    onClearError: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }

    var usernameInput by remember { mutableStateOf("") }
    var emailOrUsernameInput by remember { mutableStateOf("") }
    var emailSignUpInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Banner with Mascot Image (Direct transparent mascot without Card)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.maskot_1),
                contentDescription = "Mochibot Mascot",
                modifier = Modifier.size(150.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "are you okay?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PastelLavender
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isLoginMode) "Ruang Aman Curhat & Refleksi Diri" else "Buat Akun Privat & Bebas Tuduhan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle Switch Mode (Login / Sign Up)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isLoginMode) PastelLavender.copy(alpha = 0.25f) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            isLoginMode = true
                            onClearError()
                        }
                ) {
                    Text(
                        text = "Masuk (Login)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isLoginMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLoginMode) PastelLavender else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (!isLoginMode) PastelLavender.copy(alpha = 0.25f) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            isLoginMode = false
                            onClearError()
                        }
                ) {
                    Text(
                        text = "Daftar (Sign Up)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (!isLoginMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isLoginMode) PastelLavender else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form Card with Animated Transition
        AnimatedContent(
            targetState = isLoginMode,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                }.using(SizeTransform(clip = false))
            },
            label = "auth_screen_transition",
            modifier = Modifier.fillMaxWidth()
        ) { activeLoginMode ->
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (activeLoginMode) "Masuk ke Akunmu 👋" else "Daftar Akun Baru 🌟",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PastelRose.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PastelRose)
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = PastelRose,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (!activeLoginMode) {
                        // Sign Up: Username Input
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Username") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PastelLavender)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PastelLavender,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = PastelLavender
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Sign Up: Email Input
                        OutlinedTextField(
                            value = emailSignUpInput,
                            onValueChange = { emailSignUpInput = it },
                            label = { Text("Email") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = PastelLavender)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PastelLavender,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = PastelLavender
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Login: Email or Username Input
                        OutlinedTextField(
                            value = emailOrUsernameInput,
                            onValueChange = { emailOrUsernameInput = it },
                            label = { Text("Email / Username") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PastelLavender)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PastelLavender,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = PastelLavender
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Password Input
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Kata Sandi") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PastelLavender)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PastelLavender,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = PastelLavender
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (activeLoginMode) {
                                onLogin(emailOrUsernameInput, passwordInput)
                            } else {
                                onSignUp(usernameInput, emailSignUpInput, passwordInput)
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PastelLavender,
                            contentColor = Color(0xFF261833)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFF261833),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (activeLoginMode) "Masuk Sekarang" else "Daftar Akun Baru",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch Mode Footer Text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isLoginMode) "Belum punya akun? " else "Sudah punya akun? ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isLoginMode) "Daftar Akun" else "Masuk Akun",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = PastelLavender,
                modifier = Modifier.clickable {
                    isLoginMode = !isLoginMode
                    onClearError()
                }
            )
        }
    }
}
