package com.semijoias.app;

import android.net.Uri;

/** Constantes da loja e regras de confiança de origem. */
final class StoreConfig {

    static final String START_URL = "https://lojatoquedebeleza.semijoias.net/admin/#produtos";

    private static final String TRUSTED_DOMAIN = "semijoias.net";

    private StoreConfig() {
    }

    /** Só páginas HTTPS do domínio da loja podem usar a ponte nativa e receber o script. */
    static boolean isTrusted(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        return "https".equals(uri.getScheme())
                && host != null
                && (host.equals(TRUSTED_DOMAIN) || host.endsWith("." + TRUSTED_DOMAIN));
    }
}
