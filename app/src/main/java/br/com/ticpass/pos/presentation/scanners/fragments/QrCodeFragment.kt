package br.com.ticpass.pos.presentation.scanners.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import br.com.ticpass.pos.R

class QrCodeProcessingFragment : Fragment(R.layout.fragment_qr_code_processing) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}

class QrCodeErrorFragment : Fragment(R.layout.fragment_qr_code_error) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<android.widget.Button>(R.id.btn_retry)?.setOnClickListener {
            // Close this fragment and restart QR scanner
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
            
            // Find LoginChoiceFragment and call startQrLogin
            val choiceFragment = parentFragmentManager.findFragmentById(R.id.login_fragment_container)
            if (choiceFragment is br.com.ticpass.pos.presentation.login.fragments.LoginChoiceFragment) {
                choiceFragment.startQrLogin()
            }
        }
    }
}

class QrCodeSuccessFragment : Fragment(R.layout.fragment_qr_code_success) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}