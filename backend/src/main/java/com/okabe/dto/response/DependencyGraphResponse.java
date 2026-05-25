package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyGraphResponse {
    private CardInfoResponse card;
    private List<CardInfoResponse> blockedBy;
    private List<CardInfoResponse> blocking;
}
