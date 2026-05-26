package com.yousheng.knowledgehub.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yousheng.knowledgehub.auth.dto.RegisterRequest;
import com.yousheng.knowledgehub.auth.dto.RegisterResponse;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private AppUserMapper appUserMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("DELETE FROM app_user");
	}

	@Test
	void register_success() {
		RegisterRequest request = new RegisterRequest("alice", "Password123", "Alice");

		RegisterResponse response = authService.register(request);

		assertNotNull(response.id());
		assertEquals("alice", response.username());
		assertEquals("Alice", response.nickname());

		LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(AppUser::getUsername, "alice");
		AppUser savedUser = appUserMapper.selectOne(wrapper);

		assertNotNull(savedUser);
		assertNotEquals("Password123", savedUser.getPasswordHash());
		assertTrue(passwordEncoder.matches("Password123", savedUser.getPasswordHash()));
	}

	@Test
	void register_duplicateUsername() {
		RegisterRequest first = new RegisterRequest("bob", "Password123", "Bobby");
		RegisterRequest duplicate = new RegisterRequest("bob", "Password456", "Bob 2");

		authService.register(first);

		BizException ex = assertThrows(BizException.class, () -> authService.register(duplicate));
		assertEquals(ErrorCode.USERNAME_EXISTS, ex.getErrorCode());
	}
}
