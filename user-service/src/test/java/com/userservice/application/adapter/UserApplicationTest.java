package com.userservice.application.adapter;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.common.exception.CustomException;

@DataJpaTest
class UserApplicationTest {

	@DisplayName("회원 가입 시, 닉네임이 중복되면 예외를 던진다.")
	@Test
	void test1() throws Exception {
		//given
		UserApplication userApplication = new UserApplication(new FakeRepository(), null, null);
		//when//then
		assertThatThrownBy(() -> userApplication.verifyNickname("test_nickname"))
			.isInstanceOf(CustomException.class);
	}

	@DisplayName("회원 가입 시, 연락처가 중복되면 예외를 던진다.")
	@Test
	void test2() throws Exception {
		//given
		UserApplication userApplication = new UserApplication(new FakeRepository(), null, null);
		//when//then
		assertThatThrownBy(() -> userApplication.verifyPhoneNumber("010-1111-1111"))
			.isInstanceOf(CustomException.class);
	}
}