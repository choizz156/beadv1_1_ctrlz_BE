package com.userservice.domain.vo;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Getter
public class Address {

	private String zipCode;
	private String city;
	private String street;
	private String state;
	private String details;

	@Builder
	public Address(String zipCode, String city, String street, String state, String details) {
		this.zipCode = zipCode;
		this.city = city;
		this.street = street;
		this.state = state;
		this.details = details;
	}
}
