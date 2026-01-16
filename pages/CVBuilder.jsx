// src/pages/CVBuilder.jsx
import { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom"; // useLocation Eklendi
import { useTheme } from "../contexts/ThemeContext";
import axios from "axios";
import CVPreview from "../components/cv/CVPreview";
import PDFService from "../services/pdfService";
import { profileAPI, cvAPI, jobAPI } from "../services/api";

const CVBuilder = () => {
  // ================= STATE YÖNETİMİ =================
  const [originalUser, setOriginalUser] = useState(null); // TR orijinal veri
  const [translatedUser, setTranslatedUser] = useState(null); // EN çevrilmiş veri
  const [aiData, setAiData] = useState(null); // AI tarafından üretilen veri
  const [latestJob, setLatestJob] = useState(null); // Kullanıcının son analiz ettiği iş ilanı

  const [aiLoading, setAiLoading] = useState(false);
  const [pdfLoading, setPdfLoading] = useState(false);
  const [isTranslating, setIsTranslating] = useState(false);
  const [loadingJob, setLoadingJob] = useState(false); // İlan yükleme durumu

  const [language, setLanguage] = useState("tr"); // "tr" | "en"
  
  const navigate = useNavigate();
  const location = useLocation(); // Location hook'u (Sayfa değişimlerini dinlemek için)
  const { theme } = useTheme();

  // ================= BAŞLANGIÇ VERİLERİNİ YÜKLE =================
  useEffect(() => {
    const init = async () => {
      const userData = localStorage.getItem("user");
      if (!userData) {
        navigate("/");
        return;
      }

      const authUser = JSON.parse(userData);

      try {
        // 1. Profil Verisini Çek
        const res = await profileAPI.getMe();
        const profile = res.data || {};
        setOriginalUser({
          ...authUser,
          profile,
          ...profile
        });

        // 2. En Son İş İlanını Çek
        await loadLatestJob(authUser.id);

        // LocalStorage temizliği (İş analizi sayfasından gelindiyse flag'i temizle)
        localStorage.removeItem("lastJobAnalyzed");

      } catch (err) {
        console.error("Profil yüklenemedi:", err);
        setOriginalUser(authUser);
      }
    };

    init();
    
    // location dependency: Sayfaya geri dönüldüğünde useEffect tekrar çalışır
  }, [navigate, location]);

  // En son iş ilanını yükleyen fonksiyon (Geliştirilmiş Sıralama)
  const loadLatestJob = async (userId) => {
    try {
      setLoadingJob(true);
      const response = await jobAPI.getUserJobs(userId);
      
      if (response.data && response.data.length > 0) {
        // En son eklenen ilanı al (createdAt'e göre sırala: yeniden eskiye)
        const sorted = [...response.data].sort((a, b) => 
          new Date(b.createdAt) - new Date(a.createdAt)
        );
        console.log("Son analiz edilen ilan:", sorted[0].position);
        setLatestJob(sorted[0]);
      } else {
        setLatestJob(null);
      }
    } catch (error) {
      console.error("İş ilanı yüklenemedi:", error);
    } finally {
      setLoadingJob(false);
    }
  };

  // Manuel İlan Yenileme
  const handleRefreshJob = () => {
    if (originalUser?.id) {
      loadLatestJob(originalUser.id);
    }
  };

  // ================= DİL DEĞİŞTİRME =================
  const handleLanguageChange = async (targetLang) => {
    if (targetLang === language) return;
    setLanguage(targetLang);

    if (targetLang === "tr") {
      return;
    }

    // EN seçildiyse ve daha önce çevrilmemişse çevir
    if (targetLang === "en" && !translatedUser) {
      await translateCVData();
    }
  };

  // ================= ÇEVİRİ FONKSİYONU =================
  const translateCVData = async () => {
    if (!originalUser) {
      alert("Önce profilinizin yüklenmesini bekleyin.");
      return;
    }

    setIsTranslating(true);
    try {
      // payload: originalUser kopyası
      let payload = JSON.parse(JSON.stringify(originalUser));

      // Eğer aiData varsa, AI tarafından düzenlenen alanları üstüne yaz
      if (aiData) {
        if (aiData.summary) payload.summary = aiData.summary;

        if (aiData.optimizedExperiences && aiData.optimizedExperiences.length > 0) {
          payload.experiences = aiData.optimizedExperiences;
        }

        if (aiData.optimizedProjects && aiData.optimizedProjects.length > 0) {
          payload.projects = aiData.optimizedProjects;
        } else if (aiData.optimizedUserProjects && aiData.optimizedUserProjects.length > 0) {
          payload.projects = aiData.optimizedUserProjects;
        }

        if (aiData.skills && aiData.skills.length > 0) payload.skills = aiData.skills;
        if (aiData.languages && aiData.languages.length > 0) payload.languages = aiData.languages;
        if (aiData.certificates && aiData.certificates.length > 0) payload.certificates = aiData.certificates;
        if (aiData.optimizedEducation && aiData.optimizedEducation.length > 0) payload.education = aiData.optimizedEducation;
      }

      // Backend API'ye gönder
      const response = await axios.post(
        `http://localhost:8080/api/cv-generator/translate?lang=en`,
        payload
      );

      if (response.data) {
        console.log("Çeviri Başarılı:", response.data);
        setTranslatedUser(response.data);
      }
    } catch (error) {
      console.error("Çeviri hatası:", error);
      alert("Çeviri servisine ulaşılamadı.");
      setLanguage("tr");
    } finally {
      setIsTranslating(false);
    }
  };

  // ================= AI OPTIMIZE FONKSİYONU =================
  const handleAiGenerate = async () => {
    if (!originalUser?.id) {
      alert("Kullanıcı bilgisi bulunamadı.");
      return;
    }

    // Eğer hiç iş ilanı yoksa uyar
    if (!latestJob) {
      alert("Lütfen önce 'İş Analizi' sayfasından bir ilan analiz edin.");
      navigate("/job-analysis");
      return;
    }

    setAiLoading(true);
    try {
      const userId = originalUser.id;
      
      // Backend'e sadece userId gönderiyoruz. 
      // Backend otomatik olarak 'null' job ID varsayımıyla en son ilanı kullanacak.
      const response = await cvAPI.generateCV(userId); 

      if (response.data) {
        const data = response.data.data || response.data;
        const summaries = data.tailoredSummaries || [];

        setAiData({
          summaries: summaries,
          summary: data.tailoredSummary || (summaries.length > 0 ? summaries[0] : ""),
          skills: data.prioritizedSkills || [],
          optimizedExperiences: data.optimizedExperiences || [],
          optimizedProjects: data.optimizedProjects || [],
          optimizedUserProjects: data.optimizedUserProjects || [],
          languages: data.optimizedLanguages || [],
          certificates: data.optimizedCertificates || [],
          optimizedEducation: data.optimizedEducation || [],
          jobUsed: latestJob // UI'da göstermek için kullanılan ilanı sakla
        });

        // AI yeni içerik ürettiği için eski İngilizce çeviriyi sıfırla
        setTranslatedUser(null);

        alert(`CV başarıyla optimize edildi! ✨\nKullanılan İlan: ${latestJob?.position}`);
      }
    } catch (error) {
      console.error("AI Hatası:", error);
      
      // Daha detaylı hata mesajı
      let errorMessage = "AI servisine ulaşılamadı. Backend konsolunu kontrol edin.";
      
      if (error.response) {
        // Backend'den gelen hata mesajı
        const errorData = error.response.data;
        if (errorData?.message) {
          errorMessage = `Hata: ${errorData.message}`;
        } else if (errorData?.error) {
          errorMessage = `Hata: ${errorData.error}`;
        } else if (error.response.status === 400) {
          errorMessage = "Geçersiz istek. Kullanıcı bilgilerinizi kontrol edin.";
        } else if (error.response.status === 500) {
          errorMessage = "Sunucu hatası. Lütfen daha sonra tekrar deneyin.";
        }
      } else if (error.request) {
        errorMessage = "Backend'e ulaşılamadı. Backend servisinin çalıştığından emin olun.";
      }
      
      alert(errorMessage);
    } finally {
      setAiLoading(false);
    }
  };

  // ================= PDF İNDİRME =================
  const handleExportPDF = async () => {
    if (!originalUser) return;
    setPdfLoading(true);
    try {
      // Aktif veri: eğer EN ve translatedUser varsa onu, değilse aiData veya originalUser
      const activeData = (language === "en" && translatedUser) ? translatedUser : (aiData || originalUser);

      const dataToPrint = {
        ...activeData,
        language: language,
        linkedinUrl: activeData.linkedinUrl || activeData.profile?.linkedinUrl,
        githubUrl: activeData.githubUrl || activeData.profile?.githubUrl,
        websiteUrl: activeData.websiteUrl || activeData.profile?.websiteUrl,
      };

      await PDFService.generateCVPDF(dataToPrint);
    } catch (error) {
      console.error("PDF Hatası:", error);
      alert("PDF oluşturulurken hata oluştu.");
    } finally {
      setPdfLoading(false);
    }
  };

  const handleEditProfile = () => navigate("/profile");

  if (!originalUser) return <div style={{ padding: "20px", textAlign: "center" }}>Yükleniyor...</div>;

  // Preview'de gösterilecek aktif kullanıcı verisi
  const activeUser = (language === "en" && translatedUser) ? translatedUser : originalUser;

  return (
    <div style={{ 
      minHeight: "100vh", 
      background: theme === 'light' ? "#e3f2fd" : "#1a202c",
      padding: "0",
      fontFamily: '"Segoe UI", sans-serif' 
    }}>
      
      {/* --- HEADER --- */}
      <div style={{ 
        background: theme === 'light' ? "white" : "#2d3748", 
        padding: "16px 30px", 
        borderBottom: theme === 'light' ? "1px solid #e0e0e0" : "1px solid rgba(255, 255, 255, 0.1)"
      }}>
        <div style={{ 
          maxWidth: "1200px", 
          margin: "0 auto", 
          display: "flex", 
          justifyContent: "space-between", 
          alignItems: "center"
        }}>
          {/* SOL: BAŞLIK VE DASHBOARD BUTONU */}
          <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
            <button
              onClick={() => navigate("/dashboard")}
              style={{
                padding: "6px 12px",
                background: "transparent",
                color: theme === 'light' ? "#667eea" : "#a8b5ff",
                border: `1px solid ${theme === 'light' ? "#667eea" : "#a8b5ff"}`,
                borderRadius: "6px",
                cursor: "pointer",
                fontWeight: 500,
                fontSize: "13px"
              }}
            >
             ← Dashboard'a Dön
            </button>
            <h1 style={{ fontSize: "20px", margin: 0, fontWeight: 600, color: theme === 'light' ? "#2c3e50" : "#ffffff" }}>
              {/* CV Oluşturucu */}
            </h1>
          </div>

          {/* SAĞ: AKSIYON BUTONLARI */}
          <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
            {/* İlan Analiz Et butonu - sadece CV optimize edilmediğinde göster */}
            {!aiData && (
              <button 
                onClick={() => navigate("/job-analysis")}
                style={{ 
                  padding: "8px 16px", 
                  background: "#3b82f6", 
                  color: "white", 
                  border: "none", 
                  borderRadius: "6px", 
                  cursor: "pointer",
                  fontWeight: 500,
                  fontSize: "13px",
                  position: "relative"
                }}
                title="İş ilanı linkini analiz ederek CV'nizi optimize edebilirsiniz"
              >
                İlan Analiz Et
              </button>
            )}

            <div 
              style={{ 
                display: "flex", 
                background: theme === 'light' ? "#f8f9fa" : "rgba(255, 255, 255, 0.1)", 
                padding: "2px", 
                borderRadius: "6px", 
                border: theme === 'light' ? "1px solid #e0e0e0" : "1px solid rgba(255, 255, 255, 0.2)"
              }}
              title="CV dilini seçin (Türkçe veya İngilizce)"
            >
              <button
                onClick={() => handleLanguageChange("tr")}
                style={{ 
                  padding: "6px 12px",
                  fontWeight: language === "tr" ? "600" : "400", 
                  cursor: "pointer", 
                  border: "none", 
                  background: language === "tr" ? (theme === 'light' ? "white" : "rgba(255, 255, 255, 0.15)") : "transparent", 
                  color: language === "tr" ? (theme === 'light' ? "#2c3e50" : "#ffffff") : (theme === 'light' ? "#718096" : "rgba(255, 255, 255, 0.6)"),
                  borderRadius: "4px",
                  fontSize: "13px"
                }}
              >
                TR
              </button>
              <button
                onClick={() => handleLanguageChange("en")}
                disabled={isTranslating}
                style={{ 
                  padding: "6px 12px",
                  fontWeight: language === "en" ? "600" : "400", 
                  cursor: isTranslating ? "wait" : "pointer", 
                  border: "none", 
                  background: language === "en" ? (theme === 'light' ? "white" : "rgba(255, 255, 255, 0.15)") : "transparent", 
                  color: language === "en" ? (theme === 'light' ? "#2c3e50" : "#ffffff") : (theme === 'light' ? "#718096" : "rgba(255, 255, 255, 0.6)"),
                  borderRadius: "4px",
                  fontSize: "13px"
                }}
              >
                {isTranslating ? "..." : "EN"}
              </button>
            </div>

            {/* AI Optimize butonu - sadece CV optimize edilmediğinde göster */}
            {!aiData && (
              <button 
                onClick={handleAiGenerate} 
                disabled={aiLoading || !latestJob}
                style={{ 
                  padding: "8px 16px", 
                  background: (!latestJob || aiLoading) ? "#9ca3af" : "#667eea", 
                  color: "white", 
                  border: "none", 
                  borderRadius: "6px", 
                  cursor: (!latestJob || aiLoading) ? "not-allowed" : "pointer",
                  fontWeight: 500,
                  fontSize: "13px"
                }}
                title={!latestJob ? "Önce bir iş ilanı analiz etmelisiniz" : "AI, CV'nizi analiz edilen ilana göre otomatik optimize eder"}
              >
                {aiLoading ? "Yükleniyor..." : "AI Optimize"}
              </button>
            )}

            <button 
              onClick={handleEditProfile} 
              style={{ 
                padding: "8px 16px", 
                background: "transparent", 
                color: theme === 'light' ? "#495057" : "#e2e8f0", 
                border: theme === 'light' ? "1px solid #d1d5db" : "1px solid rgba(255, 255, 255, 0.2)", 
                borderRadius: "6px", 
                cursor: "pointer",
                fontWeight: 500,
                fontSize: "13px"
              }}
              title="Profil bilgilerinizi düzenleyin"
            >
              Profili Düzenle
            </button>

            <button 
              onClick={handleExportPDF} 
              disabled={pdfLoading} 
              style={{ 
                padding: "8px 16px", 
                background: pdfLoading ? "#9ca3af" : "#10b981", 
                color: "white", 
                border: "none", 
                borderRadius: "6px", 
                cursor: pdfLoading ? "not-allowed" : "pointer",
                fontWeight: 500,
                fontSize: "13px"
              }}
              title="CV'nizi PDF formatında indirin"
            >
              {pdfLoading ? "İndiriliyor..." : "PDF İndir"}
            </button>

            {/* Yeni CV Oluştur butonu - sadece CV oluşturulduktan sonra göster */}
            {aiData && (
              <button 
                onClick={() => {
                  setAiData(null);
                  setTranslatedUser(null);
                  setLanguage("tr");
                }} 
                style={{ 
                  padding: "8px 16px", 
                  background: "transparent", 
                  color: theme === 'light' ? "#667eea" : "#a8b5ff", 
                  border: `1px solid ${theme === 'light' ? "#667eea" : "#a8b5ff"}`, 
                  borderRadius: "6px", 
                  cursor: "pointer",
                  fontWeight: 500,
                  fontSize: "13px"
                }}
                title="Yeni bir CV oluşturmak için baştan başlayın"
              >
                🔄 Yeni CV Oluştur
              </button>
            )}
          </div>
        </div>
      </div>

      {/* İLAN DURUMU BANNER */}
      {!loadingJob && latestJob && (
        <div style={{
          background: theme === 'light' ? "#f0fdf4" : "#1a3a2e",
          borderBottom: theme === 'light' ? "1px solid #bbf7d0" : "1px solid rgba(16, 185, 129, 0.3)",
          padding: "12px 30px"
        }}>
          <div style={{
            maxWidth: "1200px",
            margin: "0 auto",
            display: "flex",
            alignItems: "center",
            gap: "12px"
          }}>
            <span style={{ 
              fontSize: "12px", 
              background: "#10b981", 
              color: "white", 
              padding: "4px 10px", 
              borderRadius: "4px",
              fontWeight: 500
            }}>
              ✓ İlan Analiz Edildi
            </span>
            <span style={{ fontSize: "13px", color: theme === 'light' ? "#166534" : "#86efac", fontWeight: "500" }}>
              {latestJob.position}
              {latestJob.company && ` • ${latestJob.company}`}
            </span>
            <button 
              onClick={handleRefreshJob}
              style={{ 
                padding: "4px 8px", 
                fontSize: "12px", 
                background: "transparent", 
                border: "none", 
                color: theme === 'light' ? "#166534" : "#86efac", 
                cursor: "pointer",
                marginLeft: "auto"
              }}
              title="İlanı yenile"
            >
              🔄
            </button>
          </div>
        </div>
      )}

      {/* ANA İÇERİK */}
      <div style={{ 
        maxWidth: "1200px", 
        margin: "0 auto", 
        padding: "30px"
      }}>
        {/* ADIM ADIM REHBER - CV optimize edilmediğinde göster */}
        {!aiData && (
          <div style={{
            background: theme === 'light' ? "white" : "#2d3748",
            borderRadius: "12px",
            padding: "24px",
            marginBottom: "24px",
            border: theme === 'light' ? "1px solid #e0e0e0" : "1px solid rgba(255, 255, 255, 0.1)"
          }}>
            <h3 style={{ 
              fontSize: "18px", 
              fontWeight: 600, 
              color: theme === 'light' ? "#2c3e50" : "#ffffff", 
              marginBottom: "16px",
              display: "flex",
              alignItems: "center",
              gap: "8px"
            }}>
              <span>📋</span>
              <span>CV Oluşturma Rehberi</span>
            </h3>
            <div style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))",
              gap: "16px"
            }}>
              <div style={{
                display: "flex",
                gap: "12px",
                padding: "12px",
                background: theme === 'light' ? "#f8f9fa" : "rgba(255, 255, 255, 0.05)",
                borderRadius: "8px"
              }}>
                <div style={{
                  width: "32px",
                  height: "32px",
                  borderRadius: "50%",
                  background: "#667eea",
                  color: "white",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontWeight: 600,
                  fontSize: "14px",
                  flexShrink: 0
                }}>
                  1
                </div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: "14px", color: theme === 'light' ? "#2c3e50" : "#ffffff", marginBottom: "4px" }}>
                    Profil Bilgilerinizi Doldurun
                  </div>
                  <div style={{ fontSize: "12px", color: theme === 'light' ? "#718096" : "rgba(255, 255, 255, 0.7)" }}>
                    Kişisel bilgiler, eğitim ve deneyimlerinizi ekleyin
                  </div>
                </div>
              </div>
              <div style={{
                display: "flex",
                gap: "12px",
                padding: "12px",
                background: theme === 'light' ? "#f8f9fa" : "rgba(255, 255, 255, 0.05)",
                borderRadius: "8px"
              }}>
                <div style={{
                  width: "32px",
                  height: "32px",
                  borderRadius: "50%",
                  background: latestJob ? "#10b981" : "#9ca3af",
                  color: "white",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontWeight: 600,
                  fontSize: "14px",
                  flexShrink: 0
                }}>
                  2
                </div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: "14px", color: theme === 'light' ? "#2c3e50" : "#ffffff", marginBottom: "4px" }}>
                    İş İlanı Analiz Edin
                  </div>
                  <div style={{ fontSize: "12px", color: theme === 'light' ? "#718096" : "rgba(255, 255, 255, 0.7)" }}>
                    CV'nizi optimize etmek için bir iş ilanı analiz edin
                  </div>
                </div>
              </div>
              <div style={{
                display: "flex",
                gap: "12px",
                padding: "12px",
                background: theme === 'light' ? "#f8f9fa" : "rgba(255, 255, 255, 0.05)",
                borderRadius: "8px"
              }}>
                <div style={{
                  width: "32px",
                  height: "32px",
                  borderRadius: "50%",
                  background: aiData ? "#10b981" : "#9ca3af",
                  color: "white",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontWeight: 600,
                  fontSize: "14px",
                  flexShrink: 0
                }}>
                  3
                </div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: "14px", color: theme === 'light' ? "#2c3e50" : "#ffffff", marginBottom: "4px" }}>
                    AI ile Optimize Edin
                  </div>
                  <div style={{ fontSize: "12px", color: theme === 'light' ? "#718096" : "rgba(255, 255, 255, 0.7)" }}>
                    AI, CV'nizi analiz edilen ilana göre optimize eder
                  </div>
                </div>
              </div>
              <div style={{
                display: "flex",
                gap: "12px",
                padding: "12px",
                background: theme === 'light' ? "#f8f9fa" : "rgba(255, 255, 255, 0.05)",
                borderRadius: "8px"
              }}>
                <div style={{
                  width: "32px",
                  height: "32px",
                  borderRadius: "50%",
                  background: "#9ca3af",
                  color: "white",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontWeight: 600,
                  fontSize: "14px",
                  flexShrink: 0
                }}>
                  4
                </div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: "14px", color: theme === 'light' ? "#2c3e50" : "#ffffff", marginBottom: "4px" }}>
                    PDF Olarak İndirin
                  </div>
                  <div style={{ fontSize: "12px", color: theme === 'light' ? "#718096" : "rgba(255, 255, 255, 0.7)" }}>
                    Hazır CV'nizi PDF formatında indirin
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* --- UYARI KUTUSU (EĞER HİÇ İLAN YOKSA) --- */}
        {!loadingJob && !latestJob && (
          <div style={{
            background: "#fffbeb",
            border: "1px solid #fbbf24",
            borderRadius: "8px",
            padding: "16px 20px",
            marginBottom: "24px",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: "16px"
          }}>
            <div style={{ display: "flex", alignItems: "center", gap: "12px", flex: 1 }}>
              <span style={{ fontSize: "20px" }}>⚠️</span>
              <div>
                <div style={{ color: "#92400e", fontSize: "14px", fontWeight: 500 }}>
                  CV'nizi optimize etmek için önce bir iş ilanı analiz edin
                </div>
                <div style={{ color: "#b45309", fontSize: "12px", marginTop: "4px" }}>
                  "İlan Analiz Et" butonuna tıklayarak başlayın
                </div>
              </div>
            </div>
            <button 
              onClick={() => navigate("/job-analysis")}
              style={{
                padding: "8px 16px",
                background: "#f59e0b",
                color: "white",
                border: "none",
                borderRadius: "6px",
                cursor: "pointer",
                fontWeight: 500,
                fontSize: "13px",
                whiteSpace: "nowrap"
              }}
            >
              İlan Analiz Et →
            </button>
          </div>
        )}

        {/* --- CV ÖNİZLEME ALANI --- */}
        <div style={{ marginTop: "24px" }}>
          <div style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "12px"
          }}>
            <h3 style={{
              fontSize: "16px",
              fontWeight: 600,
              color: theme === 'light' ? "#2c3e50" : "#ffffff",
              margin: 0
            }}>
              CV Önizleme
            </h3>
            <div style={{
              fontSize: "12px",
              color: theme === 'light' ? "#718096" : "rgba(255, 255, 255, 0.7)",
              display: "flex",
              alignItems: "center",
              gap: "8px"
            }}>
              <span>💡</span>
              <span>CV'nizi burada görüntüleyebilir ve PDF olarak indirebilirsiniz</span>
            </div>
          </div>
          <div style={{ display: "flex", justifyContent: "center" }}>
            <div style={{ 
              width: "850px", 
              background: "white", 
              borderRadius: "10px", 
              boxShadow: "0 2px 12px rgba(0,0,0,0.12)" 
            }}>
              {isTranslating ? (
                <div style={{ padding: "50px", textAlign: "center", color: "#666" }}>
                  <h3>CV İngilizceye Çevriliyor...</h3>
                  <p>Lütfen bekleyin, yapay zeka verilerinizi işliyor...</p>
                </div>
              ) : (
                <CVPreview
                  user={activeUser}
                  aiData={language === "en" ? null : aiData}
                  language={language}
                />
              )}
            </div>
          </div>
        </div>

        {/* --- ALT BİLGİ: KULLANILAN İLAN --- */}
        {aiData?.jobUsed && (
          <div style={{ 
            marginTop: "24px", 
            padding: "12px 16px", 
            background: "#f0f9ff", 
            borderRadius: "6px", 
            border: "1px solid #bae6fd" 
          }}>
            <div style={{ fontSize: "12px", color: "#0369a1", fontWeight: 500 }}>
              Bu CV optimize edildi: <strong>{aiData.jobUsed.position}</strong>
              {aiData.jobUsed.company && ` • ${aiData.jobUsed.company}`}
            </div>
          </div>
        )}

      </div>
    </div>
  );
};

export default CVBuilder;