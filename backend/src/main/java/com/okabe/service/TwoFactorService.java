package com.okabe.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.okabe.entity.User;
import com.okabe.repository.BackupCodeRepository;
import com.okabe.entity.BackupCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class TwoFactorService {

    private final GoogleAuthenticator gAuth;
    private final BackupCodeRepository backupCodeRepository;
    private final PasswordEncoder passwordEncoder;

    public TwoFactorService(BackupCodeRepository backupCodeRepository, PasswordEncoder passwordEncoder) {
        this.backupCodeRepository = backupCodeRepository;
        this.passwordEncoder = passwordEncoder;
        
        com.warrenstrange.googleauth.GoogleAuthenticatorConfig config = 
            new com.warrenstrange.googleauth.GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setWindowSize(3)
                .build();
        this.gAuth = new GoogleAuthenticator(config);
    }

    public String generateNewSecret() {
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    public String getQrCodeUri(String secret, String email) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", 
                "OKABE", email, secret, "OKABE");
    }

    public boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }

    @Transactional
    public List<String> generateBackupCodes(User user) {
        try {
            // Clear existing backup codes
            backupCodeRepository.deleteByUserId(user.getId());
            
            List<String> rawCodes = new ArrayList<>();
            Random random = new Random();
            for (int i = 0; i < 8; i++) {
                String code = String.format("%08d", random.nextInt(100000000));
                rawCodes.add(code);
                
                BackupCode backupCode = BackupCode.builder()
                        .user(user)
                        .codeHash(passwordEncoder.encode(code))
                        .isUsed(false)
                        .build();
                backupCodeRepository.save(backupCode);
                System.out.println("SUCCESS: Saved backup code " + (i + 1) + "/8");
            }
            return rawCodes;
        } catch (Exception e) {
            System.err.println("FATAL ERROR in generateBackupCodes: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public boolean verifyBackupCode(User user, String code) {
        List<BackupCode> codes = backupCodeRepository.findByUserAndIsUsedFalse(user);
        for (BackupCode bc : codes) {
            if (passwordEncoder.matches(code, bc.getCodeHash())) {
                bc.setIsUsed(true);
                backupCodeRepository.save(bc);
                return true;
            }
        }
        return false;
    }
}
