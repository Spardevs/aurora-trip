package br.com.ticpass.pos.data.room

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.compose.utils.getDateFromString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Date

data class AcquisitionRecord(
    val name: String,
    val price: Long,
    val count: Int
)

data class VoucheredRecord(
    val name: String,
    val price: Long,
    val count: Int
)

data class RefundRecord(
    val name: String,
    val price: Long,
    val count: Int
)

data class ConsumptionRecord(
    val name: String,
    val price: Long,
    val count: Int
)

class AuthManager(
    private val dataStore: DataStore<Preferences>
) {
    private val sharedPreferences: SharedPreferences = MainActivity.appContext.getSharedPreferences("payment_income_prefs", Context.MODE_PRIVATE)
    private val PAYMENT_PREFIX = "payment_"
    private val ACQUISITION_PREFIX = "acquisition_"
    private val REFUND_PREFIX = "refund_"
    private val VOUCHERED_PREFIX = "vouchered_"
    private val CASHUP_KEY = "cashup"
    private val COMMISSION_KEY = "commission"
    private val TICKET_REPRINTING_COUNT_KEY = "ticket_reprinting_count"
    private val TICKET_REPRINTING_AMOUNT_KEY = "ticket_reprinting_amount"
    private val CONSUMPTION_PREFIX = "consumption_"

    companion object {
        val MEMBERSHIP_TOKEN_KEY = stringPreferencesKey("membership_token")
        val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
        val CASHIER_NAME_KEY = stringPreferencesKey("cashier_name")
        val POS_OPENING_DATE = stringPreferencesKey("pos_opening_date")
        val ACQUIRER_PAYMENT_ENABLED = stringPreferencesKey("acquirer_payment")
        val PAYMENT_PRINTING_ENABLED = stringPreferencesKey("payment_printing")
        val MULTI_PAYMENT_ENABLED = stringPreferencesKey("multi_payment")
        val MENU_SYNC_STATE = stringPreferencesKey("menu_sync_state")
        val PAYMENT_DEBT = stringPreferencesKey("payment_debt")
        val PAYMENT_DEBT_DEDUCTED = stringPreferencesKey("payment_debt_deducted")
        val PENDING_PAYMENT_IDS_KEY = stringSetPreferencesKey("pending_payment_ids")
    }

    fun clearPrefs() {
        sharedPreferences.edit().clear().apply()
    }

    fun setConsumption(acquisitionName: String, price: Long, count: Int) {
        val consumptionData = "$acquisitionName=|=$price=|=$count"
        val editor = sharedPreferences.edit()
        editor.putString("$CONSUMPTION_PREFIX$acquisitionName", consumptionData)
        editor.apply()
    }

    fun incrementConsumptionCount(acquisitionName: String, price: Long, count: Int) {
        val currentData = getConsumption(acquisitionName)
        val newCount = currentData.count + count
        setConsumption(acquisitionName, price, newCount)
    }

    fun decrementConsumptionCount(acquisitionName: String, count: Int) {
        val currentData = getConsumption(acquisitionName)
        val newCount = currentData.count - count
        setConsumption(acquisitionName, currentData.price, newCount)
    }

    fun getConsumption(acquisitionName: String): ConsumptionRecord {
        val consumptionDataString = sharedPreferences.getString("$CONSUMPTION_PREFIX$acquisitionName", null)
        if (consumptionDataString.isNullOrEmpty()) {
            return ConsumptionRecord(
                name = "",
                price = 0L,
                count = 0
            )
        }

        val parts = consumptionDataString.split("=|=")
        return ConsumptionRecord(
            name = parts[0],
            price = parts[1].toLong(),
            count = parts[2].toInt()
        )
    }

    fun getTotalConsumption(): Long {
        var totalValue = 0L
        val allEntries = sharedPreferences.all
        for (entry in allEntries) {
            if (entry.key.startsWith(CONSUMPTION_PREFIX) && entry.value is String) {
                val parts = (entry.value as String).split("=|=")
                val price = parts[1].toLong()
                val count = parts[2].toInt()
                totalValue += price * count
            }
        }
        return totalValue
    }

    fun getAllConsumptions(): List<ConsumptionRecord> {
        val consumptions = mutableListOf<ConsumptionRecord>()
        val allEntries = sharedPreferences.all
        for (entry in allEntries) {
            if (entry.key.startsWith(CONSUMPTION_PREFIX) && entry.value is String) {
                val parts = (entry.value as String).split("=|=")
                consumptions.add(
                    ConsumptionRecord(
                        name = parts[0],
                        price = parts[1].toLong(),
                        count = parts[2].toInt()
                    )
                )
            }
        }
        return consumptions
    }

    fun setRefund(acquisitionName: String, price: Long, count: Int) {
        val refundData = "$acquisitionName=|=$price=|=$count"
        val editor = sharedPreferences.edit()
        editor.putString("$REFUND_PREFIX$acquisitionName", refundData)
        editor.apply()
    }

    fun incrementRefund(acquisitionName: String, amount: Long, count: Int) {
        val currentData = getRefund(acquisitionName)
        val newCount = currentData.count + count
        setRefund(acquisitionName, amount, newCount)
    }

    fun decrementRefund(acquisitionName: String, count: Int) {
        val currentData = getRefund(acquisitionName)
        val newCount = currentData.count - count
        setRefund(acquisitionName, currentData.price, newCount)
    }

    fun getRefund(refundType: String): RefundRecord {
        val refundDataString = sharedPreferences.getString("$REFUND_PREFIX$refundType", null)
        if (refundDataString.isNullOrEmpty()) {
            return RefundRecord(
                name = "",
                price = 0L,
                count = 0
            )
        }

        val parts = refundDataString.split("=|=")
        return RefundRecord(
            name = parts[0],
            price = parts[1].toLong(),
            count = parts[2].toInt()
        )
    }

    fun getTotalRefund(): Long {
        var totalRefund = 0L
        val allEntries = sharedPreferences.all
        for (entry in allEntries) {
            if (entry.key.startsWith(REFUND_PREFIX) && entry.value is String) {
                val parts = (entry.value as String).split("=|=")
                val price = parts[1].toLong()
                val count = parts[2].toInt()
                totalRefund += price * count
            }
        }
        return totalRefund
    }

    fun getAllRefunds(): List<RefundRecord> {
        val refunds = mutableListOf<RefundRecord>()
        val allEntries = sharedPreferences.all
        for (entry in allEntries) {
            if (entry.key.startsWith(REFUND_PREFIX) && entry.value is String) {
                val parts = (entry.value as String).split("=|=")
                refunds.add(
                    RefundRecord(
                        name = parts[0],
                        price = parts[1].toLong(),
                        count = parts[2].toInt()
                    )
                )
            }
        }
        return refunds
    }

    fun setVouchered(voucherName: String, price: Long, count: Int) {
        val voucherData = "$voucherName=|=$price=|=$count"
        val editor = sharedPreferences.edit()
        editor.putString("$VOUCHERED_PREFIX$voucherName", voucherData)
        editor.apply()
    }

    fun incrementVoucherCount(voucherName: String, price: Long, count: Int) {
        val currentData = getVouchered(voucherName)
        val newCount = currentData.count + count
        setVouchered(voucherName, price, newCount)
    }

    fun decrementVoucherCount(voucherName: String, count: Int) {
        val currentData = getVouchered(voucherName)
        val newCount = currentData.count - count
        setVouchered(voucherName, currentData.price, newCount)
    }

    fun getVouchered(voucherType: String): VoucheredRecord {
        val voucherDataString = sharedPreferences.getString("$VOUCHERED_PREFIX$voucherType", null)
        if (voucherDataString.isNullOrEmpty()) {
            return VoucheredRecord(
                name = "",
                price = 0L,
                count = 0
            )
        }

        val parts = voucherDataString.split("=|=")
        return VoucheredRecord(
            name = parts[0],
            price = parts[1].toLong(),
            count = parts[2].toInt()
        )
    }

    fun getTotalVouchered(): Long {
        var totalValue = 0L
        val allEntries = sharedPreferences.all
        for (entry in allEntries) {
            if (entry.key.startsWith(VOUCHERED_PREFIX) && entry.value is String) {
                val parts = (entry.value as String).split("=|=")
                val price = parts[1].toLong()
                val count = parts[2].toInt()
                totalValue += price * count
            }
        }
        return totalValue
    }

    fun getAllVouchered(): List<VoucheredRecord> {
        val voucheredList = mutableListOf<VoucheredRecord>()
        val allEntries = sharedPreferences.all
        for (entry in allEntries) {
            if (entry.key.startsWith(VOUCHERED_PREFIX) && entry.value is String) {
                val parts = (entry.value as String).split("=|=")
                voucheredList.add(
                    VoucheredRecord(
                        name = parts[0],
                        price = parts[1].toLong(),
                        count = parts[2].toInt()
                    )
                )
            }
        }
        return voucheredList
    }

    fun setTicketReprintingAmount(amount: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(TICKET_REPRINTING_AMOUNT_KEY, amount)
        editor.apply()
    }

    fun incrementTicketReprintingAmount(amount: Long) {
        val currentAmount = getTicketReprintingAmount()
        setTicketReprintingAmount(currentAmount + amount)
    }

    fun decrementTicketReprintingAmount(amount: Long) {
        val currentAmount = getTicketReprintingAmount()
        setTicketReprintingAmount(currentAmount - amount)
    }

    fun getTicketReprintingAmount(): Long {
        return sharedPreferences.getLong(TICKET_REPRINTING_AMOUNT_KEY, 0L)
    }

    fun setTicketReprintingCount(amount: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(TICKET_REPRINTING_COUNT_KEY, amount)
        editor.apply()
    }

    fun incrementTicketReprintingCount(amount: Long) {
        val currentAmount = getTicketReprintingCount()
        setTicketReprintingCount(currentAmount + amount)
    }

    fun decrementTicketReprintingCount(amount: Long) {
        val currentAmount = getTicketReprintingCount()
        setTicketReprintingCount(currentAmount - amount)
    }

    fun getTicketReprintingCount(): Long {
        return sharedPreferences.getLong(TICKET_REPRINTING_COUNT_KEY, 0L)
    }

    fun setCommission(amount: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(COMMISSION_KEY, amount)
        editor.apply()
    }

    fun incrementCommission(amount: Long) {
        val currentAmount = getCommission()
        setCommission(currentAmount + amount)
    }

    fun decrementCommission(amount: Long) {
        val currentAmount = getCommission()
        setCommission(currentAmount - amount)
    }

    fun getCommission(): Long {
        return sharedPreferences.getLong(COMMISSION_KEY, 0L)
    }

    fun setCashup(amount: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(CASHUP_KEY, amount)
        editor.apply()
    }

    fun incrementCashup(amount: Long) {
        val currentAmount = getCashup()
        setCashup(currentAmount + amount)
    }

    fun decrementCashup(amount: Long) {
        val currentAmount = getCashup()
        setCashup(currentAmount - amount)
    }

    fun getCashup(): Long {
        return sharedPreferences.getLong(CASHUP_KEY, 0L)
    }

    fun setPaymentIncome(paymentType: String, amount: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong("$PAYMENT_PREFIX$paymentType", amount)
        editor.apply()
    }

    fun incrementPaymentIncome(paymentType: String, amount: Long) {
        val currentAmount = getPaymentIncome(paymentType)
        setPaymentIncome(paymentType, currentAmount + amount)
    }

    fun decrementPaymentIncome(paymentType: String, amount: Long) {
        val currentAmount = getPaymentIncome(paymentType)
        setPaymentIncome(paymentType, currentAmount - amount)
    }

    fun getPaymentIncome(paymentType: String): Long {
        return sharedPreferences.getLong("$PAYMENT_PREFIX$paymentType", 0L)
    }

    fun getTotalPaymentIncome(): Long {
        var totalIncome = 0L
        val allEntries = sharedPreferences.all
        for (entry in allEntries) {
            if (entry.key.startsWith(PAYMENT_PREFIX)) {
                totalIncome += entry.value as Long
            }
        }
        return totalIncome
    }

    fun getAllPayments(): List<Pair<String, Long>> {
        val payments = mutableListOf<Pair<String, Long>>()
        val allEntries = sharedPreferences.all
        for (entry in allEntries) {
            if (entry.key.startsWith(PAYMENT_PREFIX)) {
                val paymentType = entry.key.removePrefix(PAYMENT_PREFIX)
                payments.add(Pair(paymentType, entry.value as Long))
            }
        }
        return payments
    }

    fun setAcquisition(acquisitionName: String, price: Long, count: Int) {
        val acquisitionData = "$acquisitionName=|=$price=|=$count"
        val editor = sharedPreferences.edit()
        editor.putString("$ACQUISITION_PREFIX$acquisitionName", acquisitionData)
        editor.apply()
    }

    fun incrementAcquisitionCount(acquisitionName: String, price: Long, count: Int) {
        val currentData = getAcquisition(acquisitionName)
        val newCount = currentData.count + count
        setAcquisition(acquisitionName, price, newCount)
    }

    fun decrementAcquisitionCount(acquisitionName: String, price: Long, count: Int) {
        val currentData = getAcquisition(acquisitionName)
        val newCount = currentData.count - count
        setAcquisition(acquisitionName, price, newCount)
    }

    fun getAcquisition(acquisitionType: String): AcquisitionRecord {
        val acquisitionDataString = sharedPreferences.getString("$ACQUISITION_PREFIX$acquisitionType", null)
        if (acquisitionDataString.isNullOrEmpty()) {
            return AcquisitionRecord(
                name = "",
                price = 0L,
                count = 0
            )
        }

        val parts = acquisitionDataString.split("=|=")
        return AcquisitionRecord(
            name = parts[0],
            price = parts[1].toLong(),
            count = parts[2].toInt()
        )
    }

    fun getTotalAcquisition(): Long {
        var totalValue = 0L
        val allEntries = sharedPreferences.all
        for (entry in allEntries) {
            if (entry.key.startsWith(ACQUISITION_PREFIX) && entry.value is String) {
                val parts = (entry.value as String).split("=|=")
                val price = parts[1].toLong()
                val count = parts[2].toInt()
                totalValue += price * count
            }
        }
        return totalValue
    }

    fun getAllAcquisitions(): List<AcquisitionRecord> {
        val acquisitions = mutableListOf<AcquisitionRecord>()
        val allEntries = sharedPreferences.all
        for (entry in allEntries) {
            if (entry.key.startsWith(ACQUISITION_PREFIX) && entry.value is String) {
                val parts = (entry.value as String).split("=|=")
                acquisitions.add(
                    AcquisitionRecord(
                        name = parts[0],
                        price = parts[1].toLong(),
                        count = parts[2].toInt()
                    )
                )
            }
        }
        return acquisitions
    }

    suspend fun clearAllPreferences() {
        clearPrefs()
        dataStore.edit { preferences ->
            preferences.remove(PAYMENT_DEBT)
            preferences.remove(PAYMENT_DEBT_DEDUCTED)
            preferences.remove(MEMBERSHIP_TOKEN_KEY)
            preferences.remove(JWT_TOKEN_KEY)
            preferences.remove(CASHIER_NAME_KEY)
            preferences.remove(POS_OPENING_DATE)
            preferences.remove(ACQUIRER_PAYMENT_ENABLED)
            preferences.remove(MENU_SYNC_STATE)
            preferences.remove(PAYMENT_PRINTING_ENABLED)
            preferences.remove(PENDING_PAYMENT_IDS_KEY)
        }
    }
    suspend fun clearPreference(preferenceKey: Preferences.Key<*>) {
        dataStore.edit { preferences ->
            preferences.remove(preferenceKey)
        }
    }

    suspend fun savePendingPaymentIds(ids: Set<String>) {
        val currentIds = getPendingPaymentIds().toMutableSet()
        currentIds.addAll(ids)
        dataStore.edit { preferences ->
            preferences[PENDING_PAYMENT_IDS_KEY] = currentIds
        }
    }

    suspend fun getPendingPaymentIds(): Set<String> {
        val idsFlow = dataStore.data.map { preferences ->
            preferences[PENDING_PAYMENT_IDS_KEY] ?: emptySet()
        }
        return idsFlow.first()
    }

    suspend fun clearPendingPaymentIds() {
        dataStore.edit { preferences ->
            preferences.remove(PENDING_PAYMENT_IDS_KEY)
        }
    }

    suspend fun setJWT(jwtToken: String) {
        dataStore.edit { preferences ->
            preferences[JWT_TOKEN_KEY] = jwtToken
        }
    }

    suspend fun getJwtToken(): String {
        val jwtTokenFlow = dataStore.data.map { preferences ->
            preferences[JWT_TOKEN_KEY]
        }

        val jwtToken = jwtTokenFlow.firstOrNull()
        return jwtToken ?: ""
    }

    suspend fun setMembership(exp: String) {
        dataStore.edit { preferences ->
            preferences[MEMBERSHIP_TOKEN_KEY] = exp
        }
    }

    suspend fun getMembership(): Date? {
        val expFlow = dataStore.data.map { preferences ->
            preferences[MEMBERSHIP_TOKEN_KEY]
        }

        val expDate = expFlow.firstOrNull()
        return if(!expDate.isNullOrEmpty()) getDateFromString(expDate) else null
    }

    suspend fun setDebt(exp: Long) {
        dataStore.edit { preferences ->
            preferences[PAYMENT_DEBT] = exp.toString()
        }
    }

    suspend fun getDebt(): Long {
        val debtFlow = dataStore.data.map { preferences ->
            preferences[PAYMENT_DEBT]
        }

        val debtString = debtFlow.firstOrNull()
        return if(!debtString.isNullOrEmpty()) debtString.toLong() else 0L
    }

    private val _debtDeductedFlow = MutableStateFlow(0L)

    suspend fun debtDeducted(): Long {
        val cashierNameFlow = dataStore.data.map { preferences ->
            val foo = preferences[PAYMENT_DEBT_DEDUCTED]
            if(foo.isNullOrEmpty()) 0L else foo.toLong()
        }

        val deducted = cashierNameFlow.first()

        return deducted
    }

    suspend fun setDebtDeducted(exp: Long) {
        dataStore.edit { preferences ->
            preferences[PAYMENT_DEBT_DEDUCTED] = exp.toString()
        }
        _debtDeductedFlow.value = exp
    }

    fun getDebtDeducted(): Flow<Long> {
        val stateFlow = dataStore.data.map { preferences ->
            val foo = preferences[PAYMENT_DEBT_DEDUCTED]
            if(foo.isNullOrEmpty()) 0L else foo.toLong()
        }

        return stateFlow
    }

    suspend fun setMenuState(state: String) {
        dataStore.edit { preferences ->
            preferences[MENU_SYNC_STATE] = state
        }
    }

    fun getMenuState(): Flow<String> {
        val menuStateFlow = dataStore.data.map { preferences ->
            preferences[MENU_SYNC_STATE]
        }

        return menuStateFlow.map { it ?: "outdated" }
    }

    suspend fun setCashierName(name: String) {
        dataStore.edit { preferences ->
            preferences[CASHIER_NAME_KEY] = name
        }
    }

    suspend fun getCashierName(): String {
        val cashierNameFlow = dataStore.data.map { preferences ->
            preferences[CASHIER_NAME_KEY]
        }

        val cashierName = cashierNameFlow.firstOrNull()
        return cashierName ?: ""
    }

    suspend fun setPosOpeningDate(date: String) {
        dataStore.edit { preferences ->
            preferences[POS_OPENING_DATE] = date
        }
    }

    suspend fun getPosOpeningDate(): String {
        val posOpeningDateFlow = dataStore.data.map { preferences ->
            preferences[POS_OPENING_DATE]
        }

        val posOpeningDate = posOpeningDateFlow.firstOrNull()
        return posOpeningDate ?: ""
    }

    suspend fun setMultiPaymentEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[MULTI_PAYMENT_ENABLED] = if(value) "true" else "false"
        }
    }

    suspend fun isMultiPaymentEnabled(): Boolean {
        val isMultiPaymentEnabledFlow = dataStore.data.map { preferences ->
            preferences[MULTI_PAYMENT_ENABLED]
        }

        // defaults to true
        val isMultiPaymentEnabled = isMultiPaymentEnabledFlow.firstOrNull() ?: "true"
        setMultiPaymentEnabled(isMultiPaymentEnabled == "true")

        return isMultiPaymentEnabled == "true"
    }

    suspend fun setPaymentPrintingEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PAYMENT_PRINTING_ENABLED] = if(value) "true" else "false"
        }
    }

    suspend fun isPaymentPrintingEnabled(): Boolean {
        val isPaymentPrintingEnabledFlow = dataStore.data.map { preferences ->
            preferences[PAYMENT_PRINTING_ENABLED]
        }

        // defaults to true
        val isPaymentPrintingEnabled = isPaymentPrintingEnabledFlow.firstOrNull() ?: "true"
        setPaymentPrintingEnabled(isPaymentPrintingEnabled == "true")

        return isPaymentPrintingEnabled == "true"
    }

    suspend fun toggleAcquirerPayment() {
        dataStore.edit { preferences ->
            val isAcquirerPaymentEnabled = preferences[ACQUIRER_PAYMENT_ENABLED]
            preferences[ACQUIRER_PAYMENT_ENABLED] = if(isAcquirerPaymentEnabled == "true") "false" else "true"
        }
    }

    suspend fun togglePaymentPrinting() {
        dataStore.edit { preferences ->
            val isPaymentPrintingEnabled = preferences[PAYMENT_PRINTING_ENABLED]
            preferences[PAYMENT_PRINTING_ENABLED] = if(isPaymentPrintingEnabled == "true") "false" else "true"
        }
    }

    suspend fun setAcquirerPaymentEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[ACQUIRER_PAYMENT_ENABLED] = if(value) "true" else "false"
        }
    }

    suspend fun isAcquirerPaymentEnabled(): Boolean {
        val isNativePaymentEnabledFlow = dataStore.data.map { preferences ->
            preferences[ACQUIRER_PAYMENT_ENABLED]
        }

        // defaults to true
        val isNativePaymentEnabled = isNativePaymentEnabledFlow.firstOrNull() ?: "true"
        setAcquirerPaymentEnabled(isNativePaymentEnabled == "true")

        return isNativePaymentEnabled == "true"
    }
}