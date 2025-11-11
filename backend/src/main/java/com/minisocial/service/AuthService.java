package com.minisocial.service;

import com.minisocial.dto.LoginRequest;
import com.minisocial.dto.LoginResponse;
import com.minisocial.dto.RegisterRequest;
import com.minisocial.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
