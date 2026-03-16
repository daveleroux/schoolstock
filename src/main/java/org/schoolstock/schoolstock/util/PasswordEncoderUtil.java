package org.schoolstock.schoolstock.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: PasswordEncoderUtil <password>");
            System.exit(1);
        }

        String encoded = new BCryptPasswordEncoder().encode(args[0]);
        System.out.println(encoded);
    }
}
