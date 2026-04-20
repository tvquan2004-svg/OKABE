package com.okabe.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionParser {
    
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    /**
     * Extracts usernames from content string (e.g., "@john hello @jane" -> ["john", "jane"])
     */
    public static Set<String> extractUsernames(String content) {
        Set<String> usernames = new HashSet<>();
        if (content == null || content.isEmpty()) {
            return usernames;
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }
        
        return usernames;
    }
}
