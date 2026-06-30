package com.example.banking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envoi d'e-mails transactionnels. Le {@link JavaMailSender} n'est present que
 * si la configuration SMTP est fournie (spring.mail.*). On l'injecte donc de
 * facon optionnelle : sans SMTP configure (ex. dev local), l'app demarre quand
 * meme et l'e-mail est simplement journalise au lieu d'etre envoye.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                       @Value("${app.mail.from:no-reply@localhost}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
    }

    /** Envoie le lien de reinitialisation. Echoue en silence (log) pour ne pas
     *  reveler au client si l'envoi a reussi (anti-enumeration des comptes). */
    public void sendPasswordReset(String to, String link) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP non configure : lien de reinitialisation pour {} = {}", to, link);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Reinitialisation de votre mot de passe");
            message.setText(
                    "Bonjour,\n\n"
                    + "Vous avez demande la reinitialisation de votre mot de passe.\n"
                    + "Cliquez sur le lien ci-dessous (valable 30 minutes) :\n\n"
                    + link + "\n\n"
                    + "Si vous n'etes pas a l'origine de cette demande, ignorez cet e-mail.\n");
            sender.send(message);
            log.info("E-mail de reinitialisation envoye a {}", to);
        } catch (Exception e) {
            // On ne propage pas : le controleur repond toujours pareil.
            log.error("Echec d'envoi de l'e-mail de reinitialisation a {}", to, e);
        }
    }
}
