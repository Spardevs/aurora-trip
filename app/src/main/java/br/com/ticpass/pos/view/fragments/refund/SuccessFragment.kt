package br.com.ticpass.pos.view.fragments.refund
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import br.com.ticpass.pos.R
import android.widget.Button
import android.widget.TextView

class SuccessFragment : Fragment(R.layout.fragment_refund_success) {

    companion object {
        fun newInstance(message: String? = null): SuccessFragment {
            val f = SuccessFragment()
            val args = Bundle()
            args.putString("message", message)
            f.arguments = args
            return f
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tv = view.findViewById<TextView>(R.id.tv_success)
        val btn = view.findViewById<Button>(R.id.btn_finish_success)
        tv.text = arguments?.getString("message") ?: tv.text
        btn.setOnClickListener {
            // fecha Activity chamadora
            activity?.setResult(android.app.Activity.RESULT_OK)
            activity?.finish()
        }
    }
}