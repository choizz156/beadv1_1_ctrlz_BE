package com.userservice.infrastructure.jpa.entity;

import com.common.model.persistence.BaseEntity;
import com.userservice.domain.vo.UserRole;
import com.userservice.infrastructure.jpa.vo.EmbeddedAddress;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

	@Column(nullable = false)
	private String oauthId;
	@Column(nullable = false)
	private String name;
	@Column(nullable = false, unique = true)
	private String nickname;
	@Column(nullable = false, unique = true)
	private String email;

	private String password;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private UserRole role = UserRole.USER;

	private String profileUrl;

	@Column(nullable = false)
	private String phoneNumber;


	@Embedded
	private EmbeddedAddress address;

	@Builder
	public UserEntity(
		String name,
		String email,
		String password,
		String profileUrl,
		String phoneNumber,
		EmbeddedAddress address,
		String oauthId,
		String nickname
	) {
		this.oauthId = oauthId;
		this.name = name;
		this.email = email;
		this.password = password;
		this.profileUrl = profileUrl;
		this.address = address;
		this.phoneNumber = phoneNumber;
		this.nickname = nickname;
	}

	@Override
	protected String getEntitySuffix() {
		return UserEntity.class.getAnnotation(Table.class).name();
	}
}
