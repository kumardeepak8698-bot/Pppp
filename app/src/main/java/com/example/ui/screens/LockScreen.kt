package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LockScreen(
    onUnlockSuccess: () -> Unit,
    onVerifyPin: (String) -> Boolean
) {
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)), // Dark stealth layout
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = Color(0xFF1E1E1E)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Text(
                "ENCLAVE COLD VAULT",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Text(
                "Authorized administrator signature required",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pinText,
                onValueChange = {
                    if (it.length <= 6) {
                        pinText = it
                        errorMessage = null
                    }
                },
                label = { Text("6-Digit Vault PIN", color = Color.Gray) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF64B5F6),
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lock_pin_field")
            )

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    if (onVerifyPin(pinText)) {
                        onUnlockSuccess()
                    } else {
                        errorMessage = "Invalid PIN authentication. Enter valid PIN or 1234 bypass"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lock_unlock_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6))
            ) {
                Text("Verify Crypto Credentials", fontWeight = FontWeight.Bold, color = Color.Black)
            }

            // Biometric Simulation Trigger button
            TextButton(
                onClick = {
                    // Simulated Biometric success fallback trigger
                    onUnlockSuccess()
                }
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bypass with Biometric Signature", color = Color.Gray)
            }
        }
    }
}
