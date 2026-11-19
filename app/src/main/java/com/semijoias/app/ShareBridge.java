package com.semijoias.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

/**
 * Ponte JavaScript -> Android exposta à WebView como {@code AndroidShare}.
 * Compartilha foto + texto do produto com o WhatsApp via Intent.
 */
final class ShareBridge {

    private static final String PACKAGE_WHATSAPP = "com.whatsapp";
    private static final String PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b";

    private final Context context;
    private final BooleanSupplier pageIsTrusted;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    ShareBridge(Context context, BooleanSupplier pageIsTrusted) {
        this.context = context;
        this.pageIsTrusted = pageIsTrusted;
    }

    @JavascriptInterface
    public void shareImageToWhatsApp(String imageUrl, String message) {
        // A ponte é visível a qualquer página da WebView: ignora chamadas de origem não confiável
        if (!pageIsTrusted.getAsBoolean()) return;

        executor.execute(() -> {
            try {
                File dir = new File(context.getCacheDir(), "images");
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IllegalStateException("Cache indisponível");
                }
                File image = new File(dir, "shared_image.jpg");
                ImageDownloader.download(imageUrl, image);
                mainHandler.post(() -> startShare(image, message));
            } catch (Exception e) {
                mainHandler.post(() -> toast(R.string.error_download_image));
            }
        });
    }

    void shutdown() {
        executor.shutdownNow();
    }

    private void startShare(File image, String message) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", image);

        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("image/*")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_TEXT, message)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            context.startActivity(new Intent(intent).setPackage(PACKAGE_WHATSAPP));
        } catch (Exception whatsAppMissing) {
            try {
                context.startActivity(new Intent(intent).setPackage(PACKAGE_WHATSAPP_BUSINESS));
            } catch (Exception businessMissing) {
                toast(R.string.error_open_whatsapp);
            }
        }
    }

    private void toast(int messageRes) {
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show();
    }
}
