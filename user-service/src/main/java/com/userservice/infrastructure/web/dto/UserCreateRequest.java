package com.userservice.infrastructure.web.dto;

public record UserCreateRequest(
	String email,
	String phoneNumber,
	String street,
	String zipCode,
	String state,
	String details,
	String name,
	String password
) {
}
