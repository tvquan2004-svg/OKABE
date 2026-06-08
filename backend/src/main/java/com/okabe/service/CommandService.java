package com.okabe.service;

import com.okabe.dto.response.CommandResponse;
import com.okabe.security.UserPrincipal;

public interface CommandService {
    // Thực thi lệnh nhập từ người dùng (ví dụ: /create-card, /assign)
    CommandResponse execute(String command, UserPrincipal currentUser);
}
