package com.okabe.config;

import com.okabe.entity.User;
import com.okabe.entity.Workspace;
import com.okabe.entity.WorkspaceMember;
import com.okabe.entity.enums.Role;
import com.okabe.repository.UserRepository;
import com.okabe.repository.WorkspaceMemberRepository;
import com.okabe.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String adminEmail = "admin@okabe.com"; // Email mặc định của tài khoản admin
        
        if (userRepository.existsByEmail(adminEmail)) { // Nếu admin đã tồn tại thì bỏ qua
            log.info("Admin account already exists.");
            return;
        }

        // Create Admin User
        User adminUser = User.builder()
                .email(adminEmail) // Email admin
                .username("Admin User") // Tên hiển thị
                .password(passwordEncoder.encode("admin123")) // Mã hoá mật khẩu trước khi lưu
                .provider("LOCAL") // Nhà cung cấp xác thực nội bộ
                .isActive(true) // Kích hoạt tài khoản
                .build();
                
        adminUser = userRepository.save(adminUser); // Lưu admin vào DB
        log.info("Created admin user: admin@okabe.com / admin123");

        // Find the first workspace to add them to, to test RBAC
        List<Workspace> workspaces = workspaceRepository.findAll(); // Lấy tất cả workspace
        if (!workspaces.isEmpty()) { // Nếu có ít nhất một workspace
            Workspace firstWorkspace = workspaces.get(0); // Lấy workspace đầu tiên
            
            if (!memberRepository.existsByWorkspaceIdAndUserId(firstWorkspace.getId(), adminUser.getId())) { // Nếu admin chưa là thành viên
                WorkspaceMember member = WorkspaceMember.builder()
                        .workspaceId(firstWorkspace.getId()) // Gán workspace
                        .userId(adminUser.getId()) // Gán user admin
                        .role(Role.ADMIN) // Give them ADMIN role // Gán quyền ADMIN để kiểm tra RBAC
                        .build();
                memberRepository.save(member); // Lưu member vào DB
                log.info("Added Admin user to workspace '{}' with role ADMIN", firstWorkspace.getName());
            }
        }
    }
}
