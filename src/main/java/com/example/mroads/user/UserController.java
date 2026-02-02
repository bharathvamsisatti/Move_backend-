package com.example.mroads.user;

import com.example.mroads.user.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserRepository userRepository;

	// ==========================
	// UPDATE PROFILE (TEXT DATA)
	// ==========================
	@PutMapping("/profile")
	public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
			Authentication authentication) {

		String userUuid = (String) authentication.getPrincipal();
		userService.updateProfile(userUuid, request);

		return ResponseEntity.ok("Profile updated successfully");
	}

	// ==========================
	// UPDATE PROFILE IMAGE
	// ==========================
	@PostMapping(value = "/profile-photo", consumes = "multipart/form-data")
	public ResponseEntity<?> uploadProfilePhoto(@RequestPart("image") MultipartFile image,
			Authentication authentication) throws Exception {

		String userUuid = (String) authentication.getPrincipal();
		String path = userService.updateProfilePhoto(userUuid, image);

		return ResponseEntity.ok(java.util.Map.of("profileImage", path));
	}

	// ==========================
	// GET CURRENT USER (ME)
	// ==========================
	@GetMapping("/me")
	public ResponseEntity<?> getMe(Authentication authentication) {

		String userUuid = (String) authentication.getPrincipal();

		User user = userRepository.findByUserUuid(userUuid).orElseThrow(() -> new RuntimeException("User not found"));

		Map<String, Object> response = new HashMap<>();
		response.put("name", user.getUserName());
		response.put("email", user.getEmail());
		response.put("provider", user.getProvider());
		response.put("verified", true);
		response.put("userUuid", user.getUserUuid());
		response.put("profileImage", user.getProfileImagePath());
		response.put("dateOfBirth", user.getDateOfBirth());
		response.put("alternatePhone", user.getAlternatePhoneNumber());

		return ResponseEntity.ok(response);
	}

}
