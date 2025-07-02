/*           DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
*                   Version 2, December 2004
*
* Copyright (C) 2004 Satoshi Nakamoto <satoshi@bitcoin.org>
*
* Everyone is permitted to copy and distribute verbatim or modified
* copies of this license document, and changing it is allowed as long
* as the name is changed.
*
*            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
*   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
*
*  0. You just DO WHAT THE FUCK YOU WANT TO.
*/

package br.com.ticpass.pos.compose.screens.auth

import ExtendedTheme
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.URLUtil
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.twotone.Email
import androidx.compose.material.icons.twotone.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.ticpass.pos.BuildConfig
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.MainActivity.Companion.appContext
import br.com.ticpass.pos.R
import br.com.ticpass.pos.colorDarkGray
import br.com.ticpass.pos.compose.destinations.EventsScreenDestination
import br.com.ticpass.pos.compose.screens.events.ForYouViewModel
import br.com.ticpass.pos.compose.screens.events.GradientColors
import br.com.ticpass.pos.compose.screens.events.NiaGradientBackground
import br.com.ticpass.pos.compose.screens.product.ProductsViewModel
import br.com.ticpass.pos.compose.utils.CustomGlideImage
import br.com.ticpass.pos.data.APIRepository
import br.com.ticpass.pos.data.AuthManager
import br.com.ticpass.pos.data.event.EventEntity
import br.com.ticpass.pos.data.event.EventRepository
import br.com.ticpass.pos.data.user.Cashier
import br.com.ticpass.pos.data.user.CashierRepository
import br.com.ticpass.pos.dataStore
import br.com.ticpass.pos.noRippleClickable
import com.airbnb.lottie.compose.LottieConstants
import com.google.firebase.analytics.FirebaseAnalytics
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@Composable
fun starBurst() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Row {
            LottieIcon(resId = R.raw.star_burst_1,
                onStart = {},
                onProgress = {},
                onEnd = {},
                iterations = 2,
                speed = 0.6f,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .width(150.dp)
                    .alpha(0.6f)
            )
            LottieIcon(resId = R.raw.star_burst_1,
                onStart = {},
                onProgress = {},
                onEnd = {},
                iterations = 2,
                speed = 1f,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(350.dp)
            )
        }
        Row {
            LottieIcon(resId = R.raw.star_burst_1,
                onStart = {},
                onProgress = {},
                onEnd = {},
                iterations = 2,
                speed = 1.2f,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(250.dp)
            )
        }
    }
}

fun formattedSerial(): String {
    val serial = Build.SERIAL

    val groups = mutableListOf<String>()

    if (serial.length % 2 == 1) {
        val firstGroup = serial.substring(0, 3)
        groups.add(firstGroup)

        val remaining = serial.substring(3)
        val chunkedGroups = remaining.chunked(4)
        groups.addAll(chunkedGroups)
    } else {
        val chunkedGroups = serial.chunked(4)
        groups.addAll(chunkedGroups)
    }

    return groups.joinToString("-")
}

suspend fun downloadImages(
    urls: List<String>,
    dir: File,
): List<String> = coroutineScope {

    val deferredList = urls.map { url ->
        async(Dispatchers.IO) {
            val imageUrl = URL(url)
            val connection = imageUrl.openConnection()
            connection.doInput = true
            connection.connect()

            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val fileName = URLUtil.guessFileName(url, null, null)
            val file = File(dir, fileName)

            try {
                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)

                fileOutputStream.close()
                inputStream.close()
                bitmap.recycle()

                System.gc()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            file.absolutePath
        }
    }

    deferredList.awaitAll()
}

@Composable
fun breathingLogo(
    useDefault: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition("breathingAlpha")
    val breathingAlpha by infiniteTransition.animateFloat(
        label = "breathingAlpha",
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0.2f at 0 with LinearEasing
                1f at 1000 with LinearEasing
                0.2f at 2000 with LinearEasing
            }, repeatMode = RepeatMode.Restart
        )
    )

    val resId = if (BuildConfig.FLAVOR == "pagseguro") {
        R.drawable.pagseguro_logo_full
    } else {
        R.drawable.stone_logo
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(200.dp)
            .height(100.dp)
            .padding(horizontal = 20.dp)
    ) {
        if (useDefault) {
            LottieIcon(resId = R.raw.loading_planet,
                onStart = {},
                onProgress = {},
                onEnd = {},
                iterations = LottieConstants.IterateForever,
                speed = 1f,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
//                    .alpha(breathingAlpha)
            )

        } else {
            Image(
                painter = painterResource(resId),
                contentScale = ContentScale.FillWidth,
                contentDescription = null,
                modifier = Modifier.alpha(breathingAlpha)
            )
        }
    }
}

@Composable
fun FloatingAnimation(
    modifier: Modifier = Modifier, content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "")

    val xOffsetAnimation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 30f, animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val yOffsetAnimation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 20f, animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing), repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    offsetX = xOffsetAnimation
    offsetY = yOffsetAnimation

    Box(contentAlignment = Alignment.Center,
        modifier = modifier.offset { IntOffset(0, offsetY.dp.roundToPx()) }) {
        content()
    }
}

fun onSubmit(
    emailState: TextFieldState,
    passwordState: PasswordState,
    viewModel: ForYouViewModel,
    navigator: DestinationsNavigator,
) {
    if (emailState.isValid && passwordState.isValid) {

        val defaultScope = CoroutineScope(Dispatchers.Default)
        val handler = CoroutineExceptionHandler { _, _ -> }

        defaultScope.launch(handler) {
            try {
                val deviceResponse = withContext(Dispatchers.IO) {
                    viewModel.apiRepository.registerDevice(
                        Build.MODEL, Build.SERIAL
                    )
                }

                if (deviceResponse.error != null) {
                    return@launch
                }

                val authResponse = withContext(Dispatchers.IO) {
                    viewModel.apiRepository.login(
                        emailState.text.replace("\\s".toRegex(), ""),
                        passwordState.text,
                        Build.SERIAL,
                    )
                }

                if (authResponse.error != null) {
                    return@launch
                }

                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.METHOD, "email")
                    putString("email", emailState.text)
                    putString("userId", authResponse.result.user.id)
                    putString("userName", authResponse.result.user.name)
                }
                MainActivity.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)

                onAuthSuccess(
                    authResponse.result.user.id,
                    authResponse.result.user.name,
                    authResponse.result.token,
                    viewModel.cashierRepository,
                    viewModel.apiRepository,
                    viewModel.eventRepository,
                    navigator,
                )
            }
            catch (e: Exception) {
                Log.e("LoginScreen", "error: ${e.message}")
            }
            finally {
                // defaultScope.cancel()
            }
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Destination
@Composable
fun LoginScreen(
    navigator: DestinationsNavigator,
    viewModel: ForYouViewModel = hiltViewModel(),
    productsViewModel: ProductsViewModel,
) {
    val focusRequester = remember { FocusRequester() }
    val emailState by rememberSaveable(stateSaver = EmailStateSaver) {
        mutableStateOf(EmailState(""))
    }
    val passwordState = remember { PasswordState() }

    var shouldScanQrCode by remember {
        mutableStateOf(
            false
        )
    }

    var isSerialVisible by remember {
        mutableStateOf(
            false
        )
    }

    val transition = updateTransition(
        targetState = isSerialVisible, label = null
    )

    val gearOffset by transition.animateIntOffset(transitionSpec = { tween(300) },
        label = "gearOffset",
        targetValueByState = { isSerialVisible ->
            if (!isSerialVisible) IntOffset(x = 8, y = 8) else IntOffset(x = -8, y = 0)
        })

    val gearScale by transition.animateFloat(transitionSpec = { tween(300) },
        label = "gearScale",
        targetValueByState = { isSerialVisible ->
            if (!isSerialVisible) 0.95f else 0.95f
        })

    BackHandler(true) {}

//    val satisfyFontFamily = FontFamily(Font(R.font.satisfy))
//
//    Box(
//        contentAlignment = Alignment.Center,
//        modifier = Modifier.fillMaxSize()
//    ){
//        LottieIcon(
//            resId = R.raw.nightsky,
//            onStart = {},
//            onEnd = {},
//            speed = 2.5f,
//            scale = 1.1f,
//            iterations = LottieConstants.IterateForever,
//            contentScale = ContentScale.Fit,
//            modifier = Modifier
//                .fillMaxSize()
//        )
//
//        Column(
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Box(
//                contentAlignment = Alignment.Center
//            ){
//                LottieIcon(
//                    resId = R.raw.shiny,
//                    onStart = {},
//                    onEnd = {},
//                    scale = 2.5f,
//                    iterations = LottieConstants.IterateForever,
//                    contentScale = ContentScale.FillHeight,
//                    modifier = Modifier
//                        .height(150.dp)
//                )
//
//                LottieIcon(
//                    resId = R.raw.btc_loader,
//                    onStart = {},
//                    onEnd = {},
//                    speed = 2.5f,
//                    iterations = 1,
//                    contentScale = ContentScale.FillWidth,
//                    modifier = Modifier
//                        .width(150.dp)
//                        .padding(bottom = 10.dp)
//                )
//            }
//
//            Text(
//                text = "carregando...",
//                color = Color.White,
//                fontWeight = FontWeight.ExtraBold,
//                fontSize = 45.sp,
//                lineHeight = 45.sp,
//                fontFamily = satisfyFontFamily,
//                modifier = Modifier.padding(start = 20.dp)
//            )
//        }
//    }
//
//    return

    NiaGradientBackground(
        gradientColors = GradientColors(
            bottom = Color.White,
            top = ExtendedTheme.colors.primary_500.copy(alpha = 0.4f),
            container = ExtendedTheme.colors.secondary_200,
        )
    ) {

        LottieIcon(resId = R.raw.shining_stars,
            onStart = {},
            onEnd = {},
            speed = 2.5f,
            iterations = LottieConstants.IterateForever,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.5f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {

            AnimatedVisibility(
                visible = isSerialVisible, enter = fadeIn(), exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = Color.White,
                    modifier = Modifier
                        .padding(end = 7.dp)
                        .shadow(
                            shape = RoundedCornerShape(5.dp),
                            elevation = 1.dp,
                        )
                ) {
                    Text(
                        text = formattedSerial(),
                        style = MaterialTheme.typography.titleSmall,
                        color = colorDarkGray,
                        fontWeight = FontWeight(400),
                        modifier = Modifier.padding(7.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End,
                modifier = Modifier.height(80.dp)
            ) {
                Box(modifier = Modifier
                    .padding(end = 20.dp)
                    .size(25.dp)
                    .noRippleClickable {
                        isSerialVisible = !isSerialVisible
                    }) {
                    Image(painter = painterResource(R.drawable.gear_background),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ExtendedTheme.colors.primary),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { gearOffset }
                            .scale(gearScale)
                            .align(Alignment.Center)
                            .alpha(0.6f))
                    Image(
                        painter = painterResource(R.drawable.gear),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Box(
            contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()
            ) {
                FloatingAnimation(
                    modifier = Modifier
                ) {
                    CustomGlideImage(model = R.drawable.falling_2,
                        contentDescription = null,
                        placeholder = painterResource(id = R.drawable.falling_2),
                        contentScale = ContentScale.FillWidth,
                        encodeQuality = 100,
                        modifier = Modifier
                            .width(270.dp)
                            .height(270.dp)
                            .offset { IntOffset(x = 0, y = -250) })
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(
                            topStart = 45.dp,
                            topEnd = 45.dp,
                        ),
                    )
                    .fillMaxWidth()
                    .padding(
                        top = 20.dp,
                        end = 20.dp,
                        bottom = 30.dp,
                        start = 20.dp,
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        Text(
                            text = "Pousou aqui? Faça o login",
                            fontWeight = FontWeight.Bold,
                            color = ExtendedTheme.colors.primary_900,
                            fontSize = 16.sp,
                        )
                    }

                    var isEmailLogin by remember {
                        mutableStateOf(
                            false
                        )
                    }

                    if(isEmailLogin) {
                        Email(
                            emailState = emailState,
                            imeAction = ImeAction.Next,
                            onImeAction = {
                                onSubmit(
                                    emailState,
                                    passwordState,
                                    viewModel,
                                    navigator
                                )
                            },
                            modifier = Modifier.focusRequester(focusRequester)
                        )

                        Password(label = stringResource(id = R.string.password),
                            passwordState = passwordState,
                            modifier = Modifier.focusRequester(focusRequester),
                            onImeAction = {
                                onSubmit(
                                    emailState,
                                    passwordState,
                                    viewModel,
                                    navigator
                                )
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(2f / 6f)
                            ) {
                                Button(
                                    onClick = {
                                        isEmailLogin = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.LightGray
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "voltar",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(5.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(4f / 6f)
                            ) {
                                Button(
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ExtendedTheme.colors.primary
                                    ), onClick = {
                                        onSubmit(
                                            emailState,
                                            passwordState,
                                            viewModel,
                                            navigator
                                        )
                                    }, modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "entrar",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(5.dp)
                                    )
                                }
                            }
                        }
                    }
                    else {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(4f / 5f)
                                    .padding(end = 15.dp)
                            ) {
                                Button(
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ExtendedTheme.colors.primary
                                    ), onClick = {
                                        isEmailLogin = true
                                    }, modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.TwoTone.Email,
                                        contentDescription = stringResource(id = R.string.hide_password),
                                        tint = Color.White,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                    Text(
                                        text = "Email",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(5.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(4f / 5f)
                            ) {

                                Button(
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ExtendedTheme.colors.primary
                                    ), onClick = {
                                        shouldScanQrCode = true
                                    }, modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.TwoTone.QrCode,
                                        contentDescription = stringResource(id = R.string.hide_password),
                                        tint = Color.White,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                    Text(
                                        text = "QR Code",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (shouldScanQrCode) {
            BarcodeScannerScreen(
                onLoginSuccess = { userId, userName, token ->
                    val bundle = Bundle().apply {
                        putString(FirebaseAnalytics.Param.METHOD, "qrcode")
                        putString("userId", userId)
                        putString("userName", userName)
                    }
                    MainActivity.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)

                    onAuthSuccess(
                        userId,
                        userName,
                        token,
                        viewModel.cashierRepository,
                        viewModel.apiRepository,
                        viewModel.eventRepository,
                        navigator,
                    )
                },
                onDismiss = {
                    shouldScanQrCode = false
                },
            )
        }
    }
}

fun onAuthSuccess(
    userId: String,
    userName: String,
    token: String,
    cashierRepository: CashierRepository,
    apiRepository: APIRepository,
    eventRepository: EventRepository,
    navigator: DestinationsNavigator,
) {
    val authManager = AuthManager(appContext.dataStore)
    val defaultScope = CoroutineScope(Dispatchers.Default)
    val handler = CoroutineExceptionHandler { _, _ -> }

    defaultScope.launch(handler) {
        try {
            withContext(Dispatchers.IO) {
                authManager.setJWT(
                    token
                )

                cashierRepository.insertUser(
                    Cashier(
                        id = userId,
                        name = userName,
                    )
                )
            }


            /*
            * EVENT LIST
            * */

            val eventsResponse = withContext(Dispatchers.IO) {
                val foo = apiRepository.getEvents(
                    userId,
                    token,
                )

                Log.d("LoginScreen", "foo: $foo")

                foo
            }

            Log.d("LoginScreen", "eventsResponse: ${eventsResponse.result.items}")

            if (eventsResponse.status != 200 || eventsResponse.result.items.isEmpty()) {
                return@launch
            }

            val menuImagesDir = withContext(Dispatchers.IO) {
                val baseDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES) as File
                val imagesDir = File(baseDir, "events")
                imagesDir.mkdirs()

                downloadImages(
                    eventsResponse.result.items.map { it.ticket },
                    imagesDir,
                )

                imagesDir
            }

            withContext(Dispatchers.IO) {
                eventRepository.insertMany(eventsResponse.result.items.map { event ->

                    val fileName = URLUtil.guessFileName(event.ticket, null, null)
                    val imagePath = "${menuImagesDir}/${fileName}"
                    val file = File(imagePath)

                    authManager.setAcquirerPaymentEnabled(event.isAcquirerPaymentEnabled)
                    authManager.setMultiPaymentEnabled(event.isMultiPaymentEnabled)

                    EventEntity(
                        id = event.id,
                        name = event.name,
                        logo = file.absolutePath,
                        dateStart = event.dateStart,
                        dateEnd = event.dateEnd,
                        isSelected = false,
                        details = event.details,
                        printingPriceEnabled = event.isPrintTicket,
                        pin = event.pin,
                        mode = event.mode,
                        ticketsPrintingGrouped = false,
                        hasProducts = event.products.isNotEmpty(),
                        isCreditEnabled = event.isCreditEnabled,
                        isDebitEnabled = event.isDebitEnabled,
                        isPIXEnabled = event.isPIXEnabled,
                        isVREnabled = event.isVREnabled,
                        isLnBTCEnabled = event.isLnBTCEnabled,
                        isCashEnabled = event.isCashEnabled,
                        isAcquirerPaymentEnabled = event.isAcquirerPaymentEnabled,
                        isMultiPaymentEnabled = event.isMultiPaymentEnabled,
                    )
                })
            }

            withContext(Dispatchers.Main) {
                navigator.navigate(
                    EventsScreenDestination()
                )
            }
        }
        catch (e: Exception) {
            Log.d("LoginScreen", "error: ${e.message}")
        }
        finally {
            // defaultScope.cancel()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Email(
    emailState: TextFieldState = remember { EmailState() },
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    val containerColor = Color.Gray.copy(0.1f)
    OutlinedTextField(
        singleLine = true,
        shape = RoundedCornerShape(40.dp),
        value = emailState.text,
        onValueChange = {
            emailState.text = it
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Gray,
            unfocusedTextColor = Color.Gray,
            disabledTextColor = Color.Gray,
            errorTextColor = Color.Gray,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            errorContainerColor = Color.Gray.copy(0.1f),
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = Color.Transparent,
            focusedLabelColor = Color.Transparent,
            unfocusedLabelColor = Color.Transparent,
            disabledLabelColor = Color.Transparent,
            errorLabelColor = Color.Transparent,
            focusedPlaceholderColor = Color.Transparent,
            unfocusedPlaceholderColor = Color.Transparent,
            disabledPlaceholderColor = Color.Transparent,
            errorPlaceholderColor = Color.Transparent,
        ),
        placeholder = {
            Text(
                text = stringResource(id = R.string.email),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight(400),
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = modifier.fillMaxWidth()
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                emailState.onFocusChange(focusState.isFocused)
                if (!focusState.isFocused) {
                    emailState.enableShowErrors()
                }
            },
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight(400),
        ),
        isError = emailState.showErrors(),
        supportingText = {
            emailState.getError()?.let { error -> TextFieldError(textError = error) }
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = imeAction, keyboardType = KeyboardType.Email
        ),
        keyboardActions = KeyboardActions(onDone = {
            onImeAction()
        }),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Password(
    label: String,
    passwordState: TextFieldState,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    val showPassword = rememberSaveable { mutableStateOf(false) }
    val containerColor = Color.Gray.copy(0.1f)
    OutlinedTextField(
        shape = RoundedCornerShape(40.dp),
        value = passwordState.text,
        onValueChange = {
            passwordState.text = it
            passwordState.enableShowErrors()
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Gray,
            unfocusedTextColor = Color.Gray,
            disabledTextColor = Color.Gray,
            errorTextColor = Color.Gray,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            errorContainerColor = Color.Gray.copy(0.1f),
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = Color.Transparent,
            focusedLabelColor = Color.Transparent,
            unfocusedLabelColor = Color.Transparent,
            disabledLabelColor = Color.Transparent,
            errorLabelColor = Color.Transparent,
            focusedPlaceholderColor = Color.Transparent,
            unfocusedPlaceholderColor = Color.Transparent,
            disabledPlaceholderColor = Color.Transparent,
            errorPlaceholderColor = Color.Transparent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                passwordState.onFocusChange(focusState.isFocused)
                if (!focusState.isFocused) {
                    passwordState.enableShowErrors()
                }
            },
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight(400),
        ),
        placeholder = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight(400),
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = modifier.fillMaxWidth()
            )
        },
        trailingIcon = {
            if (showPassword.value) {
                IconButton(onClick = { showPassword.value = false }) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = stringResource(id = R.string.hide_password),
                        tint = Color.Gray,
                    )
                }
            } else {
                IconButton(onClick = { showPassword.value = true }) {
                    Icon(
                        imageVector = Icons.Outlined.VisibilityOff,
                        contentDescription = stringResource(id = R.string.show_password),
                        tint = Color.Gray,
                    )
                }
            }
        },
        visualTransformation = if (showPassword.value) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        isError = passwordState.showErrors(),
        supportingText = {
            passwordState.getError()?.let { error -> TextFieldError(textError = error) }
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = imeAction, keyboardType = KeyboardType.Password
        ),
        keyboardActions = KeyboardActions(onDone = {
            onImeAction()
        }),
    )
}

@Composable
fun TextFieldError(textError: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = textError,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.error
        )
    }
}