package br.com.ticpass.pos.presentation.login.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import br.com.ticpass.pos.R

class MenuFragment : Fragment(R.layout.activity_login_menu) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // A lógica de apresentação foi movida para MenuActivity
        // Esta fragment pode ser usado se necessário em uma arquitetura com fragments
    }
}