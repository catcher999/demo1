package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.user.UpdatePasswordRequest;
import com.example.demo.dto.user.UpdateProfileRequest;
import com.example.demo.dto.user.UserVO;
import com.example.demo.service.points.PointsService;
import com.example.demo.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final PointsService pointsService;

    public UserController(UserService userService, PointsService pointsService) {
        this.userService = userService;
        this.pointsService = pointsService;
    }

    /** 获取当前用户信息 */
    @GetMapping("/profile")
    public ResponseEntity<Result<UserVO>> getProfile(
            @RequestAttribute("currentUserId") Long userId) {
        return ResponseEntity.ok(Result.success(userService.getProfile(userId)));
    }

    /** 修改个人信息（用户名） */
    @PutMapping("/profile")
    public ResponseEntity<Result<UserVO>> updateProfile(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(Result.success(userService.updateProfile(userId, request)));
    }

    /** 修改密码（首次设置 or 已有密码时修改） */
    @PutMapping("/password")
    public ResponseEntity<Result<Void>> updatePassword(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(Result.success(null));
    }

    /** 每日签到，领取算力 */
    @PostMapping("/sign")
    public ResponseEntity<Result<Map<String, Integer>>> sign(
            @RequestAttribute("currentUserId") Long userId) {
        int reward = pointsService.sign(userId);
        int balance = pointsService.getPoints(userId);
        return ResponseEntity.ok(Result.success(Map.of(
                "reward", reward,
                "balance", balance
        )));
    }
}
