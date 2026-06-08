package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class DependencyGraphResponse {
    private CardInfoResponse card; // Thẻ hiện tại
    private List<CardInfoResponse> blockedBy; // Danh sách thẻ chặn thẻ này
    private List<CardInfoResponse> blocking; // Danh sách thẻ bị thẻ này chặn
}
