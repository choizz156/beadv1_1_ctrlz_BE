package com.userservice.domain.model;

import java.time.LocalDateTime;

import com.userservice.domain.vo.Address;
import com.userservice.domain.vo.UserRole;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Getter
public class User {

	private String id;
	private String name;
	private String email;
	private String password;
	private String nickname;
	private UserRole role;
	//TODO: google에서 가져옴?
	private String profileUrl;
	private String oauthId;
	private Address address;
	private String phoneNumber;
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
		String oauthId,
		Address address,
		String phoneNumber,
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
		this.oauthId = oauthId;
		this.address = address;
		this.phoneNumber = phoneNumber;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public String getZipCode() {
		return this.address.getZipCode();
	}

	public String getState() {
		return this.address.getState();
	}

	public String getCity() {
		return this.address.getCity();
	}

	public String getDetails() {
		return this.address.getDetails();
	}

	public String getStreet() {
		return this.address.getStreet();
	}
}
