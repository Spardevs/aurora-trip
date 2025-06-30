/*
 * Copyright (c) 2025 Ticpass. All rights reserved.
 *
 * PROPRIETARY AND CONFIDENTIAL
 *
 * This software is the confidential and proprietary information of Ticpass
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Ticpass.
 *
 * Unauthorized copying, distribution, or use of this software, via any medium,
 * is strictly prohibited without the express written permission of Ticpass.
 */

package br.com.ticpass.pos.view.ui.account

import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.gplayapi.helpers.AuthHelper
import br.com.ticpass.pos.AuroraApp
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.event.AuthEvent
import br.com.ticpass.pos.databinding.FragmentGoogleBinding
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import br.com.ticpass.pos.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GoogleFragment : BaseFragment<FragmentGoogleBinding>() {

    private val viewModel: AuthViewModel by activityViewModels()

    companion object {
        const val EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup"
        const val AUTH_TOKEN = "oauth_token"
        private const val JS_SCRIPT =
            "(function() { return document.getElementById('profileIdentifier').innerHTML; })();"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cookieManager = CookieManager.getInstance()

        binding.webview.apply {
            cookieManager.removeAllCookies(null)
            cookieManager.acceptThirdPartyCookies(this)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)

                    if (newProgress != 0) {
                        binding.progressBar.also {
                            it.isVisible = newProgress < 100
                            it.isIndeterminate = false
                            it.max = 100
                            it.progress = newProgress
                        }
                    } else {
                        binding.progressBar.isIndeterminate = true
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val cookies = CookieManager.getInstance().getCookie(url)
                    // cookies can be null if there is an error
                    if (cookies != null) {
                        val cookieMap = br.com.ticpass.pos.util.AC2DMUtil.parseCookieString(cookies)
                        if (cookieMap.isNotEmpty() && cookieMap[AUTH_TOKEN] != null) {
                            val oauthToken = cookieMap[AUTH_TOKEN]
                            evaluateJavascript(JS_SCRIPT) {
                                val email = it.replace("\"".toRegex(), "")
                                viewModel.buildAuthData(view.context, email, oauthToken)
                            }
                        }
                    }
                }
            }

            settings.apply {
                allowContentAccess = true
                domStorageEnabled = true
                javaScriptEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = false
            }
            loadUrl(EMBEDDED_SETUP_URL)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            AuroraApp.events.authEvent.collect { event ->
                if (event is AuthEvent.GoogleLogin) onEventReceived(event)
            }
        }
    }

    private fun onEventReceived(event: AuthEvent.GoogleLogin) {
        if (event.success) {
            viewModel.buildGoogleAuthData(event.email, event.token, AuthHelper.Token.AAS)
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_aas_token_failed),
                Toast.LENGTH_LONG
            ).show()
        }

        findNavController().navigate(
            GoogleFragmentDirections.actionGoogleFragmentToSplashFragment()
        )
    }
}
