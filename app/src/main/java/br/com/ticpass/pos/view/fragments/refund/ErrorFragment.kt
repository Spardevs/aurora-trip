package br.com.ticpass.pos.view.fragments.refund

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import br.com.ticpass.pos.R
import android.widget.Button
import android.widget.TextView

class ErrorFragment : Fragment(R.layout.fragment_refund_error) {

    companion object {
        fun newInstance(message: String? = null): ErrorFragment {
            val f = ErrorFragment()
            val args = Bundle()
            args.putString("message", message)
            f.arguments = args
            return f
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tv = view.findViewById<TextView>(R.id.tv_error)
        val btn = view.findViewById<Button>(R.id.btn_retry)
        tv.text = arguments?.getString("message") ?: tv.text
        btn.setOnClickListener {
            // remover fragment de erro e retomar scanner
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
            (activity as? br.com.ticpass.pos.data.activity.BarcodeScannerActivity)?.resumeScannerAfterError()
        }
    }

}