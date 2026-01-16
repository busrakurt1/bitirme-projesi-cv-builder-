package com.cvbuilder.util;

public class TextCleaner {

    // Çok basit bir html temizleyici, sonra geliştiririz
    public static String cleanHtml(String html) {
        if (html == null) return "";
        // Tagleri sil
        String text = html.replaceAll("\\<.*?\\>", " ");
        // Fazla boşlukları düzelt
        return text.replaceAll("\\s+", " ").trim();
    }
}
