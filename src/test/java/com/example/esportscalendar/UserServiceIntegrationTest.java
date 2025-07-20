package com.example.esportscalendar;

import com.example.esportscalendar.domain.User;
import com.example.esportscalendar.dto.UserLoginRequest;
import com.example.esportscalendar.dto.UserSignupRequest;
import com.example.esportscalendar.repository.UserRepository;
import com.example.esportscalendar.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void 회원가입_성공() {
        // Given
        UserSignupRequest request = new UserSignupRequest();
        request.setLoginId("abcde");
        request.setPassword("abc123");
        request.setTeamName("T1");

        // When
        userService.registerUser(request);

        // Then
        User saved = userRepository.findByloginId("abcde").orElseThrow();
        assertThat(saved.getTeamName()).isEqualTo("T1");
        assertThat(saved.getPassword()).isEqualTo("abc123");
    }

    @Test
    public void 로그인_성공() {
        // Given
        UserSignupRequest request = new UserSignupRequest();
        request.setLoginId("loginid");
        request.setPassword("pw1234");
        request.setTeamName("Gen.G");
        userService.registerUser(request);

        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setLoginId("loginid");
        loginRequest.setPassword("pw1234");

        // When
        String result = userService.login(loginRequest);

        // Then
        assertThat(result).isEqualTo("로그인 성공: loginid");
    }
}

