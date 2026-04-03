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
}