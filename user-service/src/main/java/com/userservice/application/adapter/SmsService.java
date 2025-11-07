package com.userservice.application.adapter;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.common.exception.CustomException;
import com.common.exception.vo.UserExceptionCode;
import com.userservice.application.adapter.command.SellerVerificationContext;
import com.userservice.application.port.in.SellerVerificationUseCase;
import com.userservice.infrastructure.cache.vo.CacheType;
import com.userservice.infrastructure.sms.adapter.SmsClientAdapter;
import com.userservice.infrastructure.sms.utils.VerificationCodeSupplier;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * 재시도 캐시 유효 시간은 2분으로 설정하였습니다.
 * 인증번호 캐시 유효 시간은 1분으로 설정하였습니다.
 * 인증 제한 시간은 1일로 설정하였습니다.
 * {@link com.userservice.infrastructure.cache.Configuration.CacheConfiguration}
 */
@RequiredArgsConstructor
@Service
public class SmsService implements SellerVerificationUseCase {

	private final CacheManager cacheManager;
	private final SmsClientAdapter smsClientAdapter;

	private Cache verificationTryCache;
	private Cache verificationCodeCache;
	private Cache verificationVanCache;

	@PostConstruct
	public void init() {
		verificationTryCache = cacheManager.getCache(CacheType.VERIFICATION_TRY.name());
		verificationCodeCache = cacheManager.getCache(CacheType.VERIFICATION_CODE.name());
		verificationVanCache = cacheManager.getCache(CacheType.VERIFICATION_BAN_ONE_DAY.name());
	}

	@Override
	public void requestVerificationCode(SellerVerificationContext context) {
		String code = VerificationCodeSupplier.generateCode();

		checkExistingCode(context, code);

		verificationTryCache.put(context.getUserId(), code);
		smsClientAdapter.send(context.getPhoneNumber(), code);
	}

	@Override
	public void checkVerificationCode(SellerVerificationContext context) {

		String code = verificationCodeCache.get(context.getUserId(), String.class);

		if (code == null || code.isBlank()) {
			throw new CustomException("인증 코드가 존재하지 않습니다. 다시 시도해주세요");
		}

		if (!code.equals(context.getVerificationCode())) {
			applyVerificationCount(context.getUserId());
			throw new CustomException(UserExceptionCode.CODE_MISMATCH.getMessage());
		}
	}

	void checkExistingCode(SellerVerificationContext context, String code) {
		String oldCode = verificationCodeCache.get(context.getUserId(), String.class);
		if (oldCode != null) {
			verificationTryCache.put(context.getUserId(), code);
			throw new CustomException(UserExceptionCode.CODE_MISMATCH.getMessage());
		}
	}

	// 인증 횟수 추가/체크
	void applyVerificationCount(String userId) {

		if (verificationTryCache == null) {
			throw new RuntimeException("verificationTryCache 캐시 생성 오류");
		}

		AtomicInteger count = verificationTryCache.get(userId, AtomicInteger.class);
		if (count == null) {
			count = new AtomicInteger(1);
			verificationTryCache.put(userId, count);
		}

		int current = count.incrementAndGet();
		if (current >= 5) {
			verificationTryCache.evict(userId);
			verificationVanCache.put("ban_user", userId);
			throw new CustomException(UserExceptionCode.VERIFICATION_COUNT_LIMIT.getMessage());
		}
	}
}
