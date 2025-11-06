package com.asset.image.application;

import org.springframework.web.multipart.MultipartFile;

public interface AssetService<T> {

	T upload(MultipartFile file);
	T getImage(String id);
}
