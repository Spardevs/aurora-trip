package br.com.ticpass.pos.presentation.login.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import br.com.ticpass.pos.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.domain.pos.model.Pos
import br.com.ticpass.pos.presentation.login.activities.LoginConfirmActivity
import br.com.ticpass.pos.presentation.login.adapters.LoginPosAdapter
import br.com.ticpass.pos.presentation.login.states.LoginPosUiState
import br.com.ticpass.pos.presentation.login.viewmodels.LoginPosViewModel

@AndroidEntryPoint
class LoginPosFragment : Fragment() {

    private val vm: LoginPosViewModel by viewModels()
    private lateinit var adapter: LoginPosAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            = inflater.inflate(R.layout.activity_login_pos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.pos_recycler_view)
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = LoginPosAdapter(
            onClick = { pos ->
                // save selection
                val prefs = requireContext().getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
                prefs.edit {
                    putString("pos_id", pos.id)
                    putString("pos_name", "${pos.prefix} ${pos.sequence}")
                    putLong("pos_commission", pos.commission)
                }
                startActivity(Intent(requireContext(), LoginConfirmActivity::class.java))
                // navigate to confirm screen or whatever
            },
            onLongClick = { pos ->
                showClosePosDialog(pos)
            }
        )
        recycler.adapter = adapter

        val prefs = requireContext().getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        // prefer argument first, then fallback to saved prefs
        val menuId = arguments?.getString(ARG_MENU_ID) ?: prefs.getString("selected_menu_id", null)
        val auth = prefs.getString("proxy_credentials", "") ?: ""
        val cookie = prefs.getString("pos_access_token", "") ?: ""

        if (menuId == null) {
            Toast.makeText(requireContext(), "Menu não selecionado", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            vm.uiState.collectLatest { state ->
                when (state) {
                    is LoginPosUiState.Loading -> { /* show progress */ }
                    is LoginPosUiState.Success -> adapter.setItems(state.posList)
                    is LoginPosUiState.Empty -> adapter.setItems(emptyList())
                    is LoginPosUiState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Load first page initially
        vm.loadPosList(menuId, auth, cookie, page = 1)
    }

    companion object {
        private const val ARG_MENU_ID = "menu_id"
        fun newInstance(menuId: String): LoginPosFragment {
            return LoginPosFragment().apply {
                arguments = Bundle().apply { putString(ARG_MENU_ID, menuId) }
            }
        }
    }

    private fun showClosePosDialog(pos: Pos) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_close_pos_confirmation, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnYes).setOnClickListener {
            lifecycleScope.launch {
                vm.closePosSession(pos.session?.id ?: throw IllegalStateException("Session ID is null"))
            }
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnNo).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}