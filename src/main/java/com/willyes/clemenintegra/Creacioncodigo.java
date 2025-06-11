package com.willyes.clemenintegra;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Creacioncodigo {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("admin123");
        System.out.println("Hash generado internamente: " + hash);
    }

}
