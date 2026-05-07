package com.okabe.config;

import com.okabe.entity.BoardTemplate;
import com.okabe.entity.TemplateCard;
import com.okabe.entity.TemplateList;
import com.okabe.repository.BoardTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardTemplateDataInitializer implements CommandLineRunner {

    private final BoardTemplateRepository templateRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (templateRepository.findAllByIsSystemTrue().isEmpty()) {
            log.info("Seeding system board templates...");
            
            seedSoftwareSprint();
            seedMarketingCampaign();
            seedBugTracker();
            seedSimpleKanban();
            seedPersonalTasks();
            
            log.info("System board templates seeded successfully.");
        }
    }

    private void seedSoftwareSprint() {
        createSystemTemplate("Software Sprint", "Perfect for agile software development teams.",
                List.of("Backlog", "To Do", "In Progress", "In Review", "Done"));
    }

    private void seedMarketingCampaign() {
        createSystemTemplate("Marketing Campaign", "Track your marketing strategy from ideas to publishing.",
                List.of("Ideas", "Planning", "In Production", "Review", "Published"));
    }

    private void seedBugTracker() {
        createSystemTemplate("Bug Tracker", "Systematic way to track and resolve software bugs.",
                List.of("Reported", "Confirmed", "In Progress", "Fixed", "Closed"));
    }

    private void seedSimpleKanban() {
        createSystemTemplate("Simple Kanban", "Clean and simple workflow for any project.",
                List.of("To Do", "Doing", "Done"));
    }

    private void seedPersonalTasks() {
        createSystemTemplate("Personal Tasks", "Manage your daily life and long-term goals.",
                List.of("Today", "This Week", "Someday", "Done"));
    }

    private void createSystemTemplate(String name, String description, List<String> listNames) {
        if (templateRepository.existsByNameAndIsSystemTrue(name)) return;

        BoardTemplate template = BoardTemplate.builder()
                .name(name)
                .description(description)
                .isSystem(true)
                .lists(new ArrayList<>())
                .build();

        for (int i = 0; i < listNames.size(); i++) {
            TemplateList list = TemplateList.builder()
                    .template(template)
                    .name(listNames.get(i))
                    .position(i)
                    .cards(new ArrayList<>())
                    .build();
            template.getLists().add(list);
        }

        templateRepository.save(template);
    }
}
