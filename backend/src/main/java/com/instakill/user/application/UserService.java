package com.instakill.user.application;

import com.instakill.common.error.NotFoundException;
import com.instakill.user.domain.User;
import com.instakill.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public User getById(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
