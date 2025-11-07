package com.userservice.infrastructure.writer;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.userservice.infrastructure.writer.dto.ImageUrlResponse;

@FeignClient(name = "profile-image-service", url = "localhost:8080")
public interface ProfileImageClient {

	@PostMapping("/api/images")
	ImageUrlResponse uploadImage(@RequestParam("file") MultipartFile file);
}
