package com.family.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("giaphanguyen@gmail.com"); // Thay bằng email của bạn
        message.setTo(toEmail);
        message.setSubject("Mã xác thực Quên mật khẩu - Hệ thống Gia Phả");
        message.setText("Chào bạn,\n\n"
                + "Mã OTP để khôi phục mật khẩu của bạn là: " + otpCode + "\n\n"
                + "Mã này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.\n\n"
                + "Trân trọng,\nBan Quản Trị Gia Phả");

        // Lệnh thực thi gửi mail
        mailSender.send(message);
    }

    public void sendVerificationEmail(String toEmail, String verificationCode, java.time.LocalDateTime expiry) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("giaphanguyen@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Xác thực email - Hệ thống Gia Phả");
        String expiryStr = expiry != null ? expiry.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
        message.setText("Xin chào,\n\n" +
                "Cảm ơn bạn đã đăng ký. Mã xác thực để kích hoạt tài khoản của bạn là: " + verificationCode + "\n" +
                "Mã này có hiệu lực đến: " + expiryStr + "\n\n" +
                "Vui lòng không chia sẻ mã này.\n\n" +
                "Trân trọng,\nBan Quản Trị Gia Phả");
        mailSender.send(message);
    }

    public void sendWelcomeEmail(String toEmail, String fullName, String familyName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("giaphanguyen@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Chào mừng đến với hệ thống Gia Phả");
        message.setText("Xin chào " + (fullName != null ? fullName : "") + ",\n\n"
                + "Chúc mừng! Tài khoản của bạn đã được kích hoạt với vai trò Trưởng họ (FAMILY_HEAD).\n"
                + "Dòng họ được tạo: " + (familyName != null ? familyName : "(không tên)") + "\n\n"
                + "Trân trọng,\nBan Quản Trị Gia Phả");
        mailSender.send(message);
    }
}