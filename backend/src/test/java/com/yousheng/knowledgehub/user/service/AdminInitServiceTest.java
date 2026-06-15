package com.yousheng.knowledgehub.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yousheng.knowledgehub.config.AdminInitProperties;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserRole;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AdminInitServiceTest {

    @Autowired
    private AdminInitService adminInitService;

    @Autowired
    private AdminInitProperties adminInitProperties;

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM app_user");
        adminInitProperties.setEnabled(false);
        adminInitProperties.setUsername("admin");
        adminInitProperties.setPassword("admin123456");
        adminInitProperties.setNickname("admin");
    }

    @Test
    void initialize_disabled_noop() {
        adminInitProperties.setEnabled(false);

        adminInitService.initializeAdminIfNecessary();

        Long count = appUserMapper.selectCount(new LambdaQueryWrapper<>());
        assertEquals(0, count);
    }

    @Test
    void initialize_createsAdmin() {
        adminInitProperties.setEnabled(true);
        adminInitProperties.setUsername("admin");
        adminInitProperties.setPassword("SecurePass123");
        adminInitProperties.setNickname("Administrator");

        adminInitService.initializeAdminIfNecessary();

        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppUser::getUsername, "admin");
        AppUser admin = appUserMapper.selectOne(wrapper);

        assertNotNull(admin);
        assertEquals(UserRole.ADMIN.name(), admin.getRole());
        assertEquals(UserStatus.ENABLED.name(), admin.getStatus());
        assertEquals("Administrator", admin.getNickname());
        assertNotEquals("SecurePass123", admin.getPasswordHash());
        assertTrue(passwordEncoder.matches("SecurePass123", admin.getPasswordHash()));
    }

    @Test
    void initialize_blankNickname_defaultsToUsername() {
        adminInitProperties.setEnabled(true);
        adminInitProperties.setUsername("admin");
        adminInitProperties.setPassword("SecurePass123");
        adminInitProperties.setNickname(null);

        adminInitService.initializeAdminIfNecessary();

        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppUser::getUsername, "admin");
        AppUser admin = appUserMapper.selectOne(wrapper);

        assertNotNull(admin);
        assertEquals("admin", admin.getNickname());
    }

    @Test
    void initialize_adminExists_skipsCreation() {
        AppUser existingAdmin = new AppUser();
        existingAdmin.setUsername("admin");
        existingAdmin.setPasswordHash("old_hashed_password");
        existingAdmin.setNickname("Old Admin");
        existingAdmin.setRole(UserRole.ADMIN.name());
        existingAdmin.setStatus(UserStatus.ENABLED.name());
        appUserMapper.insert(existingAdmin);

        adminInitProperties.setEnabled(true);
        adminInitProperties.setUsername("admin");
        adminInitProperties.setPassword("NewPass123");

        adminInitService.initializeAdminIfNecessary();

        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppUser::getRole, UserRole.ADMIN.name());
        Long adminCount = appUserMapper.selectCount(wrapper);
        assertEquals(1, adminCount);

        AppUser admin = appUserMapper.selectOne(wrapper);
        assertEquals("old_hashed_password", admin.getPasswordHash());
    }

    @Test
    void initialize_usernameTakenByUser_throws() {
        AppUser regularUser = new AppUser();
        regularUser.setUsername("admin");
        regularUser.setPasswordHash("some_hash");
        regularUser.setNickname("Regular User");
        regularUser.setRole(UserRole.USER.name());
        regularUser.setStatus(UserStatus.ENABLED.name());
        appUserMapper.insert(regularUser);

        adminInitProperties.setEnabled(true);
        adminInitProperties.setUsername("admin");
        adminInitProperties.setPassword("SecurePass123");

        assertThrows(IllegalStateException.class, () -> adminInitService.initializeAdminIfNecessary());

        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppUser::getUsername, "admin");
        AppUser user = appUserMapper.selectOne(wrapper);
        assertNotNull(user);
        assertEquals(UserRole.USER.name(), user.getRole());
    }

    @ParameterizedTest
    @CsvSource(nullValues = {"NIL"}, value = {
            "NIL, SecurePass123",
            "'', SecurePass123",
            "ab, SecurePass123",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa, SecurePass123",
            "admin@123, SecurePass123",
            "admin, NIL",
            "admin, ''",
            "admin, 1234567",
            "admin, aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    })
    void initialize_invalidCredentials_throws(String username, String password) {
        adminInitProperties.setEnabled(true);
        adminInitProperties.setUsername(username);
        adminInitProperties.setPassword(password);

        assertThrows(IllegalStateException.class, () -> adminInitService.initializeAdminIfNecessary());
    }

    @Test
    void initialize_multipleEnabledAdmins_skipsCreation() {
        AppUser admin1 = new AppUser();
        admin1.setUsername("admin1");
        admin1.setPasswordHash("hash1");
        admin1.setNickname("Admin One");
        admin1.setRole(UserRole.ADMIN.name());
        admin1.setStatus(UserStatus.ENABLED.name());
        appUserMapper.insert(admin1);

        AppUser admin2 = new AppUser();
        admin2.setUsername("admin2");
        admin2.setPasswordHash("hash2");
        admin2.setNickname("Admin Two");
        admin2.setRole(UserRole.ADMIN.name());
        admin2.setStatus(UserStatus.ENABLED.name());
        appUserMapper.insert(admin2);

        adminInitProperties.setEnabled(true);
        adminInitProperties.setUsername("admin3");
        adminInitProperties.setPassword("SecurePass123");

        adminInitService.initializeAdminIfNecessary();

        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppUser::getRole, UserRole.ADMIN.name());
        wrapper.eq(AppUser::getStatus, UserStatus.ENABLED.name());
        Long adminCount = appUserMapper.selectCount(wrapper);
        assertEquals(2, adminCount);
    }

    @Test
    void initialize_nicknameTooLong_throws() {
        adminInitProperties.setEnabled(true);
        adminInitProperties.setUsername("admin");
        adminInitProperties.setPassword("SecurePass123");
        adminInitProperties.setNickname("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        assertThrows(IllegalStateException.class, () -> adminInitService.initializeAdminIfNecessary());
    }
}
