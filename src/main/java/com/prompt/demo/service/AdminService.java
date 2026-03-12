package com.prompt.demo.service;

import com.prompt.demo.dto.response.UserSummaryResponse;
import com.prompt.demo.entity.User;
import com.prompt.demo.exception.AppExceptions;
import com.prompt.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;

    public Page<UserSummaryResponse> listUsers(int page, int size) {
        return userRepository
                .findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(UserSummaryResponse::from);
    }

    @Transactional
    public UserSummaryResponse toggleStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("User not found: " + userId));

        user.setEnabled(!user.isEnabled());
        User saved = userRepository.save(user);

        log.info("Admin toggled user {} enabled={}", saved.getEmail(), saved.isEnabled());
        return UserSummaryResponse.from(saved);
    }
}