package com.okabe.service;

import com.okabe.entity.NotificationPreference;
import com.okabe.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public NotificationPreference getPreferences(Long userId) {
        return preferenceRepository.findById(userId)
                .orElseGet(() -> {
                    NotificationPreference defaultPref = new NotificationPreference();
                    defaultPref.setUserId(userId);
                    defaultPref.setEmailAssigned(true);
                    defaultPref.setEmailMentioned(true);
                    defaultPref.setEmailDueSoon(true);
                    defaultPref.setEmailInvited(true);
                    return defaultPref;
                });
    }

    @Transactional
    public NotificationPreference saveOrUpdatePreferences(Long userId, boolean emailAssigned, boolean emailMentioned, boolean emailDueSoon, boolean emailInvited) {
        NotificationPreference pref = preferenceRepository.findById(userId)
                .orElse(new NotificationPreference());
        
        pref.setUserId(userId);
        pref.setEmailAssigned(emailAssigned);
        pref.setEmailMentioned(emailMentioned);
        pref.setEmailDueSoon(emailDueSoon);
        pref.setEmailInvited(emailInvited);

        return preferenceRepository.save(pref);
    }
}
