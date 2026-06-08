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
        if (templateRepository.findAllByIsSystemTrue().isEmpty()) { // Nếu chưa có template hệ thống nào
            log.info("Seeding system board templates..."); // Ghi log bắt đầu seed
            
            seedSoftwareSprint(); // Tạo template mẫu cho Software Sprint
            seedMarketingCampaign(); // Tạo template mẫu cho Marketing Campaign
            seedBugTracker(); // Tạo template mẫu cho Bug Tracker
            seedSimpleKanban(); // Tạo template mẫu cho Simple Kanban
            seedPersonalTasks(); // Tạo template mẫu cho Personal Tasks
            
            log.info("System board templates seeded successfully."); // Ghi log hoàn tất
        }
        seedDependencyWorkflow(); // Luôn tạo template Dependency Workflow
    }

    private void seedSoftwareSprint() {
        createSystemTemplate("Software Sprint", "Perfect for agile software development teams.", // Template cho quy trình Scrum/Agile
                List.of("Backlog", "To Do", "In Progress", "In Review", "Done"));
    }

    private void seedMarketingCampaign() {
        createSystemTemplate("Marketing Campaign", "Track your marketing strategy from ideas to publishing.", // Template cho chiến dịch Marketing
                List.of("Ideas", "Planning", "In Production", "Review", "Published"));
    }

    private void seedBugTracker() {
        createSystemTemplate("Bug Tracker", "Systematic way to track and resolve software bugs.", // Template cho theo dõi lỗi phần mềm
                List.of("Reported", "Confirmed", "In Progress", "Fixed", "Closed"));
    }

    private void seedSimpleKanban() {
        createSystemTemplate("Simple Kanban", "Clean and simple workflow for any project.", // Template Kanban đơn giản
                List.of("To Do", "Doing", "Done"));
    }

    private void seedPersonalTasks() {
        createSystemTemplate("Personal Tasks", "Manage your daily life and long-term goals.", // Template cho công việc cá nhân
                List.of("Today", "This Week", "Someday", "Done"));
    }

    private void seedDependencyWorkflow() {
        createSystemTemplate("Dependency Workflow", // Template minh hoạ quan hệ phụ thuộc DAG giữa các thẻ
                "Visualize task dependencies with DAG. Lists are named to auto-color nodes.",
                List.of("To Do", "In Progress", "Done"),
                Map.of(
                    0, List.of("Task A - Frontend (blocker)", "Task B - Backend (blocker)"),
                    1, List.of("Task C - Integration (blocked by A & B)")
                ));
    }

    private void createSystemTemplate(String name, String description, List<String> listNames) {
        createSystemTemplate(name, description, listNames, Map.of()); // Gọi overload không có thẻ mẫu
    }

    private void createSystemTemplate(String name, String description, List<String> listNames, Map<Integer, List<String>> listCards) {
        if (templateRepository.existsByNameAndIsSystemTrue(name)) return; // Bỏ qua nếu template đã tồn tại

        BoardTemplate template = BoardTemplate.builder()
                .name(name) // Tên template
                .description(description) // Mô tả template
                .isSystem(true) // Đánh dấu là template hệ thống
                .lists(new ArrayList<>()) // Khởi tạo danh sách list rỗng
                .build();

        for (int i = 0; i < listNames.size(); i++) { // Duyệt từng tên list để tạo
            TemplateList list = TemplateList.builder()
                    .template(template) // Gán template cha
                    .name(listNames.get(i)) // Tên list (vd: "To Do", "Done")
                    .position(i) // Vị trí sắp xếp
                    .cards(new ArrayList<>()) // Khởi tạo danh sách thẻ rỗng
                    .build();

            List<String> cardTitles = listCards.getOrDefault(i, List.of()); // Lấy danh sách thẻ mẫu cho list này
            for (int j = 0; j < cardTitles.size(); j++) { // Duyệt từng thẻ mẫu để tạo
                TemplateCard card = TemplateCard.builder()
                        .templateList(list) // Gán list cha
                        .title(cardTitles.get(j)) // Tiêu đề thẻ
                        .position(j) // Vị trí trong list
                        .build();
                list.getCards().add(card); // Thêm thẻ vào list
            }

            template.getLists().add(list); // Thêm list vào template
        }

        templateRepository.save(template); // Lưu template vào cơ sở dữ liệu
    }
}
