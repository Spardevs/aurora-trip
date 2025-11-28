package br.com.ticpass.pos.presentation.common

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import br.com.ticpass.pos.R

class LoginLoadingFragment : Fragment() {

    interface Listener {
        fun onLoadingCancelled()
        fun onLoadingAction(action: String) // usado quando um botão de ação é pressionado (ex: "retry")
    }

    companion object {
        private const val ARG_MESSAGE = "arg_message"
        private const val ARG_CANCELABLE = "arg_cancelable"

        fun newInstance(message: String? = null, cancelable: Boolean = false): LoginLoadingFragment {
            val f = LoginLoadingFragment()
            val args = Bundle()
            args.putString(ARG_MESSAGE, message)
            args.putBoolean(ARG_CANCELABLE, cancelable)
            f.arguments = args
            return f
        }
    }

    private var listener: Listener? = null
    private var initialMessage: String? = null
    private var cancelable: Boolean = false
    private lateinit var messageText: TextView
    private var progressBar: ProgressBar? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialMessage = arguments?.getString(ARG_MESSAGE)
        cancelable = arguments?.getBoolean(ARG_CANCELABLE, false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messageText = view.findViewById(R.id.tv_loading_info)

        messageText.text = initialMessage ?: getString(R.string.loading_default)

        progressBar = view.findViewById(R.id.progressBar)

    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    fun updateMessage(newMessage: String) {
        if (this::messageText.isInitialized) messageText.text = newMessage else initialMessage = newMessage
    }

    fun showProgress(show: Boolean) {
        progressBar?.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Finaliza o loading; chama o listener e/ou remove o fragment.
     * O host pode preferir remover o fragment diretamente.
     */
    fun finishLoading() {
        // notifica host; host decide remover/navegar
        listener?.let {
            // silêncio — host implementa se desejar
        }
    }
}