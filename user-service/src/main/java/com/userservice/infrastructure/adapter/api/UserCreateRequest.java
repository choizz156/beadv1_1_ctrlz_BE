package com.userservice.infrastructure.adapter.api;

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
