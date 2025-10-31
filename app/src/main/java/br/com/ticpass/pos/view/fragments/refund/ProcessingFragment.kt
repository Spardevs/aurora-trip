package br.com.ticpass.pos.view.fragments.refund

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import br.com.ticpass.pos.R

class ProcessingFragment : Fragment(R.layout.fragment_refund_processing) {

    companion object {
        fun newInstance(message: String? = null): ProcessingFragment {
            val f = ProcessingFragment()
            val args = Bundle()
            args.putString("message", message)
            f.arguments = args
            return f
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val message = arguments?.getString("message")
        view.findViewById<TextView?>(R.id.tv_processing)?.text = message ?: "Processando..."
    }
}