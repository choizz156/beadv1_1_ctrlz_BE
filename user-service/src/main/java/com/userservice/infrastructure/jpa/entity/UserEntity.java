package com.userservice.infrastructure.jpa.entity;

import com.common.model.persistence.BaseEntity;
import com.userservice.domain.vo.UserRole;
import com.userservice.infrastructure.jpa.vo.EmbeddedAddress;

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

	private String oauthId;
	private String name;
	private String nickname;
	private String email;
	private String password;
	//TODO: 판매자 등록시 인증??
	@Enumerated(EnumType.STRING)
	private UserRole role = UserRole.USER;
	private String profileUrl;
	private String phoneNumber;

	@Embedded
	private EmbeddedAddress address;

	@Builder
	public UserEntity(
		String name,
		String email,
		String password,
		UserRole role,
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
		this.role = role;
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
