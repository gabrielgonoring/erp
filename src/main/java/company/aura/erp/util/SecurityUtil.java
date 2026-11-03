package company.aura.erp.util;

import java.security.SecureRandom;

public class SecurityUtil {

    private static final String RESET_CODE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%¨&*_+{}[]";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int RESET_CODE_LENGTH = 255;

    public static String generateResetCode() {
        StringBuilder sb = new StringBuilder(RESET_CODE_LENGTH);
        for (int i = 0; i < RESET_CODE_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(RESET_CODE_CHARS.length());
            sb.append(RESET_CODE_CHARS.charAt(index));
        }
        return sb.toString();
    }
}
