package com.example.esportscalendar.service;

import com.example.esportscalendar.domain.User;
import com.example.esportscalendar.dto.UserSignupRequest;
import com.example.esportscalendar.dto.UserLoginRequest;
import com.example.esportscalendar.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // 회원가입
    public void registerUser(UserSignupRequest request) {

        // loginId: 알파벳만 허용
        if (!request.getLoginId().matches("^[a-zA-Z]+$")) {
            throw new IllegalArgumentException("ID는 영어 알파벳만 사용할 수 있습니다.");
        }

        // password: 영어 + 숫자, 6자 이상
        if (!request.getPassword().matches("^(?=.*[a-zA-Z])(?=.*\\d).{6,}$")) {
            throw new IllegalArgumentException("비밀번호는 영어+숫자 조합으로 6자 이상이어야 합니다.");
        }

        // 중복 ID 방지
        if (userRepository.findByloginId(request.getLoginId()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 ID입니다.");
        }

        // 유효한 팀인지 확인
        List<String> validTeams = List.of("T1", "Gen.G", "DK", "HLE", "KT", "NS", "BRO", "DRX", "KDF", "LSB");
        if (!validTeams.contains(request.getTeamName())) {
            throw new IllegalArgumentException("존재하지 않는 팀입니다.");
        }

        // username은 loginId로 설정
        User user = new User(
                request.getLoginId(),
                request.getPassword(),
                request.getLoginId(), // username = loginId
                request.getTeamName()
        );

        userRepository.save(user);
    }

    // 로그인
    public String login(UserLoginRequest request) {
        User user = userRepository.findByloginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ID입니다."));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return "로그인 성공: " + user.getUsername();
    }

    // 전체 유저 조회
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    // 특정 유저 조회
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다. ID=" + id));
    }
}
