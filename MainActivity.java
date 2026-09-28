package com.sheviwalls.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.content.*;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.*;
import java.util.*;
import com.android.billingclient.api.*;

public class MainActivity extends Activity {
    private static final String MONTHLY_ID = "shevi_premium_monthly";
    private static final String YEARLY_ID = "shevi_premium_yearly";
    private static final String PREFS = "shevi_walls";
    private static final String PREMIUM = "premium";

    private WebView webView;
    private BillingClient billingClient;
    private boolean billingReady = false;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new SheviBridge(), "SheviBridge");
        webView.loadUrl("file:///android_asset/web/index.html");
        setContentView(webView);
        setupBilling();
    }

    private void setupBilling() {
        billingClient = BillingClient.newBuilder(this)
                .setListener((result, purchases) -> {
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase p : purchases) handlePurchase(p);
                    } else if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        billingMessage("Purchase update: " + result.getDebugMessage());
                    }
                })
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override public void onBillingServiceDisconnected() {
                billingReady = false;
                billingMessage("Google Play billing is temporarily unavailable. Please try again.");
            }

            @Override public void onBillingSetupFinished(BillingResult result) {
                billingReady = result.getResponseCode() == BillingClient.BillingResponseCode.OK;
                if (billingReady) {
                    queryProducts();
                    restore();
                } else {
                    billingMessage("Google Play billing could not be started: " + result.getDebugMessage());
                }
            }
        });
    }

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(MONTHLY_ID).setProductType(BillingClient.ProductType.SUBS).build());
        products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(YEARLY_ID).setProductType(BillingClient.ProductType.SUBS).build());

        QueryProductDetailsParams q = QueryProductDetailsParams.newBuilder()
                .setProductList(products).build();

        billingClient.queryProductDetailsAsync(q, (result, details) -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                billingMessage("Premium products are not available yet. Please try again later.");
                return;
            }
            if (details.getProductDetailsList() == null || details.getProductDetailsList().isEmpty()) {
                billingMessage("Premium products are not configured in Google Play yet.");
            }
        });
    }

    private void restore() {
        if (!billingReady) {
            billingMessage("Google Play billing is still starting. Please try again in a moment.");
            return;
        }

        // Start from the persisted state, then replace it with the current Play entitlement.
        // This means an expired/revoked subscription is not kept unlocked after the next restore.
        setPremium(false);

        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS).build(),
                (result, purchases) -> {
                    if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        billingMessage("Could not restore Premium: " + result.getDebugMessage());
                        return;
                    }
                    if (purchases != null) {
                        for (Purchase p : purchases) handlePurchase(p);
                    }
                    billingMessage(hasStoredPremium()
                            ? "Premium restored ✨"
                            : "No active Premium subscription was found.");
                });
    }

    private void handlePurchase(Purchase p) {
        if (p.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;
        if (!isSheviSubscription(p)) return;

        if (!p.isAcknowledged()) {
            billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(p.getPurchaseToken()).build(),
                    result -> {
                        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                            billingMessage("Premium purchase needs acknowledgement: " + result.getDebugMessage());
                        }
                    });
        }

        setPremium(true);
        billingMessage("Premium unlocked ✨");
    }

    private boolean isSheviSubscription(Purchase p) {
        if (p.getProducts() == null) return false;
        return p.getProducts().contains(MONTHLY_ID) || p.getProducts().contains(YEARLY_ID);
    }

    private void setPremium(boolean value) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(PREMIUM, value).apply();
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript(
                    value ? "window.onSheviPremium&&window.onSheviPremium()"
                          : "window.onSheviPremiumRevoked&&window.onSheviPremiumRevoked()",
                    null));
        }
    }

    private boolean hasStoredPremium() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREMIUM, false);
    }

    private void billingMessage(String message) {
        if (webView == null) return;
        String safe = message.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
        webView.post(() -> webView.evaluateJavascript(
                "window.onSheviBillingStatus&&window.onSheviBillingStatus('" + safe + "')", null));
    }

    private void launch(String id) {
        if (!billingReady) {
            billingMessage("Google Play billing is still starting. Please try again in a moment.");
            return;
        }

        if (!MONTHLY_ID.equals(id) && !YEARLY_ID.equals(id)) {
            billingMessage("That Premium product is not recognised.");
            return;
        }

        QueryProductDetailsParams q = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(id)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()))
                .build();

        billingClient.queryProductDetailsAsync(q, (result, details) -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                billingMessage("Could not load the Premium product: " + result.getDebugMessage());
                return;
            }
            if (details.getProductDetailsList() == null || details.getProductDetailsList().isEmpty()) {
                billingMessage("This Premium product is not configured in Google Play yet.");
                return;
            }

            ProductDetails pd = details.getProductDetailsList().get(0);
            List<ProductDetails.SubscriptionOfferDetails> offers = pd.getSubscriptionOfferDetails();
            if (offers == null || offers.isEmpty()) {
                billingMessage("No active base plan/offer was found for this Premium product.");
                return;
            }

            BillingFlowParams.ProductDetailsParams pp =
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(pd)
                            .setOfferToken(offers.get(0).getOfferToken())
                            .build();

            BillingResult launchResult = billingClient.launchBillingFlow(
                    this,
                    BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(Collections.singletonList(pp))
                            .build());

            if (launchResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                billingMessage("Google Play could not open checkout: " + launchResult.getDebugMessage());
            }
        });
    }

    public class SheviBridge {
        @JavascriptInterface
        public void purchase(String productId) {
            runOnUiThread(() -> launch(productId));
        }

        @JavascriptInterface
        public void restore() {
            runOnUiThread(MainActivity.this::restore);
        }

        @JavascriptInterface
        public void saveWallpaper(String fileName) {
            try {
                InputStream in = getAssets().open("web/images/" + fileName);
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, "Shevi Walls - " + fileName);
                v.put(MediaStore.Downloads.MIME_TYPE, "image/png");
                v.put(MediaStore.Downloads.RELATIVE_PATH, "Download/Shevi Walls");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) throw new IOException("Could not create download destination");
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new IOException("Could not open download destination");
                    byte[] b = new byte[8192];
                    int n;
                    while ((n = in.read(b)) > 0) out.write(b, 0, n);
                } finally {
                    in.close();
                }
                webView.post(() -> webView.evaluateJavascript(
                        "alert('Wallpaper saved to Downloads / Shevi Walls')", null));
            } catch (Exception e) {
                webView.post(() -> webView.evaluateJavascript(
                        "alert('Could not save wallpaper on this device')", null));
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
