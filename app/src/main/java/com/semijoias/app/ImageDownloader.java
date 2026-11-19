package com.semijoias.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Baixa uma imagem HTTPS para um arquivo local, com limites de tempo, tipo e tamanho. */
final class ImageDownloader {

    private static final int TIMEOUT_MS = 10_000;
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private ImageDownloader() {
    }

    static void download(String imageUrl, File target) throws IOException {
        URL url = new URL(imageUrl);
        if (!"https".equals(url.getProtocol())) {
            throw new IOException("Somente HTTPS é permitido");
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        try {
            String type = connection.getContentType();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK
                    || type == null || !type.startsWith("image/")) {
                throw new IOException("Resposta não é uma imagem");
            }
            copyLimited(connection.getInputStream(), target);
        } finally {
            connection.disconnect();
        }
    }

    private static void copyLimited(InputStream source, File target) throws IOException {
        long total = 0;
        byte[] buffer = new byte[8192];
        try (InputStream in = source; OutputStream out = new FileOutputStream(target)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) {
                    throw new IOException("Imagem excede o limite de tamanho");
                }
                out.write(buffer, 0, read);
            }
        }
    }
}
