package com.user.application.adapter.vo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CommandType {
	USER_SIGNUP_COMMAND("user signup command userId : ");

	private final String value;

	public String getContentWithUserId(String userId) {
		return this.value + userId;
	}
}
