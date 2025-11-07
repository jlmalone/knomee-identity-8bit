package com.knomee.identity.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.knomee.identity.theme.RetroColors
import com.knomee.identity.theme.RetroTypography
import com.knomee.identity.utils.ValidationUtils

/**
 * Dialog for requesting primary ID verification
 */
@Composable
fun RequestPrimaryIDDialog(
    onSubmit: (justification: String, stakeEth: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var justification by remember { mutableStateOf("") }
    var stakeAmount by remember { mutableStateOf("0.03") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .background(RetroColors.WindowBackground, RoundedCornerShape(8.dp))
                .border(4.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "REQUEST PRIMARY ID",
                style = RetroTypography.heading,
                color = RetroColors.BorderColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Become a verified human with voting rights",
                style = RetroTypography.caption,
                color = RetroColors.NESGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Justification input
            Text(
                text = "Justification:",
                style = RetroTypography.body,
                color = RetroColors.NESWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = justification,
                onValueChange = { justification = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("Explain why you should be verified...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = RetroColors.CardBackground,
                    unfocusedContainerColor = RetroColors.CardBackground,
                    focusedTextColor = RetroColors.NESWhite,
                    unfocusedTextColor = RetroColors.NESWhite
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stake amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stake (ETH):",
                    style = RetroTypography.body,
                    color = RetroColors.NESWhite
                )
                TextField(
                    value = stakeAmount,
                    onValueChange = { stakeAmount = it },
                    modifier = Modifier.width(120.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = RetroColors.CardBackground,
                        unfocusedContainerColor = RetroColors.CardBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            InfoBox(
                "REQUIREMENTS:\n" +
                "• Minimum 0.03 ETH stake\n" +
                "• 67% consensus required\n" +
                "• Win: Get refund + rewards\n" +
                "• Lose: 30% slashing"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Validation error display
            validationError?.let { error ->
                Text(
                    text = error,
                    style = RetroTypography.caption,
                    color = RetroColors.Error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RetroDialogButton("CANCEL", RetroColors.Error) { onDismiss() }
                RetroDialogButton("SUBMIT", RetroColors.Success) {
                    // Validate justification
                    val justificationError = ValidationUtils.validateJustification(justification, minLength = 20)
                    if (justificationError != null) {
                        validationError = justificationError
                        return@RetroDialogButton
                    }

                    // Validate stake amount
                    val stakeError = ValidationUtils.validateStakeAmount(stakeAmount, minStake = 0.03)
                    if (stakeError != null) {
                        validationError = stakeError
                        return@RetroDialogButton
                    }

                    val stake = stakeAmount.toDouble()
                    validationError = null
                    onSubmit(justification, stake)
                    onDismiss()
                }
            }
        }
    }
}

/**
 * Dialog for linking secondary account
 */
@Composable
fun LinkSecondaryAccountDialog(
    onSubmit: (primaryAddress: String, platform: String, justification: String, stakeEth: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var primaryAddress by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var justification by remember { mutableStateOf("") }
    var stakeAmount by remember { mutableStateOf("0.01") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .background(RetroColors.WindowBackground, RoundedCornerShape(8.dp))
                .border(4.dp, RetroColors.BorderColor, RoundedCornerShape(8.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LINK SECONDARY ACCOUNT",
                style = RetroTypography.heading,
                color = RetroColors.BorderColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Link this address to a primary identity",
                style = RetroTypography.caption,
                color = RetroColors.NESGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary address input
            Text(
                text = "Primary Address:",
                style = RetroTypography.body,
                color = RetroColors.NESWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = primaryAddress,
                onValueChange = { primaryAddress = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0x...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = RetroColors.CardBackground,
                    unfocusedContainerColor = RetroColors.CardBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Platform input
            Text(
                text = "Platform:",
                style = RetroTypography.body,
                color = RetroColors.NESWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = platform,
                onValueChange = { platform = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., Twitter, GitHub, LinkedIn") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = RetroColors.CardBackground,
                    unfocusedContainerColor = RetroColors.CardBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Justification input
            Text(
                text = "Justification:",
                style = RetroTypography.body,
                color = RetroColors.NESWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = justification,
                onValueChange = { justification = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = { Text("Why these accounts belong together...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = RetroColors.CardBackground,
                    unfocusedContainerColor = RetroColors.CardBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stake amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stake (ETH):",
                    style = RetroTypography.body,
                    color = RetroColors.NESWhite
                )
                TextField(
                    value = stakeAmount,
                    onValueChange = { stakeAmount = it },
                    modifier = Modifier.width(120.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = RetroColors.CardBackground,
                        unfocusedContainerColor = RetroColors.CardBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            InfoBox(
                "REQUIREMENTS:\n" +
                "• Minimum 0.01 ETH stake\n" +
                "• 51% consensus required\n" +
                "• Lose: 10% slashing"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Validation error display
            validationError?.let { error ->
                Text(
                    text = error,
                    style = RetroTypography.caption,
                    color = RetroColors.Error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RetroDialogButton("CANCEL", RetroColors.Error) { onDismiss() }
                RetroDialogButton("SUBMIT", RetroColors.Success) {
                    // Validate primary address
                    val checksumAddress = ValidationUtils.validateAndChecksumAddress(primaryAddress)
                    if (checksumAddress == null) {
                        validationError = "Invalid Ethereum address"
                        return@RetroDialogButton
                    }

                    // Validate platform
                    if (platform.isBlank() || platform.length < 2) {
                        validationError = "Platform name must be at least 2 characters"
                        return@RetroDialogButton
                    }

                    // Validate justification
                    val justificationError = ValidationUtils.validateJustification(justification, minLength = 20)
                    if (justificationError != null) {
                        validationError = justificationError
                        return@RetroDialogButton
                    }

                    // Validate stake amount
                    val stakeError = ValidationUtils.validateStakeAmount(stakeAmount, minStake = 0.01)
                    if (stakeError != null) {
                        validationError = stakeError
                        return@RetroDialogButton
                    }

                    val stake = stakeAmount.toDouble()
                    validationError = null
                    onSubmit(checksumAddress, platform, justification, stake)
                    onDismiss()
                }
            }
        }
    }
}

/**
 * Retro-styled dialog button
 */
@Composable
fun RetroDialogButton(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(40.dp)
            .border(2.dp, color, RoundedCornerShape(4.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = RetroColors.WindowBackground,
            contentColor = color
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = RetroTypography.button
        )
    }
}
