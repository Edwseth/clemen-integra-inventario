package com.willyes.clemenintegra.shared.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:integralerp@clemenlab.com}")
    private String defaultFrom;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCorreoTexto(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        if (defaultFrom != null && !defaultFrom.isBlank()) {
            mensaje.setFrom(defaultFrom);
        }

        int maxIntentos = 3;
        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                mailSender.send(mensaje);
                log.debug("Correo electrónico enviado a {} con asunto '{}'", destinatario, asunto);
                return;
            } catch (MailException ex) {
                log.warn("Intento {} de envío de correo falló: {}", intento, ex.getMessage());

                if (intento == maxIntentos) {
                    throw ex;
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interruptedEx) {
                    Thread.currentThread().interrupt();
                    log.warn("Retry de correo interrumpido en el intento {}", intento);
                }
            }
        }
    }
}
