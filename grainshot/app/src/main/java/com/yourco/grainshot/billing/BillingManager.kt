package com.yourco.grainshot.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BillingUiState(
    val isSubscribed: Boolean  = false,
    val shotCount: Int         = 0,
    val billingAvailable: Boolean = false,
    val price: String          = "$3.99",
    val pending: Boolean       = false,
    val error: String?         = null,
)

class BillingManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPurchaseGranted: suspend (token: String, expiryMs: Long?) -> Unit,
    private val onPurchaseRevoked: suspend () -> Unit,
) : PurchasesUpdatedListener {

    companion object {
        const val LIFETIME_PRODUCT_ID = "grainshot_lifetime"
    }

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(billingAvailable = true)
                    scope.launch { fetchPrice(); restorePurchases() }
                }
            }
            override fun onBillingServiceDisconnected() {
                _state.value = _state.value.copy(billingAvailable = false)
            }
        })
    }

    fun disconnect() = client.endConnection()

    private suspend fun fetchPrice() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(LIFETIME_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )).build()
        val result = withContext(Dispatchers.IO) { client.queryProductDetails(params) }
        result.productDetailsList?.firstOrNull()?.oneTimePurchaseOfferDetails?.formattedPrice
            ?.let { _state.value = _state.value.copy(price = it) }
    }

    fun launchBillingFlow(activity: Activity) {
        if (!_state.value.billingAvailable) return
        scope.launch {
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(LIFETIME_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )).build()
            val result = withContext(Dispatchers.IO) { client.queryProductDetails(params) }
            val details = result.productDetailsList?.firstOrNull() ?: return@launch
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details).build()
                )).build()
            _state.value = _state.value.copy(pending = true)
            withContext(Dispatchers.Main) { client.launchBillingFlow(activity, flowParams) }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        _state.value = _state.value.copy(pending = false)
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                purchases?.forEach { scope.launch { handlePurchase(it) } }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                _state.value = _state.value.copy(error = null)
            else ->
                _state.value = _state.value.copy(
                    error = "Purchase failed (${result.responseCode}). Try again.")
        }
    }

    suspend fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP).build()
        val result = withContext(Dispatchers.IO) { client.queryPurchasesAsync(params) }
        val active = result.purchasesList.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (active.isNotEmpty()) active.forEach { handlePurchase(it) }
        else onPurchaseRevoked()
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            val ack = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken).build()
            val ackResult = withContext(Dispatchers.IO) { client.acknowledgePurchase(ack) }
            if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(error = "COULD NOT CONFIRM PURCHASE")
                return
            }
        }
        onPurchaseGranted(purchase.purchaseToken, null) // lifetime — no expiry
        _state.value = _state.value.copy(error = null)
    }
}
