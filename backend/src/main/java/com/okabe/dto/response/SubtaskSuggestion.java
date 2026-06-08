package com.okabe.dto.response;

public record SubtaskSuggestion(String title, // Tiêu đề tác vụ con được gợi ý
                                  double estimatedHours) { // Số giờ ước tính
}
