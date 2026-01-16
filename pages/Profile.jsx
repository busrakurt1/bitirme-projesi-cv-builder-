// src/pages/ProfilePage.jsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { profileAPI } from '../services/api';
import { useTheme } from '../contexts/ThemeContext';

// --- STİLLER ---
const styles = {
  container: { maxWidth: '900px', margin: '0 auto', padding: '24px', fontFamily: 'Arial, sans-serif' },
  section: { marginBottom: '30px', padding: '20px', background: '#f8f9fa', borderRadius: '8px', border: '1px solid #e9ecef' },
  header: { borderBottom: '2px solid #1890ff', paddingBottom: '10px', marginBottom: '30px' },
  subHeader: { color: '#1890ff', marginBottom: '15px', marginTop: 0, display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  label: { display: 'block', marginBottom: '5px', fontWeight: '500', fontSize: '14px', color: '#495057' },
  input: { width: '100%', padding: '10px', border: '1px solid #ced4da', borderRadius: '4px', boxSizing: 'border-box', fontSize: '14px' },
  button: { cursor: 'pointer', borderRadius: '4px', border: 'none', padding: '8px 16px', color: 'white', fontSize: '14px', transition: '0.2s' },
  grid2: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '15px' },
  grid3: { display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '15px', marginBottom: '15px' },
  card: { background: 'var(--card-bg, white)', padding: '15px', borderRadius: '6px', border: '1px solid #dee2e6', marginBottom: '15px' }
};

// --- BAŞLANGIÇ DEĞERLERİ (BACKEND DTO İLE UYUMLU) ---
const INITIAL_ITEMS = {
  educations: { schoolName: '', department: '', degree: 'Lisans', startYear: '', graduationYear: '', gpa: '' },
  experiences: { position: '', company: '', city: '', employmentType: 'Full-time', startDate: '', endDate: '', technologies: '', description: '' },
  projects: { projectName: '', startDate: '', endDate: '', isOngoing: false, technologies: '', url: '', description: '' },
  skills: { skillName: '', level: 'INTERMEDIATE', years: 0 },
  languages: { language: '', level: 'Intermediate' },
  certificates: { name: '', issuer: '', date: '', url: '' },
};

const emptyProfile = {
  fullName: '', email: '', phone: '', location: '',
  linkedinUrl: '', githubUrl: '', websiteUrl: '', title: '',
  totalExperienceYear: 0, summary: '',
  educations: [], skills: [], experiences: [], 
  languages: [], certificates: [], projects: [],
};

// --- YARDIMCI BİLEŞENLER ---
const InputField = ({ label, theme, ...props }) => (
  <div style={{ marginBottom: '10px' }}>
    {label && <label style={{...styles.label, color: theme === 'light' ? '#495057' : '#cbd5e0'}}>{label}</label>}
    <input style={{
      ...styles.input, 
      background: props.disabled 
        ? (theme === 'light' ? '#e9ecef' : '#4a5568')
        : (theme === 'light' ? '#ffffff' : '#2d3748'),
      color: theme === 'light' ? '#2c3e50' : '#e2e8f0',
      border: theme === 'light' ? '1px solid #ced4da' : '1px solid #4a5568'
    }} {...props} />
  </div>
);

const TextAreaField = ({ label, theme, ...props }) => (
  <div style={{ marginBottom: '10px' }}>
    {label && <label style={{...styles.label, color: theme === 'light' ? '#495057' : '#cbd5e0'}}>{label}</label>}
    <textarea style={{ 
      ...styles.input, 
      minHeight: '80px', 
      fontFamily: 'inherit', 
      resize: 'vertical',
      background: theme === 'light' ? '#ffffff' : '#2d3748',
      color: theme === 'light' ? '#2c3e50' : '#e2e8f0',
      border: theme === 'light' ? '1px solid #ced4da' : '1px solid #4a5568'
    }} {...props} />
  </div>
);

const SelectField = ({ label, options, theme, ...props }) => (
  <div style={{ marginBottom: '10px' }}>
    {label && <label style={{...styles.label, color: theme === 'light' ? '#495057' : '#cbd5e0'}}>{label}</label>}
    <select style={{
      ...styles.input,
      background: theme === 'light' ? '#ffffff' : '#2d3748',
      color: theme === 'light' ? '#2c3e50' : '#e2e8f0',
      border: theme === 'light' ? '1px solid #ced4da' : '1px solid #4a5568'
    }} {...props}>
      {options.map(opt => (
        <option key={opt.value} value={opt.value} style={{
          background: theme === 'light' ? '#ffffff' : '#2d3748',
          color: theme === 'light' ? '#2c3e50' : '#e2e8f0'
        }}>{opt.label}</option>
      ))}
    </select>
  </div>
);

function ProfilePage() {
  const navigate = useNavigate();
  const { theme } = useTheme();
  const [form, setForm] = useState(emptyProfile);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // ---------------- PROFILE GET ----------------
  useEffect(() => {
    const fetchProfile = async () => {
      try {
        setLoading(true);
        const res = await profileAPI.getMe();
        const data = res.data || {};

        setForm((prev) => ({
          ...prev,
          ...data,
          educations: data.educations || [], 
          skills: data.skills || [],
          experiences: data.experiences || [],
          languages: data.languages || [],
          certificates: data.certificates || [],
          projects: data.projects || [],
        }));
      } catch (err) {
        console.error('Profil yükleme hatası:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, []);

  // ---------------- HANDLERS ----------------
  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({
      ...prev,
      [name]: name === 'totalExperienceYear' ? (Number(value) || 0) : value
    }));
  };

  const addItem = (field) => {
    setForm(prev => ({
      ...prev,
      [field]: [...(prev[field] || []), { ...INITIAL_ITEMS[field] }]
    }));
  };

  const updateItem = (field, index, subField, value) => {
    setForm(prev => {
      const list = [...prev[field]];
      list[index] = { ...list[index], [subField]: value };
      return { ...prev, [field]: list };
    });
  };

  const removeItem = (field, index) => {
    setForm(prev => ({
      ...prev,
      [field]: prev[field].filter((_, i) => i !== index)
    }));
  };

  const toggleItemOngoing = (field, index, dateFieldToClear = 'endDate') => {
    setForm(prev => {
      const list = [...prev[field]];
      const current = list[index];
      const newIsOngoing = !current.isOngoing;
      list[index] = {
        ...current,
        isOngoing: newIsOngoing,
        [dateFieldToClear]: newIsOngoing ? '' : current[dateFieldToClear]
      };
      return { ...prev, [field]: list };
    });
  };

  // ---------------- SAVE ----------------
  const handleSave = async () => {
    try {
      setSaving(true);
      
      const payload = {
        ...form,
        totalExperienceYear: Number(form.totalExperienceYear) || 0,
        skills: form.skills.map(s => ({ ...s, years: Number(s.years) || 0 })),
        // Educations listesi olduğu gibi gider
      };

      console.log("Gönderilen Payload:", payload); // Debug için

      const res = await profileAPI.updateMe(payload);
      
      if (res.data) {
         setForm(prev => ({ ...prev, ...res.data }));
      }
      
      alert('✅ Profil başarıyla kaydedildi!');
    } catch (err) {
      console.error('Kaydetme hatası:', err);
      alert('❌ Kaydetme sırasında hata oluştu.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return (
    <div style={{ 
      textAlign: 'center', 
      padding: '50px',
      background: theme === 'light' ? '#ffffff' : '#1a202c',
      color: theme === 'light' ? '#2c3e50' : '#ffffff',
      minHeight: '100vh'
    }}>
      Profil yükleniyor...
    </div>
  );

  const themeStyles = {
    container: {
      ...styles.container,
      background: theme === 'light' ? '#ffffff' : '#1a202c',
      color: theme === 'light' ? '#2c3e50' : '#e2e8f0',
      minHeight: '100vh'
    },
    section: {
      ...styles.section,
      background: theme === 'light' ? '#f8f9fa' : '#2d3748',
      border: theme === 'light' ? '1px solid #e9ecef' : '1px solid #4a5568'
    },
    label: {
      ...styles.label,
      color: theme === 'light' ? '#495057' : '#cbd5e0'
    },
    input: {
      ...styles.input,
      background: theme === 'light' ? '#ffffff' : '#2d3748',
      color: theme === 'light' ? '#2c3e50' : '#e2e8f0',
      border: theme === 'light' ? '1px solid #ced4da' : '1px solid #4a5568'
    },
    card: {
      ...styles.card,
      background: theme === 'light' ? '#ffffff' : '#2d3748',
      border: theme === 'light' ? '1px solid #dee2e6' : '1px solid #4a5568'
    }
  };

  return (
    <div style={themeStyles.container} className={`theme-${theme}`} data-theme={theme}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '30px', paddingBottom: '10px', borderBottom: `2px solid ${theme === 'light' ? '#1890ff' : '#42a5f5'}` }}>
        <h1 style={{ margin: 0, borderBottom: 'none', paddingBottom: 0, color: theme === 'light' ? '#2c3e50' : '#e2e8f0' }}>Profil Bilgileri</h1>
        <button
          onClick={() => navigate('/dashboard')}
          style={{
            padding: '10px 20px',
            background: '#667eea',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer',
            fontWeight: 600,
            fontSize: '14px'
          }}
        >
          ← Dashboard'a Dön
        </button>
      </div>

      {/* --- KİŞİSEL BİLGİLER --- */}
      <section style={themeStyles.section}>
        <div style={{...styles.subHeader, color: theme === 'light' ? '#1890ff' : '#42a5f5'}}><h3 style={{color: theme === 'light' ? '#1890ff' : '#42a5f5', margin: 0}}>Kişisel Bilgiler</h3></div>
        <div style={styles.grid2}>
          <InputField label="Ad Soyad" name="fullName" value={form.fullName ?? ''} onChange={handleChange} theme={theme} />
          <InputField label="Email" value={form.email ?? ''} disabled theme={theme} />
          <InputField label="Telefon" name="phone" value={form.phone ?? ''} onChange={handleChange} theme={theme} />
          <InputField label="Lokasyon" name="location" value={form.location ?? ''} onChange={handleChange} theme={theme} />
        </div>
        <div style={styles.grid3}>
          <InputField label="LinkedIn" name="linkedinUrl" placeholder="https://linkedin.com/in/..." value={form.linkedinUrl ?? ''} onChange={handleChange} theme={theme} />
          <InputField label="GitHub" name="githubUrl" placeholder="https://github.com/..." value={form.githubUrl ?? ''} onChange={handleChange} theme={theme} />
          <InputField label="Website" name="websiteUrl" placeholder="https://..." value={form.websiteUrl ?? ''} onChange={handleChange} theme={theme} />
        </div>
      </section>

      {/* --- ÖZET --- */}
      <section style={themeStyles.section}>
        <div style={{...styles.subHeader, color: theme === 'light' ? '#1890ff' : '#42a5f5'}}><h3 style={{color: theme === 'light' ? '#1890ff' : '#42a5f5', margin: 0}}>Profesyonel Özet</h3></div>
        <InputField label="Ünvan (Title)" name="title" placeholder="Örn: Full Stack Developer" value={form.title ?? ''} onChange={handleChange} theme={theme} />
        <InputField label="Toplam Deneyim (Yıl)" type="number" name="totalExperienceYear" value={form.totalExperienceYear} onChange={handleChange} theme={theme} />
        <TextAreaField label="Özet" name="summary" placeholder="Kariyer hedefleriniz ve yetkinlikleriniz..." value={form.summary ?? ''} onChange={handleChange} theme={theme} />
      </section>

      {/* --- EĞİTİM BİLGİLERİ (GÜNCELLENMİŞ) --- */}
      <section style={themeStyles.section}>
        <div style={{...styles.subHeader, color: theme === 'light' ? '#1890ff' : '#42a5f5'}}>
          <h3 style={{color: theme === 'light' ? '#1890ff' : '#42a5f5', margin: 0}}>Eğitim Bilgileri</h3>
          <button onClick={() => addItem('educations')} style={{ ...styles.button, background: '#28a745' }}>+ Eğitim Ekle</button>
        </div>
        
        {form.educations.map((edu, idx) => (
          <div key={idx} style={themeStyles.card}>
            <div style={styles.grid2}>
              <InputField label="Okul / Kurum Adı" value={edu.schoolName} onChange={(e) => updateItem('educations', idx, 'schoolName', e.target.value)} theme={theme} />
              <InputField label="Bölüm / Alan" value={edu.department} onChange={(e) => updateItem('educations', idx, 'department', e.target.value)} theme={theme} />
            </div>
            
            <div style={styles.grid3}>
               <SelectField 
                 label="Derece / Seviye" 
                 options={[
                   { value: 'Yok', label: 'Diplomasız / Alaylı' },
                   { value: 'İlköğretim', label: 'İlköğretim (İlk/Orta)' },
                   { value: 'Lise', label: 'Lise' },
                   { value: 'Meslek Yüksekokulu', label: 'Meslek Yüksekokulu' },
                   { value: 'Önlisans', label: 'Önlisans' },
                   { value: 'Lisans', label: 'Lisans' },
                   { value: 'Yüksek Lisans', label: 'Yüksek Lisans' },
                   { value: 'Doktora', label: 'Doktora' },
                   { value: 'Doçent', label: 'Doçent (Doç. Dr.)' },
                   { value: 'Profesör', label: 'Profesör (Prof. Dr.)' }
                 ]}
                 value={edu.degree} 
                 onChange={(e) => updateItem('educations', idx, 'degree', e.target.value)}
                 theme={theme}
               />
               <InputField label="Başlangıç Yılı" placeholder="2010" value={edu.startYear} onChange={(e) => updateItem('educations', idx, 'startYear', e.target.value)} theme={theme} />
               <InputField label="Mezuniyet Yılı" placeholder="2014 veya Devam" value={edu.graduationYear} onChange={(e) => updateItem('educations', idx, 'graduationYear', e.target.value)} theme={theme} />
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
                <div style={{ width: '150px' }}>
                    <InputField label="Not Ortalaması (GPA)" placeholder="3.50 veya 85" value={edu.gpa} onChange={(e) => updateItem('educations', idx, 'gpa', e.target.value)} style={{marginBottom:0}} theme={theme} />
                </div>
                <button onClick={() => removeItem('educations', idx)} style={{ ...styles.button, background: '#dc3545', height: '38px' }}>Kaydı Sil</button>
            </div>
          </div>
        ))}
        {form.educations.length === 0 && <div style={{ color: theme === 'light' ? '#6c757d' : '#94a3b8', textAlign: 'center', fontStyle: 'italic' }}>Henüz eğitim bilgisi eklenmemiş.</div>}
      </section>

      {/* --- YETENEKLER --- */}
      <section style={themeStyles.section}>
        <div style={{...styles.subHeader, color: theme === 'light' ? '#1890ff' : '#42a5f5'}}>
          <h3 style={{color: theme === 'light' ? '#1890ff' : '#42a5f5', margin: 0}}>Yetenekler</h3>
          <button onClick={() => addItem('skills')} style={{ ...styles.button, background: '#28a745' }}>+ Yetenek Ekle</button>
        </div>
        {form.skills.map((skill, idx) => (
          <div key={idx} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr auto', gap: '10px', marginBottom: '10px' }}>
            <input style={{...themeStyles.input, ...styles.input}} placeholder="Yetenek" value={skill.skillName} onChange={(e) => updateItem('skills', idx, 'skillName', e.target.value)} />
            <select style={{...themeStyles.input, ...styles.input}} value={skill.level} onChange={(e) => updateItem('skills', idx, 'level', e.target.value)}>
              <option value="BEGINNER">Başlangıç</option>
              <option value="INTERMEDIATE">Orta</option>
              <option value="ADVANCED">İleri</option>
            </select>
            <input style={{...themeStyles.input, ...styles.input}} type="number" placeholder="Yıl" value={skill.years} onChange={(e) => updateItem('skills', idx, 'years', e.target.value)} />
            <button onClick={() => removeItem('skills', idx)} style={{ ...styles.button, background: '#dc3545' }}>Sil</button>
          </div>
        ))}
      </section>

      {/* --- DENEYİMLER --- */}
      <section style={themeStyles.section}>
        <div style={{...styles.subHeader, color: theme === 'light' ? '#1890ff' : '#42a5f5'}}>
          <h3 style={{color: theme === 'light' ? '#1890ff' : '#42a5f5', margin: 0}}>Deneyimler</h3>
          <button onClick={() => addItem('experiences')} style={{ ...styles.button, background: '#28a745' }}>+ Deneyim Ekle</button>
        </div>
        {form.experiences.map((exp, idx) => (
          <div key={idx} style={themeStyles.card}>
            <div style={styles.grid2}>
              <InputField label="Pozisyon" value={exp.position} onChange={(e) => updateItem('experiences', idx, 'position', e.target.value)} theme={theme} />
              <InputField label="Şirket" value={exp.company} onChange={(e) => updateItem('experiences', idx, 'company', e.target.value)} theme={theme} />
            </div>
            <div style={styles.grid2}>
              <InputField label="Başlangıç" type="month" value={exp.startDate} onChange={(e) => updateItem('experiences', idx, 'startDate', e.target.value)} theme={theme} />
              <InputField label="Bitiş" type="month" value={exp.endDate} onChange={(e) => updateItem('experiences', idx, 'endDate', e.target.value)} theme={theme} />
            </div>
            <TextAreaField label="Açıklama" value={exp.description} onChange={(e) => updateItem('experiences', idx, 'description', e.target.value)} theme={theme} />
            <div style={{ textAlign: 'right' }}>
              <button onClick={() => removeItem('experiences', idx)} style={{ ...styles.button, background: '#dc3545' }}>Sil</button>
            </div>
          </div>
        ))}
      </section>

      {/* --- PROJELER --- */}
      <section style={themeStyles.section}>
        <div style={{...styles.subHeader, color: theme === 'light' ? '#1890ff' : '#42a5f5'}}>
          <h3 style={{color: theme === 'light' ? '#1890ff' : '#42a5f5', margin: 0}}>Projeler</h3>
          <button onClick={() => addItem('projects')} style={{ ...styles.button, background: '#28a745' }}>+ Proje Ekle</button>
        </div>
        {form.projects.map((proj, idx) => (
          <div key={idx} style={themeStyles.card}>
            <div style={styles.grid2}>
              <InputField label="Proje Adı" value={proj.projectName} onChange={(e) => updateItem('projects', idx, 'projectName', e.target.value)} theme={theme} />
              <InputField label="Link" value={proj.url} onChange={(e) => updateItem('projects', idx, 'url', e.target.value)} theme={theme} />
            </div>
            <div style={{ display: 'flex', gap: '15px', alignItems: 'center', marginBottom: '15px' }}>
                <div style={{flex:1}}>
                    <InputField label="Başlangıç" type="month" value={proj.startDate} onChange={(e) => updateItem('projects', idx, 'startDate', e.target.value)} theme={theme} />
                </div>
                {!proj.isOngoing && (
                    <div style={{flex:1}}>
                        <InputField label="Bitiş" type="month" value={proj.endDate} onChange={(e) => updateItem('projects', idx, 'endDate', e.target.value)} theme={theme} />
                    </div>
                )}
                <div style={{ paddingTop: '15px' }}>
                    <label style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', color: theme === 'light' ? '#2c3e50' : '#e2e8f0' }}>
                        <input type="checkbox" checked={proj.isOngoing} onChange={() => toggleItemOngoing('projects', idx, 'endDate')} style={{marginRight: '5px'}}/> Devam Ediyor
                    </label>
                </div>
            </div>
            <TextAreaField label="Proje Detayı" value={proj.description} onChange={(e) => updateItem('projects', idx, 'description', e.target.value)} theme={theme} />
            <div style={{ textAlign: 'right' }}>
              <button onClick={() => removeItem('projects', idx)} style={{ ...styles.button, background: '#dc3545' }}>Sil</button>
            </div>
          </div>
        ))}
      </section>

      {/* --- DİLLER --- */}
      <section style={themeStyles.section}>
        <div style={styles.subHeader}>
          <h3>Yabancı Diller</h3>
          <button onClick={() => addItem('languages')} style={{ ...styles.button, background: '#28a745' }}>+ Dil Ekle</button>
        </div>
        {form.languages.map((lang, idx) => (
          <div key={idx} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr auto', gap: '10px', marginBottom: '10px' }}>
            <input style={{...themeStyles.input, ...styles.input}} placeholder="Dil" value={lang.language} onChange={(e) => updateItem('languages', idx, 'language', e.target.value)} />
            <select style={{...themeStyles.input, ...styles.input}} value={lang.level} onChange={(e) => updateItem('languages', idx, 'level', e.target.value)}>
              <option value="Beginner">Başlangıç</option>
              <option value="Intermediate">Orta</option>
              <option value="Advanced">İleri</option>
              <option value="Native">Anadil</option>
            </select>
            <button onClick={() => removeItem('languages', idx)} style={{ ...styles.button, background: '#dc3545' }}>Sil</button>
          </div>
        ))}
      </section>

      {/* --- SERTİFİKALAR (EKLENDİ) --- */}
      <section style={themeStyles.section}>
        <div style={{...styles.subHeader, color: theme === 'light' ? '#1890ff' : '#42a5f5'}}>
          <h3 style={{color: theme === 'light' ? '#1890ff' : '#42a5f5', margin: 0}}>Sertifikalar</h3>
          <button onClick={() => addItem('certificates')} style={{ ...styles.button, background: '#28a745' }}>+ Sertifika Ekle</button>
        </div>
        {form.certificates.map((cert, idx) => (
          <div key={idx} style={themeStyles.card}>
            <div style={styles.grid2}>
              <InputField label="Sertifika Adı" value={cert.name} onChange={(e) => updateItem('certificates', idx, 'name', e.target.value)} theme={theme} />
              <InputField label="Veren Kurum" value={cert.issuer} onChange={(e) => updateItem('certificates', idx, 'issuer', e.target.value)} theme={theme} />
            </div>
            <div style={{ display: 'flex', gap: '15px', alignItems: 'flex-end' }}>
               <div style={{flex: 1}}>
                  <InputField label="Tarih" placeholder="2023-05" value={cert.date} onChange={(e) => updateItem('certificates', idx, 'date', e.target.value)} style={{marginBottom: 0}} theme={theme} />
               </div>
               <div style={{flex: 2}}>
                  <InputField label="Link" placeholder="https://..." value={cert.url} onChange={(e) => updateItem('certificates', idx, 'url', e.target.value)} style={{marginBottom: 0}} theme={theme} />
               </div>
               <button onClick={() => removeItem('certificates', idx)} style={{ ...styles.button, background: '#dc3545', height: '38px', marginBottom: '10px' }}>Sil</button>
            </div>
          </div>
        ))}
      </section>

      {/* --- SAVE BAR --- */}
      <div style={{ 
        position: 'sticky', 
        bottom: 0, 
        background: 'transparent', 
        padding: '20px 0 0 0', 
        borderTop: 'none', 
        boxShadow: 'none',
        zIndex: 1000,
        marginTop: '40px'
      }}>
        <button 
          onClick={handleSave} 
          disabled={saving}
          type="button"
          style={{ 
            width: '100%', 
            padding: '15px 24px', 
            fontSize: '16px', 
            background: saving ? '#9ca3af' : '#667eea', 
            color: 'white',
            fontWeight: 'bold',
            border: 'none',
            borderRadius: '8px',
            cursor: saving ? 'not-allowed' : 'pointer',
            opacity: saving ? 0.7 : 1,
            boxShadow: saving ? 'none' : '0 4px 12px rgba(102, 126, 234, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px'
          }}
          onMouseEnter={(e) => {
            if (!saving) {
              e.currentTarget.style.background = '#5568d3';
              e.currentTarget.style.transform = 'translateY(-2px)';
              e.currentTarget.style.boxShadow = '0 6px 16px rgba(102, 126, 234, 0.4)';
            }
          }}
          onMouseLeave={(e) => {
            if (!saving) {
              e.currentTarget.style.background = '#667eea';
              e.currentTarget.style.transform = 'translateY(0)';
              e.currentTarget.style.boxShadow = '0 4px 12px rgba(102, 126, 234, 0.3)';
            }
          }}
        >
          {saving ? (
            <>
              <span>⏳</span>
              <span>Kaydediliyor...</span>
            </>
          ) : (
            <>
              <span>💾</span>
              <span>PROFİLİ KAYDET</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
}

export default ProfilePage;