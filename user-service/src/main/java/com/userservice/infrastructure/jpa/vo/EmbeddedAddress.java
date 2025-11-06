package com.userservice.infrastructure.jpa.vo;

import jakarta.persistence.Column;
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

	@Column(nullable = false)
	private String zipCode;
	@Column(nullable = false)
	private String city;
	@Column(nullable = false)
	private String street;
	@Column(nullable = false)
	private String state;
	@Column(nullable = false)
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
