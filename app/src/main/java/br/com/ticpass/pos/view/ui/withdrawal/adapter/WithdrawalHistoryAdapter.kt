package br.com.ticpass.pos.view.ui.withdrawal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.databinding.ItemWithdrawalHistoryBinding
import br.com.ticpass.pos.viewmodel.withdrawal.Withdrawal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class WithdrawalHistoryAdapter :
    ListAdapter<Withdrawal, WithdrawalHistoryAdapter.WithdrawalViewHolder>(WithdrawalDiffCallback()) {

    class WithdrawalViewHolder(private val binding: ItemWithdrawalHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(withdrawal: Withdrawal) {
            binding.textViewAmount.text = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(withdrawal.amount)
            binding.textViewDate.text = SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date(withdrawal.timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WithdrawalViewHolder {
        val binding = ItemWithdrawalHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WithdrawalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WithdrawalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WithdrawalDiffCallback : DiffUtil.ItemCallback<Withdrawal>() {
        override fun areItemsTheSame(oldItem: Withdrawal, newItem: Withdrawal): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: Withdrawal, newItem: Withdrawal): Boolean {
            return oldItem == newItem
        }
    }
}