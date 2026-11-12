package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.localization.AppStrings
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun WelcomePortalScreen(
    viewModel: MainViewModel,
    onNavigateToAdmin: () -> Unit,
    onNavigateToTeacher: () -> Unit,
    onNavigateToParent: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val lang by viewModel.language.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()

    var showAdminPasscodeDialog by remember { mutableStateOf(false) }
    var adminPasscode by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf(false) }

    var showParentRegisterDialog by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    fun signInWithGoogle(onSuccessRole: ((String) -> Unit)? = null) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            authError = "Google Web Client ID missing: ${e.message}"
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        isSigningIn = true
        scope.launch {
            try {
                val result = credentialManager.getCredential(context as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    viewModel.refreshAuthUser()
                    isSigningIn = false
                    onSuccessRole?.invoke("AUTH_OK")
                } else {
                    authError = "Unexpected credential format"
                    isSigningIn = false
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In dismissed: ${e.message}", e)
                isSigningIn = false
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                authError = e.localizedMessage ?: "Sign-in failed"
                isSigningIn = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NavyPrimary,
                        NavyDark,
                        Color(0xFF0A192F)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Language Switcher Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = NavyLight.copy(alpha = 0.6f),
                modifier = Modifier.testTag("welcome_language_toggle")
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Button(
                        onClick = { viewModel.setLanguage("so") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lang == "so") AmberWarning else Color.Transparent,
                            contentColor = if (lang == "so") Color.Black else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Somali", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = { viewModel.setLanguage("en") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lang == "en") AmberWarning else Color.Transparent,
                            contentColor = if (lang == "en") Color.Black else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("English", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // School Emblem & Logo
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .border(3.dp, AmberWarning, CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.school_logo),
                contentDescription = "School Logo",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // School Name & Motto
        Text(
            text = schoolConfig.name.uppercase(java.util.Locale.ROOT),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = schoolConfig.motto,
            color = BlueLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        // Single School Restriction Badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2B6CB0).copy(alpha = 0.35f),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(AmberWarning, BlueLight))),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Single School System • ID: ${schoolConfig.schoolId}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Section Title: Select Portal
        Text(
            text = AppStrings.get("select_role", lang),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )

        // 3 Role Cards: ADMIN, TEACHER, PARENT
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Role 1: ADMIN
            RolePortalCard(
                title = AppStrings.get("admin", lang),
                portalName = AppStrings.get("admin_portal", lang),
                desc = if (lang == "so") "Maamul ardayda, macallimiinta, fasalada, imtixaannada & lacagaha" else "Manage teachers, students, classes, examinations & finances",
                icon = Icons.Default.AdminPanelSettings,
                accentColor = AmberWarning,
                bgGradient = listOf(Color(0xFF1E3A8A), Color(0xFF172554)),
                tag = "portal_admin_button",
                onClick = {
                    if (currentUser == null) {
                        signInWithGoogle {
                            showAdminPasscodeDialog = true
                        }
                    } else if (userProfile?.role == "ADMIN") {
                        onNavigateToAdmin()
                    } else {
                        showAdminPasscodeDialog = true
                    }
                }
            )

            // Role 2: TEACHER
            RolePortalCard(
                title = AppStrings.get("teacher", lang),
                portalName = AppStrings.get("teacher_portal", lang),
                desc = if (lang == "so") "Xaadiriska fasalka, buundooyinka ardayda, shaqo-guri & kaararka" else "Class attendance, marks entry, student remarks & report cards",
                icon = Icons.Default.School,
                accentColor = BlueLight,
                bgGradient = listOf(Color(0xFF1E40AF), Color(0xFF1E293B)),
                tag = "portal_teacher_button",
                onClick = {
                    if (currentUser == null) {
                        signInWithGoogle {
                            onNavigateToTeacher()
                        }
                    } else {
                        onNavigateToTeacher()
                    }
                }
            )

            // Role 3: PARENT
            RolePortalCard(
                title = AppStrings.get("parent", lang),
                portalName = AppStrings.get("parent_portal", lang),
                desc = if (lang == "so") "Natiijooyinka carruurtaada, xaadiriska, fiiga, shaqo-guri & jadwalka" else "Track child grades, daily attendance, fee balances & remarks",
                icon = Icons.Default.FamilyRestroom,
                accentColor = GreenLight,
                bgGradient = listOf(Color(0xFF065F46), Color(0xFF0F172A)),
                tag = "portal_parent_button",
                onClick = {
                    if (currentUser == null) {
                        signInWithGoogle {
                            if (userProfile?.role != "PARENT") {
                                showParentRegisterDialog = true
                            } else {
                                onNavigateToParent()
                            }
                        }
                    } else if (userProfile?.role == "PARENT") {
                        onNavigateToParent()
                    } else {
                        showParentRegisterDialog = true
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Active Sign-in status / Google Button
        if (currentUser == null) {
            Button(
                onClick = { signInWithGoogle() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NavyPrimary)
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        AppStrings.get("sign_in_google", lang),
                        color = NavyPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDark.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = currentUser?.displayName ?: "User",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            color = BlueLight,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.signOut {} },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFC8181)),
                        modifier = Modifier.testTag("welcome_sign_out_button")
                    ) {
                        Text(AppStrings.get("sign_out", lang), fontSize = 12.sp)
                    }
                }
            }
        }

        if (authError != null) {
            Text(
                text = authError ?: "",
                color = Color(0xFFFEB2B2),
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Admin Passcode Dialog
    if (showAdminPasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showAdminPasscodeDialog = false },
            title = {
                Text(
                    AppStrings.get("admin_portal", lang),
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            },
            text = {
                Column {
                    Text(
                        if (lang == "so")
                            "Geli furaha sirta ah ee maamulka (Master Passcode: IFTIN#2026):"
                        else
                            "Enter the administrator security passcode (Master Passcode: IFTIN#2026):",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = adminPasscode,
                        onValueChange = {
                            adminPasscode = it
                            passcodeError = false
                        },
                        label = { Text(AppStrings.get("admin_access_code", lang)) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passcodeError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_passcode_input")
                    )
                    if (passcodeError) {
                        Text(
                            AppStrings.get("invalid_passcode", lang),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyAdminPasscode(
                            enteredPasscode = adminPasscode,
                            onSuccess = {
                                showAdminPasscodeDialog = false
                                onNavigateToAdmin()
                            },
                            onError = { passcodeError = true }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.testTag("admin_passcode_submit")
                ) {
                    Text(AppStrings.get("verify", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminPasscodeDialog = false }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }

    // Parent Room Creation Dialog
    if (showParentRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showParentRegisterDialog = false },
            title = {
                Text(
                    AppStrings.get("create_parent_room", lang),
                    fontWeight = FontWeight.Bold,
                    color = GreenAccent
                )
            },
            text = {
                Text(
                    if (lang == "so")
                        "Ma doonaysaa in aad samaysato qolka waalidka ee Iftin Academy si aad ula socoto carruurtaada?"
                    else
                        "Do you want to create your Parent Room for Iftin Academy to monitor your children's progress?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createParentRoom {
                            showParentRegisterDialog = false
                            onNavigateToParent()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                    modifier = Modifier.testTag("create_parent_room_confirm")
                ) {
                    Text(AppStrings.get("create_parent_room", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showParentRegisterDialog = false }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun RolePortalCard(
    title: String,
    portalName: String,
    desc: String,
    icon: ImageVector,
    accentColor: Color,
    bgGradient: List<Color>,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(bgGradient))
                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = portalName,
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = desc,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
