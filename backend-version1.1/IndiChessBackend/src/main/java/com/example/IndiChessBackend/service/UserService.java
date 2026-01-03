package com.example.IndiChessBackend.service;

import com.example.IndiChessBackend.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;

    public String getUserbyUsername(String username) {
        return userRepo.getUserByUsername(username).getUsername();
    }
}
