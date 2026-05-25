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
import java.util.Map;

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
        seedDependencyWorkflow();
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

    private void seedDependencyWorkflow() {
        createSystemTemplate("Dependency Workflow",
                "Visualize task dependencies with DAG. Lists are named to auto-color nodes.",
                List.of("To Do", "In Progress", "Done"),
                Map.of(
                    0, List.of("Task A - Frontend (blocker)", "Task B - Backend (blocker)"),
                    1, List.of("Task C - Integration (blocked by A & B)")
                ));
    }

    private void createSystemTemplate(String name, String description, List<String> listNames) {
        createSystemTemplate(name, description, listNames, Map.of());
    }

    private void createSystemTemplate(String name, String description, List<String> listNames, Map<Integer, List<String>> listCards) {
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

            List<String> cardTitles = listCards.getOrDefault(i, List.of());
            for (int j = 0; j < cardTitles.size(); j++) {
                TemplateCard card = TemplateCard.builder()
                        .templateList(list)
                        .title(cardTitles.get(j))
                        .position(j)
                        .build();
                list.getCards().add(card);
            }

            template.getLists().add(list);
        }

        templateRepository.save(template);
    }
}
