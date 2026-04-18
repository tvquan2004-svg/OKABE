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
        String adminEmail = "admin@okabe.com";
        
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin account already exists.");
            return;
        }

        // Create Admin User
        User adminUser = User.builder()
                .email(adminEmail)
                .username("Admin User")
                .password(passwordEncoder.encode("admin123"))
                .provider("LOCAL")
                .isActive(true)
                .build();
                
        adminUser = userRepository.save(adminUser);
        log.info("Created admin user: admin@okabe.com / admin123");

        // Find the first workspace to add them to, to test RBAC
        List<Workspace> workspaces = workspaceRepository.findAll();
        if (!workspaces.isEmpty()) {
            Workspace firstWorkspace = workspaces.get(0);
            
            if (!memberRepository.existsByWorkspaceIdAndUserId(firstWorkspace.getId(), adminUser.getId())) {
                WorkspaceMember member = WorkspaceMember.builder()
                        .workspaceId(firstWorkspace.getId())
                        .userId(adminUser.getId())
                        .role(Role.ADMIN) // Give them ADMIN role
                        .build();
                memberRepository.save(member);
                log.info("Added Admin user to workspace '{}' with role ADMIN", firstWorkspace.getName());
            }
        }
    }
}
