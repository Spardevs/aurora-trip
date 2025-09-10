package br.com.ticpass.pos.data.acquirers.workers.jobs

import br.com.ticpass.pos.queue.core.QueueItemStatus
import br.com.ticpass.pos.queue.processors.payment.data.PaymentProcessingStorage
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingQueueItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class CleanTransactions(
    private val paymentStorage: PaymentProcessingStorage
) {

    companion object {
        private const val TAG = "CleanTransactions"
    }

    /**
     * Executa a limpeza das transações completadas
     */
    fun execute() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cleanCompletedTransactions()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Erro ao limpar transações completadas")
            }
        }
    }

    /**
     * Verifica se todas as transações estão completas e limpa a fila
     */
    private suspend fun cleanCompletedTransactions() {
        // Busca todas as transações com status COMPLETED
        val completedTransactions = paymentStorage.getAllByStatus(
            listOf(QueueItemStatus.COMPLETED)
        )

        // Busca todas as transações com outros status para verificar se há pendências
        val pendingTransactions = paymentStorage.getAllByStatus(
            listOf(
                QueueItemStatus.PENDING,
                QueueItemStatus.PROCESSING,
                QueueItemStatus.FAILED,
                QueueItemStatus.CANCELLED
            )
        )

        if (pendingTransactions.isEmpty() && completedTransactions.isNotEmpty()) {
            // Se não há transações pendentes e existem transações completadas, limpa a fila
            Timber.tag(TAG).i("Limpando ${completedTransactions.size} transações completadas")
            paymentStorage.removeByStatus(listOf(QueueItemStatus.COMPLETED))
            Timber.tag(TAG).i("Fila limpa com sucesso")
        } else if (pendingTransactions.isNotEmpty()) {
            Timber.tag(TAG).d(
                "Existem ${pendingTransactions.size} transações pendentes. " +
                        "A limpeza será realizada quando todas estiverem completas."
            )
        } else {
            Timber.tag(TAG).d("Nenhuma transação para limpar")
        }
    }

    /**
     * Verifica se todas as transações estão completas (método síncrono para verificações rápidas)
     */
    suspend fun areAllTransactionsCompleted(): Boolean {
        val pendingTransactions = paymentStorage.getAllByStatus(
            listOf(
                QueueItemStatus.PENDING,
                QueueItemStatus.PROCESSING,
                QueueItemStatus.FAILED,
                QueueItemStatus.CANCELLED
            )
        )
        return pendingTransactions.isEmpty()
    }

    /**
     * Limpa transações completadas de forma forçada (útil para manutenção)
     */
    suspend fun forceCleanCompletedTransactions() {
        try {
            val completedTransactions = paymentStorage.getAllByStatus(
                listOf(QueueItemStatus.COMPLETED)
            )

            if (completedTransactions.isNotEmpty()) {
                Timber.tag(TAG).i("Limpando forçadamente ${completedTransactions.size} transações completadas")
                paymentStorage.removeByStatus(listOf(QueueItemStatus.COMPLETED))
                Timber.tag(TAG).i("Limpeza forçada concluída")
            } else {
                Timber.tag(TAG).d("Nenhuma transação completa para limpar forçadamente")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Erro na limpeza forçada de transações")
        }
    }
}