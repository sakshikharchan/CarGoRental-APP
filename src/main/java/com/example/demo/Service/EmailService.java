package com.example.demo.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("CargoRent — Reset Your Password");
            helper.setFrom("noreply@cargorent.com");

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { font-family: 'Segoe UI', sans-serif; background: #f5f4f0; margin: 0; padding: 0; }
                    .container { max-width: 520px; margin: 40px auto; background: #fff;
                                 border-radius: 16px; border: 1px solid #e2e4ec; overflow: hidden; }
                    .header { background: #0f1117; padding: 28px 36px; }
                    .brand { font-size: 20px; font-weight: 800; color: #fff; letter-spacing: -0.3px; }
                    .brand span { color: #818cf8; }
                    .body { padding: 36px 36px 28px; }
                    h2 { font-size: 22px; color: #0f1117; margin: 0 0 12px; }
                    p { font-size: 14px; color: #6b7280; line-height: 1.65; margin: 0 0 16px; }
                    .btn { display: inline-block; background: #4f46e5; color: #fff;
                           padding: 13px 28px; border-radius: 10px; text-decoration: none;
                           font-size: 15px; font-weight: 600; margin: 8px 0 20px; }
                    .note { font-size: 12px; color: #9ca3af; }
                    .link-box { background: #f9fafb; border: 1px solid #e5e7eb;
                                border-radius: 8px; padding: 12px 16px; margin: 16px 0;
                                word-break: break-all; font-size: 12px; color: #374151; }
                    .footer { padding: 20px 36px; background: #f9fafb;
                              border-top: 1px solid #f3f4f6; font-size: 12px; color: #9ca3af; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header">
                      <div class="brand">Cargo<span>Rent</span></div>
                    </div>
                    <div class="body">
                      <h2>Reset your password</h2>
                      <p>Hi %s,</p>
                      <p>We received a request to reset the password for your CargoRent account.
                         Click the button below to set a new password.</p>
                      <a href="%s" class="btn">Reset My Password</a>
                      <p>Or copy and paste this link into your browser:</p>
                      <div class="link-box">%s</div>
                      <p class="note">This link expires in <strong>30 minutes</strong>.
                         If you didn't request this, you can safely ignore this email.</p>
                    </div>
                    <div class="footer">
                      © 2025 CargoRent · Fleet Management Platform<br>
                      This is an automated email, please do not reply.
                    </div>
                  </div>
                </body>
                </html>
            """.formatted(userName, resetLink, resetLink);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}