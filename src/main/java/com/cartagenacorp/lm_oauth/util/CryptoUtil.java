package com.cartagenacorp.lm_oauth.util;

import com.cartagenacorp.lm_oauth.exceptions.BaseException;
import org.springframework.http.HttpStatus;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtil {

    public static String generatePassphrase() {
        byte[] passphraseBytes = new byte[16];
        new SecureRandom().nextBytes(passphraseBytes);
        return Base64.getEncoder().encodeToString(passphraseBytes);
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Error generando MD5", e);
        }
    }

    private static SecretKeySpec getKeyFromString(String keyStr) throws Exception {
        byte[] key = keyStr.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = new byte[16]; // AES requiere 128 bits
        System.arraycopy(key, 0, keyBytes, 0, Math.min(key.length, keyBytes.length));
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static String encrypt(String data, String secret) {
        try {
            SecretKeySpec key = getKeyFromString(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar", e);
        }
    }

    public static String decrypt(String encryptedData, String secret) {
        try {
            SecretKeySpec key = getKeyFromString(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            return new String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BaseException("OTP inválida", HttpStatus.UNPROCESSABLE_ENTITY.value());
        }
    }
}
