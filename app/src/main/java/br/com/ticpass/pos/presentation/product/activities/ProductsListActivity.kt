package br.com.ticpass.pos.presentation.product.activities

import android.os.Bundle
import br.com.ticpass.pos.presentation.shared.activities.BaseDrawerActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductsListActivity : BaseDrawerActivity() {
    override val hasMenu: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inserir o fragment de login no content_frame
//        val loginFragment = LoginFragment() // Substitua pelo seu fragment real
//        setContentFragment(loginFragment)
    }

     override fun showLogoInDrawerToolbar(): Boolean = true
}