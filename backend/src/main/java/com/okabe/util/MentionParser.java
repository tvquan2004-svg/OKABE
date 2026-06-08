package com.okabe.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionParser {
    
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s@]+(?:\\s+[^\\s@]+)*)");

    /**
     * Extracts usernames from content string (e.g., "@john hello @jane" -> ["john", "jane"])
     */
    public static Set<String> extractUsernames(String content) {
        // Trích xuất danh sách username từ nội dung mention (vd: "@john @jane")
        Set<String> usernames = new HashSet<>(); // Dùng Set để tránh trùng lặp
        if (content == null || content.isEmpty()) {
            return usernames; // Trả về Set rỗng nếu content null hoặc rỗng
        }

        Matcher matcher = MENTION_PATTERN.matcher(content); // Tạo matcher từ pattern
        while (matcher.find()) { // Duyệt tất cả các mention tìm được
            usernames.add(matcher.group(1)); // group(1) lấy phần username (không bao gồm @)
        }
        
        return usernames;
    }
}
