package com.campus.trade.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordCodec {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordCodec() {
    }

    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        SECURE_RANDOM.nextBytes(saltBytes);
        return HexFormat.of().formatHex(saltBytes);
    }

    public static String encode(String rawPassword, String salt) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest((salt + ":" + rawPassword).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm unavailable", e);
        }
    }

    public static boolean matches(String rawPassword, String salt, String encodedPassword) {
        if (rawPassword == null || salt == null || encodedPassword == null) {
            return false;
        }
        return encode(rawPassword, salt).equalsIgnoreCase(encodedPassword);
    }
}
