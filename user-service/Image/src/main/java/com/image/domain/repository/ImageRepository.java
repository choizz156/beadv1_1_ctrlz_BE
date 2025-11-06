package com.image.domain.repository;

import java.util.Optional;

import com.image.domain.entity.Image;

public interface ImageRepository {

	Image save(Image image);
	Optional<Image> findById(String id);

	Optional<Image> findByS3Key(String s3Key);

	void deleteByS3Key(String s3Key);

	void deleteById(String id);
}
