package com.cvbuilder.service;

import com.cvbuilder.dto.UserDTO;
import com.cvbuilder.dto.UserRequest;

import java.util.List;

public interface UserService {

    UserDTO registerUser(UserRequest userRequest);

    UserDTO loginUser(String email, String password);

    List<UserDTO> getAllUsers();

    UserDTO getUserProfile(Long id);

    UserDTO updateUserProfile(Long id, UserRequest userRequest);

    void deleteUser(Long id);

    boolean existsByEmail(String email);
}
