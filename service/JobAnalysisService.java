package com.cvbuilder.service;

import com.cvbuilder.dto.JobAnalysisResponse;
import com.cvbuilder.dto.MarketAnalysisResponse;
import com.cvbuilder.entity.JobPosting;
import java.util.List;

public interface JobAnalysisService {

    /**
     * Kullanıcının yapıştırdığı ham metni analiz eder (Bot engelini aşmak için en güvenli yol).
     */
    JobAnalysisResponse analyzeJobByRawText(Long userId, String jobContent);

    /**
     * Tekil bir iş ilanı URL'ini analiz eder (Bot koruması olmayan siteler için).
     */
    JobAnalysisResponse analyzeJobPosting(Long userId, String url);

    /**
     * Pazar analizi yapar.
     */
    MarketAnalysisResponse performMarketAnalysis(String area, Long userId);

    /**
     * Belirli bir kullanıcıya ait geçmiş analizleri listeler.
     */
    List<JobPosting> getJobsByUserId(Long userId);

    /**
     * Veritabanından ID ile spesifik bir iş ilanı kaydını getirir.
     */
    JobPosting getJobById(Long jobId);

	JobAnalysisResponse analyzeJobPosting(Object object, String url);
}