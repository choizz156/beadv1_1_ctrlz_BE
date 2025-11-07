package com.userservice.application.adapter.command;

import com.common.exception.CustomException;
import com.common.exception.vo.UserExceptionCode;
import com.userservice.application.port.in.UserCommandUseCase;
import com.userservice.domain.model.User;
import com.userservice.domain.vo.UserRole;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SellerVerificationContext {

	private final String phoneNumber;
	private final String userId;
	private String verificationCode;

	@Builder
	private SellerVerificationContext(String phoneNumber, String id, String verificationCode,
		UserCommandUseCase userCommandUseCase) {
		User user = userCommandUseCase.getUser(id);

		if (user.getRoles().contains(UserRole.SELLER)) {
			throw new CustomException(UserExceptionCode.ALREADY_SELLER.getMessage());
		}

		if (!user.getPhoneNumber().equals(phoneNumber)) {
			throw new CustomException(UserExceptionCode.NOT_YOUR_PHONE.getMessage());
		}

		this.userId = id;
		this.phoneNumber = phoneNumber;
	}

	public static SellerVerificationContext forSending(String phoneNumber, String id, UserCommandUseCase userCommandUseCase) {

		if (phoneNumber.isEmpty() || id.isEmpty()) {
			throw new CustomException("연락처 또는 id가 필요합니다.");
		}

		return SellerVerificationContext.builder()
			.userCommandUseCase(userCommandUseCase)
			.id(id)
			.phoneNumber(phoneNumber)
			.build();
	}

	public static SellerVerificationContext toVerify(String id, String verificationCode) {

		if (verificationCode.isEmpty() || id.isEmpty()) {
			throw new CustomException("연락처 또는 id가 필요합니다.");
		}

		return SellerVerificationContext.builder()
			.id(id)
			.verificationCode(verificationCode)
			.build();
	}


}
