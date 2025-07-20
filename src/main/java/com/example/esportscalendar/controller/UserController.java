package com.example.esportscalendar.controller;

import com.example.esportscalendar.domain.User;
import com.example.esportscalendar.dto.UserSignupRequest;
import com.example.esportscalendar.dto.UserLoginRequest;
import com.example.esportscalendar.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // ✅ 회원가입: loginId + password + teamName
    @PostMapping("/signup")
    public String signup(@RequestBody UserSignupRequest request) {
        userService.registerUser(request);
        return "회원가입 완료";
    }

    // ✅ 로그인: loginId + password
    @PostMapping("/login")
    public String login(@RequestBody UserLoginRequest request) {
        return userService.login(request);
    }

    // ✅ 전체 유저 조회
    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAllUsers();
    }

    // ✅ 단일 유저 조회
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findUserById(id);
    }
}
