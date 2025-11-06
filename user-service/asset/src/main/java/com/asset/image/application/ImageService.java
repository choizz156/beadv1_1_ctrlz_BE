package com.asset.image.application;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.asset.image.domain.entity.Image;
import com.asset.image.domain.repository.ImageRepository;
import com.common.exception.CustomException;
import com.common.exception.vo.FileExceptionCode;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@RequiredArgsConstructor
@Transactional
@Service
public class ImageService implements AssetService<Image> {

	@Value("${custom.bucket-name}")
	private String bucketName;
	@Value("${custom.image.allowed-extension}")
	private String allowedExtension;
	@Value("${custom.image.max-size}")
	private long maxSize;

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final ImageRepository imageRepository;

	@Override
	public Image upload(MultipartFile file) {

		validateFile(file);

		String originalFileName = file.getOriginalFilename();
		String storedFileName = generateFileName(originalFileName);
		String s3key = generateS3Key(storedFileName);

		try {
			uploadToS3(file, s3key);

			String s3Url = getS3Url(s3key);

			Image image = Image.builder()
				.originalFileName(originalFileName)
				.storedFileName(storedFileName)
				.s3Url(s3Url)
				.s3Key(s3key)
				.fileSize(file.getSize())
				.contentType(file.getContentType())
				.build();

			return imageRepository.save(image);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	private void uploadToS3(MultipartFile file, String s3key) throws IOException {
		PutObjectRequest request = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(s3key)
			.contentType(file.getContentType())
			.contentLength(file.getSize())
			.build();

		s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
	}

	private void validateFile(MultipartFile file) {

		if (file == null || file.isEmpty()) {
			throw new CustomException(FileExceptionCode.FILE_EMPTY.getValue());
		}

		if (file.getSize() > maxSize) {
			throw new CustomException(
				FileExceptionCode.FILE_EMPTY.addArgument(String.valueOf(file.getSize()))
			);
		}

		String originalFileName = file.getOriginalFilename();
		if (originalFileName == null || !isValidExtension(originalFileName)) {
			throw new CustomException(
				FileExceptionCode.NOT_VALID_EXTENSION.addArgument(originalFileName)
			);
		}

		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new CustomException(FileExceptionCode.NOT_IMAGE.getValue());
		}
	}

	private boolean isValidExtension(String fileName) {
		String extension = getFileExtension(fileName);
		List<String> allowed = List.of(allowedExtension.split(","));
		return allowed.stream()
			.anyMatch(ext -> ext.trim().equalsIgnoreCase(extension));
	}

	private String getFileExtension(String fileName) {
		int lastIndexOf = fileName.lastIndexOf(".");
		if (lastIndexOf == -1) {
			return "";
		}
		return fileName.substring(lastIndexOf + 1).toLowerCase();
	}

	private String generateFileName(String originalFileName) {
		String extension = getFileExtension(originalFileName);
		String uuid = UUID.randomUUID().toString();
		return uuid + "." + extension;
	}

	private String generateS3Key(String fileName) {
		String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
		return "images/" + date + fileName;
	}

	private String getS3Url(String s3Key) {
		return "https://%s.s3.amazonaws.com/%s".formatted(bucketName, s3Key);
	}
}
