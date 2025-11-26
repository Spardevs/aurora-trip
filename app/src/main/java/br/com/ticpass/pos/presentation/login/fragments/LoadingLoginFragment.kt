package br.com.ticpass.pos.presentation.login.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.ticpass.pos.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingLoginFragment : Fragment() {

    interface LoadingListener {
        fun onLoadingFinished()
    }

    private var listener: LoadingListener? = null

    companion object {
        private const val ARG_MESSAGE = "arg_message"
        private const val DEFAULT_MESSAGE = "Carregando..."

        fun newInstance(message: String?): LoadingLoginFragment {
            val f = LoadingLoginFragment()
            val args = Bundle()
            args.putString(ARG_MESSAGE, message)
            f.arguments = args
            return f
        }
    }

    private var message: String? = null
    private lateinit var messageTextView: TextView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // host activity can implement LoadingListener to be notificado quando o loading terminar
        if (context is LoadingListener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        message = arguments?.getString(ARG_MESSAGE) ?: DEFAULT_MESSAGE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_loading_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messageTextView = view.findViewById(R.id.textView)
        messageTextView.text = message ?: DEFAULT_MESSAGE

        // Simula carregamento de 5 segundos (exemplo). Em produção, substitua pela lógica real.
        lifecycleScope.launch {
            delay(5000L)
            if (isAdded) {
                listener?.onLoadingFinished()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}