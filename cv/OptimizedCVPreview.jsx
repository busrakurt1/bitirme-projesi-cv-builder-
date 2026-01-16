import './CVPreview.css';

const OptimizedCVPreview = ({ user, optimizationTips, originalScore, optimizedScore }) => {
  if (!user) {
    return <div>Optimize edilmiş CV yükleniyor...</div>;
  }

  return (
    <div id="optimized-cv-preview" className="cv-container modern-template optimized-cv">
      {/* OPTIMIZASYON BAŞLIĞI */}
      <div style={{
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        color: 'white',
        padding: '15px',
        borderRadius: '8px',
        marginBottom: '20px',
        textAlign: 'center'
      }}>
        <h3 style={{ margin: 0 }}>🎯 İlana Özel Optimize Edilmiş CV</h3>
        <p style={{ margin: '5px 0 0 0', opacity: 0.9 }}>
          Uyum Puanı: <strong>%{originalScore} → %{optimizedScore}</strong>
        </p>
      </div>

      {/* BAŞLIK BÖLÜMÜ */}
      <header className="cv-header">
        <div className="personal-info">
          <h1 className="full-name">{user.fullName}</h1>
          <p className="title">{user.summary || 'Yazılım Geliştirici'}</p>
        </div>
        <div className="contact-info">
          <p>📧 {user.email}</p>
          <p>📱 {user.phone || 'Belirtilmemiş'}</p>
          <p>📍 {user.location || 'Belirtilmemiş'}</p>
        </div>
      </header>

      {/* OPTIMIZASYON DURUMU */}
      {optimizationTips && optimizationTips.length > 0 && (
        <section className="cv-section" style={{ background: '#f0f8ff', padding: '15px', borderRadius: '8px' }}>
          <h2>🚀 Optimizasyon Özeti</h2>
          <ul style={{ margin: 0, paddingLeft: '20px' }}>
            {optimizationTips.map((tip, index) => (
              <li key={index} style={{ marginBottom: '8px' }}>{tip}</li>
            ))}
          </ul>
        </section>
      )}

      {/* PROFESYONEL ÖZET */}
      {user.summary && (
        <section className="cv-section">
          <h2>👤 Profesyonel Özet</h2>
          <p>{user.summary}</p>
        </section>
      )}

      {/* TEKNİK BECERİLER */}
      {user.technicalSkills && (
        <section className="cv-section">
          <h2>💻 Teknik Beceriler</h2>
          <div className="skills-grid">
            <div>
              <p>{user.technicalSkills}</p>
            </div>
          </div>
        </section>
      )}

      {/* İŞ DENEYİMİ */}
      {user.workExperience && (
        <section className="cv-section">
          <h2>💼 İş Deneyimi</h2>
          <div className="experience-item">
            <p>{user.workExperience}</p>
          </div>
        </section>
      )}

      {/* PROJELER */}
      {user.projects && (
        <section className="cv-section">
          <h2>🚀 Projeler</h2>
          <div className="project-item">
            <p>{user.projects}</p>
          </div>
        </section>
      )}

      {/* EĞİTİM */}
      {(user.educationLevel || user.university) && (
        <section className="cv-section">
          <h2>🎓 Eğitim</h2>
          <div className="education-item">
            <h3>{user.university || 'Üniversite'}</h3>
            <p>{user.department || 'Bölüm'}</p>
            <p>{user.educationLevel} - {user.graduationYear || 'Mezuniyet Yılı'}</p>
          </div>
        </section>
      )}

      {/* BAŞARILAR */}
      {user.achievements && (
        <section className="cv-section">
          <h2>🏆 Öne Çıkanlar</h2>
          <p>{user.achievements}</p>
        </section>
      )}
    </div>
  );
};

export default OptimizedCVPreview;