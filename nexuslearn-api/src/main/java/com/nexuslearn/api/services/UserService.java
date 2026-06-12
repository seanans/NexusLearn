package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.UserSearchDto;
import com.nexuslearn.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserSearchDto> searchUsers(String query, UUID currentUserId) {
        return userRepository.searchUsersExcludingSelf(query, currentUserId, PageRequest.of(0, 10))
                .stream()
                .map(u -> new UserSearchDto(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail()))
                .toList();
    }
}