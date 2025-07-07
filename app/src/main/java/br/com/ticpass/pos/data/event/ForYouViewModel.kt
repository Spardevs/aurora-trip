package br.com.ticpass.pos.data.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticpass.pos.data.api.APIRepository
import br.com.ticpass.pos.data.room.repository.AcquisitionRepository
import br.com.ticpass.pos.data.room.repository.CashupRepository
import br.com.ticpass.pos.data.room.repository.CategoryRepository
import br.com.ticpass.pos.data.room.repository.ConsumptionRepository
import br.com.ticpass.pos.data.room.repository.EventRepository
import br.com.ticpass.pos.data.room.repository.OrderRepository
import br.com.ticpass.pos.data.room.repository.PassRepository
import br.com.ticpass.pos.data.room.repository.PaymentRepository
import br.com.ticpass.pos.data.room.repository.PosRepository
import br.com.ticpass.pos.data.room.repository.RefundRepository
import br.com.ticpass.pos.data.room.repository.CashierRepository
import br.com.ticpass.pos.data.room.repository.VoucherRepository
import br.com.ticpass.pos.data.room.repository.VoucherRedemptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FollowableTopic(
    val topic: Topic,
    val isFollowed: Boolean,
)

data class Topic(
    val id: String,
    val name: String,
    val shortDescription: String,
    val longDescription: String,
    val url: String,
    val imageUrl: String,
)

@HiltViewModel
class ForYouViewModel @Inject constructor(
    val apiRepository: APIRepository,
    val cashierRepository: CashierRepository,
    val eventRepository: EventRepository,
    val posRepository: PosRepository,
    val categoryRepository: CategoryRepository,
    val orderRepository: OrderRepository,
    val paymentRepository: PaymentRepository,
    val cashupRepository: CashupRepository,
    val passRepository: PassRepository,
    val acquisitionRepository: AcquisitionRepository,
    val refundRepository: RefundRepository,
    val voucherRepository: VoucherRepository,
    val voucherRedemptionRepository: VoucherRedemptionRepository,
    val consumptionRepository: ConsumptionRepository
) : ViewModel() {

    val isSyncing = false
    private val shouldShowOnboarding: Flow<Boolean> = flowOf(true)
    val myTopics = flowOf(

        listOf(
            FollowableTopic(
                isFollowed = true,
                topic = Topic(
                    id = "1111",
                    name = "foo",
                    shortDescription = "",
                    longDescription = "",
                    url = "",
                    imageUrl = "",
                )
            ),
            FollowableTopic(
                isFollowed = true,
                topic = Topic(
                    id = "2222",
                    name = "bar",
                    shortDescription = "",
                    longDescription = "",
                    url = "",
                    imageUrl = "",
                )
            ),
            FollowableTopic(
                isFollowed = true,
                topic = Topic(
                    id = "3333",
                    name = "bar",
                    shortDescription = "",
                    longDescription = "",
                    url = "",
                    imageUrl = "",
                )
            ),
            FollowableTopic(
                isFollowed = true,
                topic = Topic(
                    id = "4444",
                    name = "bar",
                    shortDescription = "",
                    longDescription = "",
                    url = "",
                    imageUrl = "",
                )
            ),
            FollowableTopic(
                isFollowed = true,
                topic = Topic(
                    id = "5555",
                    name = "bar",
                    shortDescription = "",
                    longDescription = "",
                    url = "",
                    imageUrl = "",
                )
            ),
            FollowableTopic(
                isFollowed = true,
                topic = Topic(
                    id = "6666",
                    name = "bar",
                    shortDescription = "",
                    longDescription = "",
                    url = "",
                    imageUrl = "",
                )
            ),
            FollowableTopic(
                isFollowed = true,
                topic = Topic(
                    id = "7777",
                    name = "bar",
                    shortDescription = "",
                    longDescription = "",
                    url = "",
                    imageUrl = "",
                )
            ),
            FollowableTopic(
                isFollowed = true,
                topic = Topic(
                    id = "8888",
                    name = "bar",
                    shortDescription = "",
                    longDescription = "",
                    url = "",
                    imageUrl = "",
                )
            ),
        )
    )

    val onboardingUiState: StateFlow<OnboardingUiState> =
        combine(shouldShowOnboarding) { shouldShowOnboarding ->
            val events = eventRepository.getAllEvents()

            if (shouldShowOnboarding.first()) {
                OnboardingUiState.Shown(events = events)
            } else {
                OnboardingUiState.NotShown
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = OnboardingUiState.Loading,
            )
}