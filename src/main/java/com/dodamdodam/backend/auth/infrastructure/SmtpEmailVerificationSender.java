package com.dodamdodam.backend.auth.infrastructure;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailVerificationSender {
    private final JavaMailSenderImpl sender;
    private final String from;

    /** 유한 timeout과 TLS를 적용하며 메일 설정 오류는 다른 API의 기동을 막지 않는다. */
    public SmtpEmailVerificationSender(Environment environment) {
        JavaMailSenderImpl configured = null;
        this.from = environment.getProperty("app.email.smtp.from", "");
        try {
            String host = environment.getProperty("app.email.smtp.host", "");
            int port = Integer.parseInt(environment.getProperty("app.email.smtp.port", "587"));
            int timeout = Integer.parseInt(environment.getProperty("app.email.smtp.timeout-ms", "5000"));
            boolean tls = bool(environment, "app.email.smtp.starttls", true);
            boolean ssl = bool(environment, "app.email.smtp.ssl", false);
            if (host.isBlank() || port < 1 || port > 65535 || timeout < 1 || from.contains("\r") || from.contains("\n")) {
                throw new IllegalArgumentException();
            }
            new jakarta.mail.internet.InternetAddress(from, true).validate();
            // 평문은 인증 없는 loopback SMTP 테스트에만 허용한다.
            String username = environment.getProperty("app.email.smtp.username", "");
            if (!tls && !ssl && (!username.isEmpty() || !loopback(host))) {
                throw new IllegalArgumentException();
            }
            var candidate = new JavaMailSenderImpl();
            candidate.setHost(host);
            candidate.setPort(port);
            candidate.setDefaultEncoding("UTF-8");
            var properties = candidate.getJavaMailProperties();
            properties.setProperty("mail.debug", "false");
            properties.setProperty("mail.smtp.connectiontimeout", Integer.toString(timeout));
            properties.setProperty("mail.smtp.timeout", Integer.toString(timeout));
            properties.setProperty("mail.smtp.writetimeout", Integer.toString(timeout));
            properties.setProperty("mail.smtp.starttls.enable", Boolean.toString(tls));
            properties.setProperty("mail.smtp.starttls.required", Boolean.toString(tls));
            properties.setProperty("mail.smtp.ssl.enable", Boolean.toString(ssl));
            properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
            properties.setProperty("mail.smtp.auth", Boolean.toString(!username.isEmpty()));
            if (!username.isEmpty()) {
                String file = environment.getProperty("app.email.smtp.password-file", "");
                String password = Files.readString(Path.of(file), StandardCharsets.UTF_8);
                if (password.isEmpty()) {
                    throw new IllegalArgumentException();
                }
                candidate.setUsername(username);
                candidate.setPassword(password);
            }
            configured = candidate;
        }
        catch (IOException | RuntimeException | jakarta.mail.MessagingException invalid) {
            configured = null;
        }
        this.sender = configured;
    }

    /** 정상·decoy 모두 같은 SMTP 경로를 사용하며 decoy에는 번호를 넣지 않는다. */
    public void send(String email, String code, boolean eligible) {
        if (sender == null) {
            throw new IllegalStateException("이메일 전송 설정을 확인할 수 없습니다.");
        }
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("도담도담 이메일 확인 안내");
        message.setText(eligible ? "이메일 확인 번호: " + code + "\n10분 안에 입력해 주세요. 요청하지 않았다면 무시해 주세요."
                : "이메일 확인 요청을 받았습니다. 이 요청으로 인증을 진행할 수 없다면 입력 정보와 이용 경로를 확인해 주세요. 요청하지 않았다면 무시해 주세요.");
        sender.send(message);
    }

    /** 문자열 설정 오류를 false로 바꿔 TLS를 끄지 않는다. */
    private static boolean bool(Environment environment, String name, boolean fallback) {
        String value = environment.getProperty(name, Boolean.toString(fallback));
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException();
        }
        return Boolean.parseBoolean(value);
    }

    /** 외부 DNS 조회 없이 명시적인 loopback 주소만 테스트용 평문 전송으로 인정한다. */
    private static boolean loopback(String host) {
        return host.equals("localhost") || InetAddress.ofLiteral(host).isLoopbackAddress();
    }
}
