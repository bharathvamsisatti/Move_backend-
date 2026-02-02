package com.example.mroads.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender mailSender;

	public void sendOtp(String to, String otp) {

		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(to);
		message.setSubject("MOVE – Password Reset OTP");
		message.setText("Hello,\n\n" + "Your OTP for resetting your MOVE account password is:\n\n" + "OTP: " + otp
				+ "\n\n" + "This OTP is valid for 5 minutes.\n\n"
				+ "If you did not request this, please ignore this email.\n\n" + "— Team MOVE 🚗");

		mailSender.send(message);
	}
}
