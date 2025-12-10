package br.com.ticpass.pos.presentation.login.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import br.com.ticpass.pos.presentation.login.activities.LoginMenuActivity
import br.com.ticpass.pos.presentation.scanners.contracts.QrScannerContract
import br.com.ticpass.pos.presentation.scanners.fragments.QrCodeErrorFragment
import br.com.ticpass.pos.presentation.scanners.fragments.QrCodeProcessingFragment
import br.com.ticpass.pos.presentation.scanners.fragments.QrCodeSuccessFragment
import br.com.ticpass.pos.presentation.scanners.states.QrLoginState
import br.com.ticpass.pos.presentation.scanners.viewmodels.QrLoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fragment showing login method choices (email/username or QR code)
 */
@AndroidEntryPoint
class LoginChoiceFragment : Fragment(R.layout.fragment_login_choice) {

    private val qrViewModel: QrLoginViewModel by activityViewModels()

    private lateinit var qrScannerLauncher: ActivityResultLauncher<Unit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        qrScannerLauncher = registerForActivityResult(QrScannerContract()) { qrText ->
            if (qrText == null) {
                Toast.makeText(requireContext(), R.string.qr_scan_cancelled, Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            qrViewModel.signInWithQr(qrText)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.email_login_button).setOnClickListener {
            navigateToCredentialLogin()
        }

        view.findViewById<Button>(R.id.qr_code_login_button).setOnClickListener {
            startQrLogin()
        }

        observeQrLoginState()
    }

    private fun navigateToCredentialLogin() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.login_fragment_container, CredentialLoginFragment())
            .addToBackStack(null)
            .commit()
        
        // Notify activity to animate falling image
        (activity as? LoginHostCallback)?.onShowCredentialForm()
    }

    fun startQrLogin() {
        qrScannerLauncher.launch(Unit)
    }

    private fun observeQrLoginState() {
        viewLifecycleOwner.lifecycleScope.launch {
            qrViewModel.state.collect { state ->
                when (state) {
                    is QrLoginState.Processing -> showQrFragment(QrCodeProcessingFragment())
                    is QrLoginState.Error -> {
                        val frag = QrCodeErrorFragment().apply {
                            arguments = Bundle().apply { putString("qr_error_message", state.message) }
                        }
                        showQrFragment(frag)
                    }
                    is QrLoginState.Success -> {
                        showQrFragment(QrCodeSuccessFragment())
                        launch {
                            delay(800)
                            navigateToMenuActivity()
                        }
                    }
                    is QrLoginState.Idle -> removeQrFragment()
                }
            }
        }
    }

    private fun showQrFragment(fragment: Fragment) {
        removeQrFragment()
        parentFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment, TAG_QR_FRAGMENT)
            .commitAllowingStateLoss()
    }

    private fun removeQrFragment() {
        parentFragmentManager.findFragmentByTag(TAG_QR_FRAGMENT)?.let {
            parentFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
        }
    }

    private fun navigateToMenuActivity() {
        removeQrFragment()
        startActivity(android.content.Intent(requireContext(), LoginMenuActivity::class.java))
        activity?.finish()
    }

    companion object {
        private const val TAG_QR_FRAGMENT = "qr_loading_fragment"
    }
}

/**
 * Callback interface for LoginHostActivity
 */
interface LoginHostCallback {
    fun onShowCredentialForm()
    fun onHideCredentialForm()
}
