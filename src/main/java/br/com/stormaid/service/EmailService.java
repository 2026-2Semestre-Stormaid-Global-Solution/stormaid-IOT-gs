package br.com.stormaid.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendRecoveryCodeEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("StormAid - Código de Recuperação de Senha");
        message.setText("Olá,\n\nRecebemos um pedido para recuperar a sua senha na plataforma StormAid.\n\n" +
                "O seu código de verificação de 6 dígitos é: " + code + "\n\n" +
                "Este código expira em breve. Se não solicitou esta alteração, ignore este e-mail.\n\n" +
                "Atenciosamente,\nEquipa StormAid");

        mailSender.send(message);
    }
}