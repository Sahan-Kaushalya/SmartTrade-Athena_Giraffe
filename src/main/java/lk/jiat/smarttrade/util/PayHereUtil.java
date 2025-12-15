package lk.jiat.smarttrade.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PayHereUtil {
    private static final String MERCHANT_ID = "1231403"; // replace with your merchant id
    private static final String MERCHANT_SECRET = "MjU2MzQ3NjU0MDIzOTA3NTIxMTc0MjkwNDA1MTkwMjYyNDAyNTMxNQ=="; // replace with your merchant secret

    public static String getMerchantId() {
        return MERCHANT_ID;
    }

    public static String getMerchantSecret() {
        return MERCHANT_SECRET;
    }

    public static String generateHash(String orderId, double amount, String currency) {

        String formattedAmount = String.format("%.2f", amount);

        String secretHash = md5(MERCHANT_SECRET).toUpperCase();

        String raw = MERCHANT_ID
                + orderId
                + formattedAmount
                + currency
                + secretHash;

        return md5(raw).toUpperCase();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 error", e);
        }
    }
}
