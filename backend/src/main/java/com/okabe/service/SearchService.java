package com.okabe.service;

import com.okabe.dto.response.SearchResultItem;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface SearchService {
    // Tìm kiếm toàn bộ workspace (thẻ, board, thành viên) theo từ khoá
    List<SearchResultItem> globalSearch(String query, UserPrincipal currentUser);
}
