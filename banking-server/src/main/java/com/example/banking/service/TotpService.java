package com.example.banking.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * Implementation TOTP (RFC 6238) compatible Google Authenticator / Authy :
 * HMAC-SHA1, periode 30 s, 6 chiffres. Aucune dependance externe (JDK pur).
 */
@Service
public class TotpService {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;

    private final SecureRandom random = new SecureRandom();

    /** Genere un secret aleatoire (160 bits) encode en base32. */
    public String generateSecret() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** URI otpauth:// a encoder dans un QR code pour l'appli d'authentification. */
    public String buildOtpAuthUri(String secret, String accountLabel, String issuer) {
        String label = URLEncoder.encode(issuer + ":" + accountLabel, StandardCharsets.UTF_8);
        String iss = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + iss
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD_SECONDS;
    }

    /** Verifie un code (fenetre +/-1 pas pour tolerer un petit decalage d'horloge). */
    public boolean verify(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }
        String clean = code.trim().replaceAll("\\s", "");
        if (!clean.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        byte[] key = base32Decode(secret);
        long step = Instant.now().getEpochSecond() / PERIOD_SECONDS;
        for (long w = -1; w <= 1; w++) {
            if (generateCode(key, step + w).equals(clean)) {
                return true;
            }
        }
        return false;
    }

    private String generateCode(byte[] key, long counter) {
        try {
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] h = mac.doFinal(data);
            int offset = h[h.length - 1] & 0x0F;
            int binary = ((h[offset] & 0x7f) << 24)
                    | ((h[offset + 1] & 0xff) << 16)
                    | ((h[offset + 2] & 0xff) << 8)
                    | (h[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Echec de generation TOTP", e);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int idx = (buffer >> (bitsLeft - 5)) & 0x1f;
                bitsLeft -= 5;
                sb.append(BASE32.charAt(idx));
            }
        }
        if (bitsLeft > 0) {
            int idx = (buffer << (5 - bitsLeft)) & 0x1f;
            sb.append(BASE32.charAt(idx));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String s) {
        String clean = s.trim().replace("=", "").replace(" ", "").toUpperCase();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;
        for (int i = 0; i < clean.length(); i++) {
            int val = BASE32.indexOf(clean.charAt(i));
            if (val < 0) {
                continue;
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }
}
