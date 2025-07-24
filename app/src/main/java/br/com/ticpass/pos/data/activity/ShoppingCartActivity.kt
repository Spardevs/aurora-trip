package br.com.ticpass.pos.data.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import br.com.ticpass.pos.R

class ShoppingCartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_cart)

        val btn = findViewById<MaterialButton>(R.id.btnConfirm)
        btn?.apply {
            isClickable = true
            bringToFront()
            setOnClickListener {
                Log.d("ShoppingCartActivity", "CONFIRM CLICK!")
                Toast.makeText(this@ShoppingCartActivity, "Confirm clicado!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
