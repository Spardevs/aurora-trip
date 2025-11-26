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
    }
}

class QrCodeSuccessFragment : Fragment(R.layout.fragment_qr_code_success) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}