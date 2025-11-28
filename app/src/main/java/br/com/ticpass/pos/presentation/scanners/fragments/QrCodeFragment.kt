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

        // Encontra o botão de retry e define o listener para reiniciar o scanner
        view.findViewById<android.widget.Button>(R.id.btn_retry)?.setOnClickListener {
            // Navega de volta para o scanner de QR code
            activity?.let { act ->
                if (act is br.com.ticpass.pos.presentation.login.activities.LoginActivity) {
                    // Fecha o fragment atual
                    parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
                    // Inicia o scanner novamente
                    act.startQrLogin()
                }
            }
        }
    }
}

class QrCodeSuccessFragment : Fragment(R.layout.fragment_qr_code_success) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}