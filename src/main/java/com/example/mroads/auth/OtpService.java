package com.example.mroads.auth;

import com.example.mroads.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpService {

	private final EmailService emailService;

	private static final long EXPIRY_SECONDS = 300; // 5 minutes

	private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

	public void generateAndSend(String email) {
		String otp = generateOtp();
		store.put(email, new OtpEntry(otp, Instant.now()));

		emailService.sendOtp(email, otp);
	}

	public boolean verify(String email, String otp) {
		OtpEntry entry = store.get(email);

		if (entry == null)
			return false;
		if (Instant.now().isAfter(entry.created.plusSeconds(EXPIRY_SECONDS))) {
			store.remove(email);
			return false;
		}

		return entry.otp.equals(otp);
	}

	public void clear(String email) {
		store.remove(email);
	}

	private String generateOtp() {
		return String.valueOf(100000 + new Random().nextInt(900000));
	}

	private record OtpEntry(String otp, Instant created) {
	}
}
