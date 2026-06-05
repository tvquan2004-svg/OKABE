package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItem {
    private String id;
    private String type;
    private String title;
    private String subtitle;
    private String breadcrumb;
    private String url;
    private String icon;
}
