package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.UserSearchDto;
import com.nexuslearn.api.security.CustomUserDetails;
import com.nexuslearn.api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDto>> searchUsers(@RequestParam String query, @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<UserSearchDto> results = userService.searchUsers(query, userDetails.user().getId());
        return ResponseEntity.ok(results);
    }
}