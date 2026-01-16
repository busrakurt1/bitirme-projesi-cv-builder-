package com.cvbuilder.external;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JobScraperClient {

    private static final Map<String, String[]> SITE_SELECTORS = new HashMap<>();
    
    static {
        // Kariyer.net için GÜNCELLENMİŞ selector'lar
        SITE_SELECTORS.put("kariyer.net", new String[]{
            // Ana içerik bölümleri (yeni yapı)
            "section[data-testid='job-detail-content']",
            "div.job-detail-content",
            "div.job-description",
            "div.description-content",
            "div[class*='jobDetailContent']",
            "div[class*='jobDescription']",
            
            // Fallback selector'lar
            "main#mainContent",
            "article",
            "div.content",
            
            // Meta tag'lardan bilgi çekme
            "meta[name='description']",
            "meta[property='og:description']"
        });
        
        SITE_SELECTORS.put("linkedin.com", new String[]{
            ".description__text", 
            ".show-more-less-html__markup",
            ".jobs-description__content"
        });
    }

    public Map<String, String> fetchJobData(String url) {
        Map<String, String> result = new HashMap<>();
        log.info("🌐 İlan çekiliyor: {}", url);
        
        try {
            // Daha detaylı header'lar ekleyelim
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0")
                    .header("Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .referrer("https://www.google.com/")
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .timeout(30000)
                    .maxBodySize(0) // Sınırı kaldır
                    .execute();

            Document doc = response.parse();
            result.put("pageTitle", doc.title());
            
            log.debug("📄 Sayfa başlığı: {}", doc.title());
            log.debug("📄 Sayfa URL: {}", response.url());
            log.debug("📄 Status Code: {}", response.statusCode());
            
            // Debug için tüm meta tag'ları loglayalım
            Elements allMeta = doc.select("meta");
            log.debug("📊 Toplam meta tag sayısı: {}", allMeta.size());
            
            // 1. Siteye özel selector ile içerik çekme
            String jobContent = extractJobSpecificContent(doc, url);
            
            // 2. Eğer özel içerik boşsa veya çok kısaysa yedek mekanizmalar
            if (jobContent == null || jobContent.trim().length() < 100) {
                log.warn("⚠️ Özel selector sonuç vermedi, alternatif yöntemler deneniyor...");
                
                // Alternatif 1: Ana içerik seçimi
                jobContent = tryAlternativeSelectors(doc);
                
                // Alternatif 2: Meta description
                if (jobContent.length() < 100) {
                    jobContent = extractMetaDescription(doc);
                }
                
                // Alternatif 3: Body temizleme (son çare)
                if (jobContent.length() < 100) {
                    jobContent = cleanBodyText(doc.body());
                }
                
                if (jobContent.length() < 50) {
                    log.error("❌ Tüm yöntemler başarısız oldu. HTML yapısını debug edin.");
                    // HTML yapısını logla (ilk 5000 karakter)
                    log.debug("🔍 HTML Önizleme:\n{}", doc.html().substring(0, Math.min(5000, doc.html().length())));
                }
            }

            result.put("jobContent", jobContent);
            result.put("location", extractFromMeta(doc, "location", "place", "og:description"));
            result.put("company", extractFromMeta(doc, "company", "organization", "og:title"));
            
            // Pozisyon bilgisini title'dan çıkarmaya çalış
            result.put("position", extractPositionFromTitle(doc.title(), jobContent));
            
            log.info("✅ İlan içeriği başarıyla çekildi (Karakter sayısı: {})", jobContent.length());
            return result;

        } catch (IOException e) {
            log.error("❌ İlan çekme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("İlan verisi çekilemedi: " + e.getMessage());
        }
    }
    
    private String extractJobSpecificContent(Document doc, String url) {
        String domain = extractDomain(url);
        
        if (SITE_SELECTORS.containsKey(domain)) {
            for (String selector : SITE_SELECTORS.get(domain)) {
                try {
                    Elements elements = doc.select(selector);
                    if (!elements.isEmpty()) {
                        StringBuilder content = new StringBuilder();
                        for (Element element : elements) {
                            content.append(element.text()).append("\n\n");
                        }
                        log.debug("✅ Selector bulundu: '{}' - {} element", selector, elements.size());
                        return content.toString().trim();
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Selector hatası '{}': {}", selector, e.getMessage());
                }
            }
        }
        
        return "";
    }
    
    private String tryAlternativeSelectors(Document doc) {
        String[] altSelectors = {
            "div[class*='content']",
            "div[class*='detail']",
            "div[class*='description']",
            "section",
            "article",
            "div.main",
            "div.container",
            "div.wrapper"
        };
        
        for (String selector : altSelectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                // Büyük metin bloklarını filtrele
                for (Element element : elements) {
                    String text = element.text();
                    if (text.length() > 300) { // Anlamlı içerik
                        log.debug("✅ Alternatif selector bulundu: '{}' - {} karakter", selector, text.length());
                        return text;
                    }
                }
            }
        }
        
        return "";
    }
    
    private String extractMetaDescription(Document doc) {
        Element metaDesc = doc.select("meta[name=description]").first();
        if (metaDesc != null) {
            return metaDesc.attr("content");
        }
        
        Element ogDesc = doc.select("meta[property='og:description']").first();
        if (ogDesc != null) {
            return ogDesc.attr("content");
        }
        
        return "";
    }
    
    private String cleanBodyText(Element body) {
        if (body == null) return "";
        
        // Klon oluştur
        Element content = body.clone();
        
        // Gereksiz elementleri temizle
        String[] removeSelectors = {
            "nav", "header", "footer", "script", "style", 
            "iframe", "aside", ".nav", ".menu", ".sidebar", 
            ".ads", ".advertisement", ".footer", ".header",
            ".social", ".share", ".comment", ".modal",
            "link", "meta", "noscript", "svg", "path"
        };
        
        for (String selector : removeSelectors) {
            content.select(selector).remove();
        }
        
        // Metni al ve temizle
        String text = content.text();
        
        // Çok kısa satırları ve gereksiz boşlukları temizle
        text = text.replaceAll("(?m)^[ \\t\\x0B\\f]*$", "\n"); // Boş satırları normalize et
        text = text.replaceAll("\\s+", " "); // Fazla boşlukları temizle
        
        return text.trim();
    }
    
    private String extractFromMeta(Document doc, String... keys) {
        for (String key : keys) {
            Element meta = doc.select("meta[name=" + key + "], meta[property=" + key + "]").first();
            if (meta != null) {
                String content = meta.attr("content");
                if (content != null && !content.trim().isEmpty()) {
                    return content;
                }
            }
        }
        return "";
    }
    
    private String extractPositionFromTitle(String title, String content) {
        // Title'dan pozisyon çıkarmaya çalış
        if (title != null && title.contains("|")) {
            return title.split("\\|")[0].trim();
        }
        
        // İçerikten anahtar kelimeler ara
        String[] keywords = {"stajyer", "developer", "mühendis", "uzman", "analist", "yönetici"};
        for (String keyword : keywords) {
            if (content.toLowerCase().contains(keyword)) {
                return keyword.substring(0, 1).toUpperCase() + keyword.substring(1);
            }
        }
        
        return "Belirtilmemiş";
    }
    
    private String extractDomain(String url) {
        try {
            java.net.URL uri = new java.net.URL(url);
            String host = uri.getHost();
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            
            // Ana domain'i al (subdomain'leri kaldır)
            for (Map.Entry<String, String[]> entry : SITE_SELECTORS.entrySet()) {
                if (host.contains(entry.getKey())) {
                    return entry.getKey();
                }
            }
            
            return host;
        } catch (Exception e) {
            return url;
        }
    }
}