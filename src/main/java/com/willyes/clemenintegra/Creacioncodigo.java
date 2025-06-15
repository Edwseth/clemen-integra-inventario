package com.willyes.clemenintegra;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Creacioncodigo {

    private static final Logger log = LoggerFactory.getLogger(Creacioncodigo.class);
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("almacen123");
        log.info("Hash generado internamente: {}", hash);
    }

}
