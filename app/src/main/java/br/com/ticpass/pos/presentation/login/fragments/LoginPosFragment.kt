package br.com.ticpass.pos.presentation.login.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import br.com.ticpass.pos.presentation.login.viewmodels.LoginPosViewModel

@AndroidEntryPoint
class LoginPosFragment : Fragment() {

    private val vm: LoginPosViewModel by viewModels()
    private lateinit var adapter: LoginPosAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            = inflater.inflate(R.layout.fragment_pos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.pos_recycler_view)
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = PosAdapter(
            onClick = { pos ->
                // save selection
                val prefs = requireContext().getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
                prefs.edit {
                    putString("pos_id", pos.id)
                    putString("pos_name", "${pos.prefix} ${pos.sequence}")
                    putLong("pos_commission", pos.commission)
                }
                // navigate to confirm screen or whatever
            },
            onLongClick = { pos ->
                // show close dialog and call close via repository / api if desired
            }
        )
        recycler.adapter = adapter

        val prefs = requireContext().getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val menuId = prefs.getString("selected_menu_id", null)
        val auth = prefs.getString("proxy_credentials", "") ?: ""
        val cookie = prefs.getString("pos_access_token", "") ?: ""

        if (menuId == null) {
            Toast.makeText(requireContext(), "Menu não selecionado", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            vm.uiState.collectLatest { state ->
                when (state) {
                    is PosUiState.Loading -> { /* show progress */ }
                    is PosUiState.Success -> adapter.setItems(state.posList)
                    is PosUiState.Empty -> {
                        adapter.setItems(emptyList())
                    }
                    is PosUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // start observing & refresh
        vm.observeMenu(menuId, auth, cookie)
    }
}