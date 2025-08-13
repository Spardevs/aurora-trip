package br.com.ticpass.pos.data.activity

import android.os.Bundle
import br.com.ticpass.pos.databinding.ActivitySupportBinding

class SupportActivity : BaseActivity() {

    private lateinit var binding: ActivitySupportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Fale Conosco"
    }
}