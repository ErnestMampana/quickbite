package za.co.ernest.quickbite.AuthenticationService.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PasswordGenerator {

    private static final SecureRandom random = new SecureRandom();

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!$*-.=?@_";

    public static String generateSecurePassword(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("Password length must be at least 4 characters.");
        }

        List<Character> passwordChars = new ArrayList<>();

        passwordChars.add(LOWER.charAt(random.nextInt(LOWER.length())));
        passwordChars.add(UPPER.charAt(random.nextInt(UPPER.length())));
        passwordChars.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        passwordChars.add(SPECIAL.charAt(random.nextInt(SPECIAL.length())));


        String allCharacters = LOWER + UPPER + DIGITS + SPECIAL;

        for (int i = 4; i < length; i++) {
            passwordChars.add(allCharacters.charAt(random.nextInt(allCharacters.length())));
        }

        Collections.shuffle(passwordChars, random);

        StringBuilder passwordBuilder = new StringBuilder();
        for (char ch : passwordChars) {
            passwordBuilder.append(ch);
        }

        return passwordBuilder.toString();
    }
}
