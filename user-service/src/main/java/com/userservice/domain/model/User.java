package com.userservice.domain.model;

import java.time.LocalDateTime;

import com.userservice.domain.vo.Address;
import com.userservice.domain.vo.UserRole;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Getter
public class User {

	private String id;
	private String name;
	private String email;
	private String password;
	private String nickname;
	private UserRole role;
	private String profileUrl;
	private Address address;
	private String phoneNumber;
	private String oauthId;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@Builder
	public User(
		String id,
		String name,
		String email,
		String password, String nickname,
		UserRole role,
		String profileUrl,
		Address address, String phoneNumber, String oauthId,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
	) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.password = password;
		this.nickname = nickname;
		this.role = role;
		this.profileUrl = profileUrl;
		this.address = address;
		this.phoneNumber = phoneNumber;
		this.oauthId = oauthId;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}
}
