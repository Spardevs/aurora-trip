package br.com.ticpass.pos.util

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.ticpass.pos.feature.payment.PaymentProcessingViewModel
import br.com.ticpass.pos.feature.payment.PaymentState
import br.com.ticpass.pos.payment.events.PaymentEventHandler
import br.com.ticpass.pos.payment.models.SystemPaymentMethod
import br.com.ticpass.pos.payment.view.TimeoutCountdownView
import br.com.ticpass.pos.queue.processors.payment.models.PaymentProcessingEvent
import br.com.ticpass.pos.view.ui.shoppingCart.ShoppingCartManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

class PaymentFragmentUtils @Inject constructor(
    private val shoppingCartManager: ShoppingCartManager
) {

    companion object {
        /**
         * Formata um valor Double para moeda brasileira (R$)
         */
        fun formatCurrency(value: Double): String {
            val format = NumberFormat.getCurrencyInstance(
                Locale.Builder()
                    .setLanguage("pt")
                    .setRegion("BR")
                    .build()
            )
            return format.format(value)
        }

        /**
         * Cria um PaymentEventHandler para uso em Fragments
         */
        fun createPaymentEventHandler(
            context: Context,
            statusTextView: TextView? = null,
            infoTextView: TextView? = null,
            imageView: ImageView? = null,
            timeoutCountdownView: TimeoutCountdownView? = null
        ): PaymentEventHandler {
            return PaymentEventHandler(
                context = context,
                dialogEventTextView = infoTextView ?: TextView(context),
                dialogQRCodeImageView = imageView ?: ImageView(context),
                dialogTimeoutCountdownView = timeoutCountdownView ?: TimeoutCountdownView(context)
            )
        }

        /**
         * Configura os observers para monitorar o estado do pagamento e eventos do processador
         *
         * @param fragment Fragment que está observando
         * @param paymentViewModel ViewModel de processamento de pagamento
         * @param paymentEventHandler Handler para eventos de pagamento
         * @param statusTextView TextView para exibir status
         * @param onSuccess Callback executado quando pagamento é bem-sucedido
         * @param onError Callback executado quando há erro (recebe mensagem de erro)
         * @param onCancelled Callback executado quando pagamento é cancelado
         * @param isPix Se true, trata eventos específicos de PIX
         */
        fun setupPaymentObservers(
            fragment: Fragment,
            paymentViewModel: PaymentProcessingViewModel,
            paymentEventHandler: PaymentEventHandler,
            statusTextView: TextView,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
            onCancelled: () -> Unit,
            isPix: Boolean = false
        ) {
            // Observer para eventos do processador (detalhes técnicos do pagamento)
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    paymentViewModel.paymentProcessingEvents.collect { event ->
                        paymentEventHandler.handlePaymentEvent(event)
                        if (isPix) {
                            handlePixPaymentEvents(event, statusTextView)
                        } else {
                            handleCardPaymentEvents(event, statusTextView)
                        }
                    }
                }
            }

            // Observer para estado geral do pagamento (sucesso, erro, cancelado, etc)
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    paymentViewModel.paymentState.collect { state ->
                        when (state) {
                            is PaymentState.Processing -> {
                                // Só atualiza se não houver mensagem específica já exibida
                                if (statusTextView.text.isNullOrEmpty() ||
                                    statusTextView.text.contains("Aprovado") ||
                                    statusTextView.text.contains("Erro") ||
                                    statusTextView.text.contains("Cancelado")
                                ) {
                                    statusTextView.text = "Processando pagamento..."
                                }
                            }

                            is PaymentState.Success -> {
                                onSuccess()
                            }

                            is PaymentState.Error -> {
                                onError(state.errorMessage)
                            }

                            is PaymentState.Cancelled -> {
                                onCancelled()
                            }

                            is PaymentState.Idle -> {
                                statusTextView.text = "Pronto para iniciar pagamento"
                            }

                            is PaymentState.Initializing -> {
                                statusTextView.text = "Inicializando sistema de pagamento..."
                            }
                        }
                    }
                }
            }

            // Observer para eventos de UI (dialogs, inputs, etc)
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    paymentViewModel.uiEvents.collect { event ->
                        paymentEventHandler.handleUiEvent(event)
                    }
                }
            }
        }

        /**
         * Trata eventos específicos de pagamento PIX
         */
        private fun handlePixPaymentEvents(event: PaymentProcessingEvent, statusTextView: TextView) {
            when (event) {
                is PaymentProcessingEvent.QRCODE_SCAN -> {
                    statusTextView.text = "QR Code gerado - Aguardando pagamento"
                }
                is PaymentProcessingEvent.PIX_QRCODE_GENERATED -> {
                    statusTextView.text = "QR Code PIX gerado - Aguardando pagamento"
                }
                is PaymentProcessingEvent.TRANSACTION_PROCESSING -> {
                    statusTextView.text = "Processando transação PIX"
                }
                is PaymentProcessingEvent.AUTHORIZING -> {
                    statusTextView.text = "Autorizando pagamento PIX"
                }
                is PaymentProcessingEvent.GENERIC_SUCCESS -> {
                    statusTextView.text = "Pagamento PIX confirmado"
                }
                else -> {
                    // Outros eventos PIX podem ser tratados aqui
                }
            }
        }

        /**
         * Trata eventos específicos de pagamento com cartão
         */
        private fun handleCardPaymentEvents(event: PaymentProcessingEvent, statusTextView: TextView) {
            when (event) {
                is PaymentProcessingEvent.CARD_REACH_OR_INSERT -> {
                    statusTextView.text = "Aproxime ou insira o cartão"
                }
                is PaymentProcessingEvent.USE_CHIP -> {
                    statusTextView.text = "Insira o cartão no chip"
                }
                is PaymentProcessingEvent.USE_MAGNETIC_STRIPE -> {
                    statusTextView.text = "Passe o cartão na tarja magnética"
                }
                is PaymentProcessingEvent.SWIPE_CARD_REQUESTED -> {
                    statusTextView.text = "Passe o cartão"
                }
                is PaymentProcessingEvent.CARD_INSERTED -> {
                    statusTextView.text = "Cartão inserido - processando..."
                }
                is PaymentProcessingEvent.PIN_REQUESTED -> {
                    statusTextView.text = "Insira o PIN do cartão"
                }
                is PaymentProcessingEvent.TRANSACTION_PROCESSING -> {
                    statusTextView.text = "Processando transação..."
                }
                is PaymentProcessingEvent.AUTHORIZING -> {
                    statusTextView.text = "Autorizando pagamento..."
                }
                is PaymentProcessingEvent.CARD_BIN_REQUESTED -> {
                    statusTextView.text = "Verificando cartão..."
                }
                is PaymentProcessingEvent.CARD_HOLDER_REQUESTED -> {
                    statusTextView.text = "Verificando titular..."
                }
                is PaymentProcessingEvent.CVV_REQUESTED -> {
                    statusTextView.text = "Verificando CVV..."
                }
                is PaymentProcessingEvent.DOWNLOADING_TABLES -> {
                    statusTextView.text = "Baixando tabelas..."
                }
                is PaymentProcessingEvent.CARD_REMOVAL_REQUESTING -> {
                    statusTextView.text = "Remova o cartão"
                }
                is PaymentProcessingEvent.CONTACTLESS_ON_DEVICE -> {
                    statusTextView.text = "Pagamento contactless detectado"
                }
                else -> {
                    // Outros eventos de cartão podem ser tratados aqui
                }
            }
        }

        /**
         * Enfileira um pagamento e opcionalmente inicia o processamento imediatamente
         *
         * IMPORTANTE: O amount é convertido para centavos (multiplicado por 100)
         * antes de ser enviado ao ViewModel, pois o processador trabalha com centavos.
         *
         * @param paymentViewModel ViewModel de processamento
         * @param shoppingCartManager Gerenciador do carrinho (usado se amount for null)
         * @param method Método de pagamento (CREDIT, DEBIT, PIX, etc)
         * @param amount Valor em reais (será convertido para centavos). Se null, usa o total do carrinho
         * @param isTransactionless Se true, não gera transação no banco
         * @param startImmediately Se true, inicia o processamento imediatamente após enfileirar
         */
        fun enqueuePayment(
            paymentViewModel: PaymentProcessingViewModel,
            shoppingCartManager: ShoppingCartManager,
            method: SystemPaymentMethod,
            amount: Double? = null,
            isTransactionless: Boolean,
            startImmediately: Boolean
        ) {
            val cart = shoppingCartManager.getCart()
            val paymentAmountInReais = amount ?: (cart.totalPrice.toDouble() / 100.0)
            val commission = 0

            // Converte para centavos (unidade esperada pelo processador)
            val amountInCents = (paymentAmountInReais * 100).toInt()

            paymentViewModel.enqueuePayment(
                amount = amountInCents,
                commission = commission,
                method = method,
                isTransactionless = isTransactionless
            )

            if (startImmediately) {
                paymentViewModel.startProcessing()
            }
        }
    }
}