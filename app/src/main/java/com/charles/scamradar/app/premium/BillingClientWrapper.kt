package com.charles.scamradar.app.premium

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingClientWrapper(
    private val context: Context,
    private val entitlementRepository: EntitlementRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(BillingState.IDLE)
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private val _productCache = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _productCache.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { purchases.forEach { handlePurchase(it) } }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _state.value = BillingState.IDLE
        } else {
            _state.value = BillingState.ERROR
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .enablePendingPurchases(
            com.android.billingclient.api.PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .setListener(purchasesUpdatedListener)
        .build()

    fun startConnection() {
        if (_state.value == BillingState.READY) return
        _state.value = BillingState.CONNECTING
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = BillingState.READY
                    scope.launch {
                        queryProducts()
                        refreshEntitlement()
                    }
                } else {
                    _state.value = BillingState.ERROR
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.value = BillingState.DISCONNECTED
            }
        })
    }

    suspend fun queryProducts() {
        val ids = listOf(
            EntitlementSku.PREMIUM_MONTHLY,
            EntitlementSku.PREMIUM_YEARLY,
            EntitlementSku.FAMILY_MONTHLY,
            EntitlementSku.FAMILY_YEARLY,
        )
        val productList = ids.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        val deferred = suspendCancellableCoroutine<List<ProductDetails>> { cont ->
            client.queryProductDetailsAsync(params) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(list)
                } else {
                    cont.resume(emptyList())
                }
            }
        }
        _productCache.value = deferred
    }

    fun launchPurchase(activity: Activity, product: ProductDetails) {
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    suspend fun refreshEntitlement() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val list = suspendCancellableCoroutine<List<Purchase>> { cont ->
            client.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(purchases)
                } else {
                    cont.resume(emptyList())
                }
            }
        }
        if (list.isEmpty()) {
            entitlementRepository.applyEntitlement(EntitlementState.FREE)
            return
        }
        list.forEach { handlePurchase(it) }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            val ack = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(ack) { /* best-effort */ }
        }
        val tier = entitlementForProducts(purchase.products)
        entitlementRepository.applyEntitlement(tier)
    }

    private fun entitlementForProducts(ids: List<String>): EntitlementState {
        return when {
            ids.any { it == EntitlementSku.FAMILY_MONTHLY || it == EntitlementSku.FAMILY_YEARLY } ->
                EntitlementState.FAMILY
            ids.any { it == EntitlementSku.PREMIUM_MONTHLY || it == EntitlementSku.PREMIUM_YEARLY } ->
                EntitlementState.PREMIUM
            else -> EntitlementState.FREE
        }
    }

    fun close() {
        if (client.isReady) client.endConnection()
    }

    enum class BillingState { IDLE, CONNECTING, READY, DISCONNECTED, ERROR }
}
