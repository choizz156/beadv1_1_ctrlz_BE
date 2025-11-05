package com.userservice.infrastructure.jpa.vo;

import jakarta.persistence.Embeddable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Embeddable
public class EmbeddedAddress {

	private String zipCode;
	private String city;
	private String street;
	private String state;
	private String details;

	@Builder
	public EmbeddedAddress(String zipCode, String city, String street, String state, String details) {
		this.zipCode = zipCode;
		this.city = city;
		this.street = street;
		this.state = state;
		this.details = details;
	}
}
