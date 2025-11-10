package com.user.infrastructure.api.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.common.model.web.BaseResponse;
import com.user.application.adapter.dto.UserContext;
import com.user.application.port.in.UserCommandUseCase;
import com.user.infrastructure.api.dto.UserCreateRequest;
import com.user.infrastructure.api.dto.UserCreateResponse;
import com.user.infrastructure.api.mapper.UserContextMapper;
import com.user.infrastructure.feign.ProfileImageClient;
import com.user.infrastructure.feign.dto.ImageResponse;
import com.user.infrastructure.reader.port.UserReaderPort;
import com.user.infrastructure.reader.port.dto.UserDescription;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

	@Value("${custom.image.default}")
	private String defaultImageUrl;

	private final UserReaderPort userReaderPort;
	private final UserCommandUseCase userCommandUseCase;
	private final ProfileImageClient profileImageClient;

	@PostMapping
	public BaseResponse<UserCreateResponse> createUser(
		@RequestPart("profileImage") MultipartFile profileImage,
		@Valid @RequestPart("request") UserCreateRequest request
	) {

		ImageResponse imageResponse = (profileImage.isEmpty())
			? new ImageResponse(defaultImageUrl, null)
			: profileImageClient.uploadImage(profileImage);

		UserContext context = UserContextMapper.toContext(request, imageResponse);
		UserContext savedUserContext = userCommandUseCase.create(context);

		return new BaseResponse<>(new UserCreateResponse(
			savedUserContext.userId(),
			savedUserContext.profileImageUrl(),
			savedUserContext.nickname()
		),
			"가입 완료");
	}

	@GetMapping("/{id}")
	public UserDescription getUser(@PathVariable String id) {
		return userReaderPort.getUserDescription(id);
	}
}
