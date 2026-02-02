package com.example.mroads.user;

import com.example.mroads.user.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	// ==========================
	// UPDATE PROFILE DETAILS
	// ==========================
	public void updateProfile(String userUuid, UpdateProfileRequest request) {

		User user = userRepository.findByUserUuid(userUuid).orElseThrow(() -> new RuntimeException("User not found"));

		if (request.getName() != null && !request.getName().isBlank()) {
			user.setUserName(request.getName());
		}

		user.setDateOfBirth(request.getDateOfBirth());
		user.setAlternatePhoneNumber(request.getAlternatePhone());

		userRepository.save(user);
	}

	// ==========================
	// UPDATE PROFILE PHOTO
	// ==========================
	public String updateProfilePhoto(String userUuid, MultipartFile image) throws IOException {

		if (image == null || image.isEmpty()) {
			throw new RuntimeException("Profile image is required");
		}

		User user = userRepository.findByUserUuid(userUuid).orElseThrow(() -> new RuntimeException("User not found"));

		Path uploadDir = Paths.get("uploads/profile");
		Files.createDirectories(uploadDir);

		String fileName = userUuid + "-" + UUID.randomUUID() + "-" + image.getOriginalFilename();

		Path filePath = uploadDir.resolve(fileName);
		Files.write(filePath, image.getBytes());

		user.setProfileImagePath(filePath.toString());
		userRepository.save(user);

		return filePath.toString();
	}
}
