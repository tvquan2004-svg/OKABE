package com.okabe.service;

import com.okabe.dto.response.CommandResponse;
import com.okabe.security.UserPrincipal;

public interface CommandService {
    CommandResponse execute(String command, UserPrincipal currentUser);
}
