package br.com.ticpass.pos.data.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.ticpass.pos.databinding.ActivitySettingsBinding

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.configMenuLayout.setOnClickListener {
            val intent = Intent(this, ConfigMenuProductsActivity::class.java)
            startActivity(intent)
        }
    }
}