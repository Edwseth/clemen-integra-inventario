package com.willyes.clemenintegra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
public class PasswordEncoderUtil {

    public static void main(String[] args) {
        // Cambia esta línea con la contraseña que quieres codificar
        String rawPassword = "Arrux3023";

        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword = encoder.encode(rawPassword);

        log.info("Contraseña original: {}", rawPassword);
        log.info("Contraseña codificada: {}", encodedPassword);
    }
}

