import React, { useState } from "react";
import "./CVPreview.css";

const CVPreview = ({ user, aiData, language = "tr" }) => {
  if (!user) {
    return <div className="loading-msg">Kullanıcı verisi bekleniyor...</div>;
  }

  // --- 1. DİL SÖZLÜĞÜ (SABİT BAŞLIKLAR İÇİN) ---
  const LABELS = {
    tr: {
      summary: "ÖZET",
      experience: "DENEYİM",
      education: "EĞİTİM",
      projects: "PROJELER",
      skills: "YETENEKLER",
      languages: "DİLLER",
      certificates: "SERTİFİKALAR",
      present: "Devam Ediyor",
      changeText: "Metni Değiştir"
    },
    en: {
      summary: "SUMMARY",
      experience: "EXPERIENCE",
      education: "EDUCATION",
      projects: "PROJECTS",
      skills: "SKILLS",
      languages: "LANGUAGES",
      certificates: "CERTIFICATES",
      present: "Present",
      changeText: "Change Text"
    }
  };

  const t = LABELS[language];

  // --- 2. YARDIMCI FONKSİYONLAR ---
  
  const normalizeUrl = (url) => {
    if (!url) return "";
    return url.startsWith("http://") || url.startsWith("https://") ? url : `https://${url}`;
  };

  const cleanText = (text) => {
    if (!text) return "";
    
    // JSON ve süslü parantez kalıntılarını temizle
    let cleaned = text.replace(/\{[^}]*\}/g, ""); // Süslü parantez içeriğini kaldır
    cleaned = cleaned.replace(/\[[^\]]*\]/g, ""); // Köşeli parantez içeriğini kaldır
    cleaned = cleaned.replace(/\"([^\"]*)\"/g, "$1"); // Tırnakları kaldır ama içeriği koru
    
    // Özel karakterleri temizle
    cleaned = cleaned.replace(/[@#*_`>]/g, "").replace(/\s{2,}/g, " ").trim();
    
    // Proje açıklamalarından tekrarlayan ifadeleri kaldır
    cleaned = cleaned.replace(/^(Proje\s+Adı|Proje|Project\s+Name|Project):\s*/i, "");
    cleaned = cleaned.replace(/\b(Proje\s+Adı|Proje|Project\s+Name|Project):\s*/gi, "");
    // "Bu projede", "Bu proje" gibi ifadeleri kaldır
    cleaned = cleaned.replace(/^(Bu\s+projede?|In\s+this\s+project)\s*,?\s*/i, "");
    cleaned = cleaned.replace(/\b(Bu\s+projede?|In\s+this\s+project)\s*,?\s*/gi, "");
    
    // Deneyim açıklamalarından pozisyon adı tekrarlarını kaldır
    // Örneğin: "Backend Geliştiricisi - Backend geliştirme" -> "Backend Geliştiricisi - geliştirme"
    // Veya: "Backend - Backend geliştirme" -> "Backend - geliştirme"
    // Pozisyon adının başta tekrarını kaldır
    cleaned = cleaned.replace(/^([A-Za-z\s]+?)\s*[-–—]\s*\1\s+/i, "$1 - ");
    
    // Başta ve sonda gereksiz karakterleri temizle
    cleaned = cleaned.replace(/^[,\s:;\\-]+/, "");
    cleaned = cleaned.replace(/[,\s:;\\-]+$/, "");
    
    return cleaned.trim();
  };

  const formatSkillName = (skill) => {
    if (!skill) return "";
    let formatted = String(skill).trim();
    
    // Süslü parantez ve JSON kalıntılarını temizle
    formatted = formatted.replace(/\{[^}]*\}/g, "");
    formatted = formatted.replace(/\[[^\]]*\]/g, "");
    
    // Yaygın yazım hatalarını düzelt
    const corrections = {
      "rubby": "Ruby",
      "RUBBY": "Ruby",
      "ruby": "Ruby",
      "java": "Java",
      "python": "Python",
      "javascript": "JavaScript",
      "typescript": "TypeScript",
      "react": "React",
      "angular": "Angular",
      "ANGULAR": "Angular",
      "vue": "Vue",
      "nodejs": "Node.js",
      "node.js": "Node.js",
      "oop": "OOP",
      "agile": "Agile",
      "devops": "DevOps",
      "ai": "AI",
      "sql": "SQL",
      "nosql": "NoSQL",
      "git": "Git",
      "docker": "Docker",
      "kubernetes": "Kubernetes",
      "aws": "AWS",
      "azure": "Azure",
      "gcp": "GCP",
      "c": "C", // Tek harfli C'yi koru
      "c#": "C#",
      "c++": "C++",
      "html": "HTML",
      "css": "CSS",
      "json": "JSON",
      "xml": "XML",
      "rest": "REST",
      "api": "API",
      "http": "HTTP",
      "https": "HTTPS",
      "mysql": "MySQL",
      "postgresql": "PostgreSQL",
      "mongodb": "MongoDB",
      "redis": "Redis",
      "spring": "Spring",
      "django": "Django",
      "flask": "Flask",
      "express": "Express",
      "scrum": "Scrum",
      "kanban": "Kanban",
      "jira": "Jira",
      "jenkins": "Jenkins",
      "terraform": "Terraform",
      "ansible": "Ansible",
      "ci/cd": "CI/CD",
      "microservices": "Microservices",
      "graphql": "GraphQL",
      "websocket": "WebSocket"
    };
    
    // Önce yazım hatalarını düzelt
    const lowerSkill = formatted.toLowerCase();
    if (corrections[lowerSkill]) {
      formatted = corrections[lowerSkill];
    } else {
      // Tek harfli yetenekler için özel işlem
      if (formatted.length === 1) {
        formatted = formatted.toUpperCase();
      } else if (formatted === formatted.toLowerCase() && formatted.length > 0) {
        // Teknik terim değilse ve tamamen küçük harfliyse, Title Case uygula
        formatted = formatted.charAt(0).toUpperCase() + formatted.slice(1).toLowerCase();
      } else if (formatted === formatted.toUpperCase() && formatted.length > 1) {
        // Tüm büyük harfliyse, sadece ilk harfi büyük yap
        formatted = formatted.charAt(0) + formatted.slice(1).toLowerCase();
      }
    }
    
    return formatted;
  };

  const getLanguageLevelText = (level) => {
    if (!level) return "";
    const normalized = level.toUpperCase();
    const mapTR = { "BEGINNER": "Başlangıç", "INTERMEDIATE": "Orta", "ADVANCED": "İleri", "NATIVE": "Ana Dil" };
    const mapEN = { "BEGINNER": "Beginner", "INTERMEDIATE": "Intermediate", "ADVANCED": "Advanced", "NATIVE": "Native" };
    const map = language === "tr" ? mapTR : mapEN;
    return map[normalized] || level; 
  };

  const formatDate = (dateString, isOngoing) => {
    if (isOngoing) return t.present;
    if (!dateString) return "";
    try {
      const date = new Date(dateString);
      if (isNaN(date)) return dateString;
      return date.toLocaleDateString(language === "tr" ? "tr-TR" : "en-US", {
        year: "numeric",
        month: language === "tr" ? "long" : "short"
      });
    } catch {
      return dateString;
    }
  };

  // --- 3. VERİ HAZIRLIĞI ---

  const baseSummary = aiData?.summary || user.summary || user.aboutMe || "";
  const summaries = aiData?.summaries && aiData.summaries.length > 0
      ? aiData.summaries
      : baseSummary ? [baseSummary] : [];
  const [summaryIndex, setSummaryIndex] = useState(0);

  const handleChangeSummary = () => {
    if (summaries.length <= 1) return;
    setSummaryIndex((prev) => (prev + 1) % summaries.length);
  };
  const currentSummary = summaries[summaryIndex] || "";

  const userData = {
    fullName: user.fullName || user.adSoyad || "İSİM GİRİLMEDİ",
    title: user.title || user.preferredJobRoles || "",
    email: user.email || "",
    phone: user.phone || user.phoneNumber || "",
    location: user.location || user.address || "",
    summary: currentSummary,
    skills: aiData?.skills || user.skills || user.technicalSkills || [],
    languages: aiData?.languages || user.languages || [],
    certificates: aiData?.certificates || user.certificates || [],
    linkedinUrl: user.linkedinUrl || user.profile?.linkedinUrl || user.linkedin || "",
    githubUrl: user.githubUrl || user.profile?.githubUrl || user.github || "",
    websiteUrl: user.websiteUrl || user.profile?.websiteUrl || user.website || "",
  };

  const experienceList = aiData?.optimizedExperiences?.length > 0 ? aiData.optimizedExperiences : user.experiences || [];
  
  const projectList = aiData?.optimizedProjects?.length > 0 
    ? aiData.optimizedProjects 
    : (aiData?.optimizedUserProjects?.length > 0 ? aiData.optimizedUserProjects : user.projects || []);

  let educationList = aiData?.optimizedEducation?.length > 0 
    ? aiData.optimizedEducation 
    : (user.education || []);

  if (educationList.length === 0 && (user.educationSchool || user.university)) {
    educationList.push({
      university: user.educationSchool || user.university,
      degree: user.educationDegree || user.educationLevel,
      field: user.educationDepartment || user.department,
      startYear: user.educationStartYear,
      graduationYear: user.educationEndYear,
    });
  }

  // --- 4. RENDER ---
  return (
    <div id="cv-preview" className="cv-container">
      
      {/* HEADER */}
      <header className="cv-header">
        <h1 className="full-name">{userData.fullName}</h1>
        {userData.title && <div className="title-role">{userData.title}</div>}
        
        <div className="contact-info">
          {userData.email} 
          {userData.phone && ` | ${userData.phone}`}
          {userData.location && ` | ${userData.location}`}
        </div>

        <div className="social-links">
          {userData.linkedinUrl && (
            <div className="social-item"><a href={normalizeUrl(userData.linkedinUrl)} target="_blank" rel="noreferrer">LinkedIn</a></div>
          )}
          {userData.githubUrl && (
            <div className="social-item"><a href={normalizeUrl(userData.githubUrl)} target="_blank" rel="noreferrer">GitHub</a></div>
          )}
          {userData.websiteUrl && (
            <div className="social-item"><a href={normalizeUrl(userData.websiteUrl)} target="_blank" rel="noreferrer">Portfolio</a></div>
          )}
        </div>
      </header>

      {/* SUMMARY */}
      {userData.summary && (
        <section className="cv-section">
          <h2 className="section-title">{t.summary}</h2>
          {summaries.length > 1 && (
            <div className="summary-title-wrapper pdf-exclude" style={{ justifyContent: 'flex-end', display: 'flex' }}>
              <button type="button" className="summary-change-button" onClick={handleChangeSummary}>
                {t.changeText} ({summaryIndex + 1}/{summaries.length})
              </button>
            </div>
          )}
          <p className="summary-text">{cleanText(userData.summary)}</p>
        </section>
      )}

      {/* EXPERIENCE */}
      {experienceList.length > 0 && (
        <section className="cv-section">
          <h2 className="section-title">{t.experience}</h2>
          <div className="section-content">
            {experienceList.map((exp, index) => {
              const pos = exp.position || exp.title;
              const comp = exp.company || exp.companyName || exp.subtitle;
              const start = exp.startDate || (exp.date ? exp.date.split(' - ')[0] : "");
              const end = exp.endDate || (exp.date ? exp.date.split(' - ')[1] : "");
              const dateDisplay = (exp.date && exp.date.length > 10) ? exp.date : `${formatDate(start)} - ${formatDate(end, exp.isOngoing)}`;

              // Deneyim açıklamasını temizle - pozisyon adını kaldır
              const cleanExperienceDescription = (desc, position) => {
                if (!desc) return "";
                let cleaned = cleanText(desc);
                
                // Süslü parantez ile başlayan metinleri temizle
                cleaned = cleaned.replace(/^\{\s*/, "");
                cleaned = cleaned.replace(/^\{\s*\./, "");
                
                // Pozisyon adını açıklamadan kaldır
                if (position) {
                  const escapedPos = position.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                  const posLower = position.toLowerCase();
                  
                  // "Backend - Backend geliştirme" gibi durumları düzelt (tire ile tekrar)
                  const posPattern1 = new RegExp(`^${escapedPos}\\s*[-–—]\\s*${escapedPos}\\s+`, "i");
                  cleaned = cleaned.replace(posPattern1, "");
                  
                  // Pozisyon adını başta kaldır (tire olmadan)
                  const posPattern2 = new RegExp(`^${escapedPos}\\s+`, "i");
                  cleaned = cleaned.replace(posPattern2, "");
                  
                  // Pozisyon adının kelimelerini tek tek kontrol et ve tekrar edenleri kaldır
                  const words = posLower.split(/\s+/);
                  for (const word of words) {
                    if (word.length > 3) {
                      // "Backend geliştirme" gibi durumlarda "Backend" kelimesini kaldır
                      const wordPattern = new RegExp(`\\b${word}\\s+[-–—]?\\s*${word}\\b`, "gi");
                      cleaned = cleaned.replace(wordPattern, word);
                      // Başta tekrar eden kelimeyi kaldır
                      const wordPattern2 = new RegExp(`^${word}\\s+[-–—]?\\s*`, "i");
                      cleaned = cleaned.replace(wordPattern2, "");
                    }
                  }
                }
                
                // Eğer sadece süslü parantez veya çok kısaysa, varsayılan metin ekle
                if (cleaned.trim().length < 10 || cleaned.trim() === "{" || cleaned.trim().startsWith("{")) {
                  cleaned = "Bu pozisyonda çalıştım ve projelerin başarıyla tamamlanmasına katkı sağladım.";
                }
                
                return cleaned.trim();
              };

              return (
                <div key={index} className="experience-item">
                  <div className="row-space-between">
                    <div className="job-title">{pos}</div>
                    <div className="dates">{dateDisplay}</div>
                  </div>
                  <div className="row-space-between">
                    <div className="company-info">{comp}</div>
                    {exp.location && <div className="location">{exp.location}</div>}
                  </div>
                  
                  {Array.isArray(exp.description) ? (
                    <ul className="responsibilities">
                      {exp.description.map((item, i) => <li key={i}>{cleanExperienceDescription(item, pos)}</li>)}
                    </ul>
                  ) : (
                    <p className="description">{cleanExperienceDescription(exp.description, pos)}</p>
                  )}
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* EDUCATION */}
      {educationList.length > 0 && (
        <section className="cv-section">
          <h2 className="section-title">{t.education}</h2>
          <div className="section-content">
            {educationList.map((edu, index) => {
              // Eğitim bilgilerini temizle
              const schoolName = cleanText(edu.schoolName || edu.university || edu.educationSchool || "Belirtilmemiş");
              const degree = cleanText(edu.degree || "");
              const department = cleanText(edu.field || edu.department || "");
              
              return (
                <div key={index} className="education-item">
                  <div className="row-space-between">
                    <div className="university">{schoolName}</div>
                    <div className="education-dates">
                      {formatDate(edu.startYear || edu.startDate)} - {formatDate(edu.graduationYear || edu.endDate, edu.isOngoing)}
                    </div>
                  </div>
                  <div className="degree">
                    {degree && !degree.startsWith('{') ? degree : ""}
                    {department && !department.startsWith('{') ? (degree && !degree.startsWith('{') ? `, ${department}` : department) : ""}
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* PROJECTS (DÜZELTİLDİ: Sınıf ismi 'responsibilities' yapıldı) */}
      {projectList.length > 0 && (
        <section className="cv-section">
          <h2 className="section-title">{t.projects}</h2>
          <div className="section-content">
            {projectList.map((proj, index) => {
              const name = proj.name || proj.title || proj.projectName;
              const dateDisplay = (proj.date && proj.date.length > 10)
                  ? proj.date
                  : `${formatDate(proj.startDate)} - ${formatDate(proj.endDate, proj.isOngoing)}`;

              // Proje açıklamasını temizle - proje adını kaldır
              const cleanProjectDescription = (desc, projectName) => {
                if (!desc) return "";
                let cleaned = cleanText(desc);
                
                // Süslü parantez ile başlayan metinleri temizle
                cleaned = cleaned.replace(/^\{\s*/, "");
                cleaned = cleaned.replace(/^\{\s*\./, "");
                
                // Proje adını açıklamadan kaldır
                if (projectName) {
                  // Proje adını başta kaldır (örn: "DEVPATH AI Bu projede" -> "Bu projede")
                  const escapedName = projectName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                  const namePattern = new RegExp(`^${escapedName}\\s+`, "i");
                  cleaned = cleaned.replace(namePattern, "");
                  // "Bu projede", "Bu proje" gibi ifadeleri de kaldır
                  cleaned = cleaned.replace(/^(Bu\s+projede?|In\s+this\s+project)\s*,?\s*/i, "");
                  // Proje adını cümle içinde de kaldır (tekrar eden durumlar için)
                  const namePattern2 = new RegExp(`\\b${escapedName}\\s+`, "gi");
                  cleaned = cleaned.replace(namePattern2, "");
                }
                
                // Eğer sadece süslü parantez veya boşsa, varsayılan metin ekle
                if (cleaned.trim().length < 10 || cleaned.trim() === "{" || cleaned.trim().startsWith("{")) {
                  cleaned = "Bu projede geliştirme yaptım ve projenin başarıyla tamamlanmasına katkı sağladım.";
                }
                
                return cleaned.trim();
              };

              return (
                <div key={index} className="project-item">
                  <div className="row-space-between">
                    <div className="project-title">{name}</div>
                    <div className="project-dates">{dateDisplay}</div>
                  </div>
                  {Array.isArray(proj.description) ? (
                    // 🔥 BURASI DÜZELTİLDİ: project-description-list YERİNE responsibilities
                    <ul className="responsibilities">
                      {proj.description.map((d, i) => <li key={i}>{cleanProjectDescription(d, name)}</li>)}
                    </ul>
                  ) : (
                    <p className="description">{cleanProjectDescription(proj.description, name)}</p>
                  )}
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* SKILLS */}
      {userData.skills.length > 0 && (
        <section className="cv-section">
          <h2 className="section-title">{t.skills}</h2>
          <div className="skills-grid">
            {userData.skills.map((skill, index) => {
              const skillName = typeof skill === "object" ? (skill.name || skill.skillName) : skill;
              return (
                <div key={index} className="skill-item">
                  {formatSkillName(skillName)}
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* LANGUAGES */}
      {userData.languages.length > 0 && (
        <section className="cv-section">
          <h2 className="section-title">{t.languages}</h2>
          <div className="languages-grid">
            {userData.languages.map((lang, index) => (
               <div key={index} className="language-item">
                 <span style={{ fontWeight: "600" }}>{lang.language}</span>
                 {lang.level && <span className="language-level"> - {getLanguageLevelText(lang.level)}</span>}
               </div>
            ))}
          </div>
        </section>
      )}

      {/* CERTIFICATES */}
      {userData.certificates.length > 0 && (
        <section className="cv-section certificates-section">
          <h2 className="section-title">{t.certificates}</h2>
          <div className="section-content">
            {userData.certificates.map((cert, index) => {
               const certDate = cert.date ? cert.date : formatDate(cert.issueDate);
               return (
                <div key={index} className="certificate-item">
                  <div className="row-space-between">
                    <div className="job-title" style={{ fontSize: "11pt", fontWeight: "500" }}>{cert.name}</div>
                    <div className="dates">{certDate}</div>
                  </div>
                  {cert.issuer && <div className="certificate-issuer">{cert.issuer}</div>}
                </div>
               );
            })}
          </div>
        </section>
      )}

    </div>
  );
};

export default CVPreview;