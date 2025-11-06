package com.image.domain.entity;

import com.common.model.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "images")
public class Image extends BaseEntity {

	@Column(nullable = false)
	private String originalFileName;

	@Column(nullable = false)
	private String storedFileName;

	@Column(nullable = false, length = 1000)
	private String s3Url;

	@Column(nullable = false)
	private String s3Key;

	@Column(nullable = false)
	private Long fileSize;

	@Column(nullable = false)
	private String contentType;

	@Builder
	public Image(
		String originalFileName,
		String storedFileName,
		String s3Url,
		String s3Key,
		Long fileSize,
		String contentType
	) {
		this.originalFileName = originalFileName;
		this.storedFileName = storedFileName;
		this.s3Url = s3Url;
		this.s3Key = s3Key;
		this.fileSize = fileSize;
		this.contentType = contentType;
	}

	@Override
	protected String getEntitySuffix() {
		return this.getClass().getAnnotation(Table.class).name();
	}
}
