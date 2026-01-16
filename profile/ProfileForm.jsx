// src/pages/ProfilePage.jsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { profileAPI } from '../services/api';

// --- SABİTLER VE STİLLER ---
const styles = {
  container: { maxWidth: '900px', margin: '0 auto', padding: '24px', fontFamily: 'Arial, sans-serif' },
  section: { marginBottom: '30px', padding: '20px', background: '#f8f9fa', borderRadius: '8px' },
  header: { borderBottom: '2px solid #1890ff', paddingBottom: '10px', marginBottom: '30px' },
  subHeader: { color: '#1890ff', marginBottom: '15px', marginTop: 0 },
  label: { display: 'block', marginBottom: '5px', fontWeight: '500', fontSize: '14px' },
  input: { width: '100%', padding: '10px', border: '1px solid #ddd', borderRadius: '4px', boxSizing: 'border-box' },
  button: { cursor: 'pointer', borderRadius: '4px', border: 'none', padding: '8px 16px', color: 'white' },
  grid2: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '15px' },
  grid3: { display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '15px', marginBottom: '15px' },
};

// YENİ: Eğitim şablonu eklendi
const TEMPLATES = {
  skills: { skillName: '', level: 'INTERMEDIATE', years: 0 },
  experiences: { position: '', company: '', city: '', startDate: '', endDate: '', description: '' },
  languages: { language: '', level: 'Intermediate' },
  certificates: { name: '', issuer: '', date: '', url: '' },
  projects: { projectName: '', startDate: '', endDate: '', isOngoing: false },
  educations: { schoolName: '', department: '', degree: 'Lisans', startYear: '', graduationYear: '', gpa: '' } // YENİ
};

const emptyProfile = {
  fullName: '', email: '', phone: '', location: '',
  linkedinUrl: '', githubUrl: '', websiteUrl: '',
  title: '', totalExperienceYear: 0, summary: '',
  // Eski tekil eğitim alanları kaldırıldı -> yerine liste geldi
  educations: [], 
  skills: [], experiences: [], languages: [], certificates: [], projects: [],
};

// --- YARDIMCI BİLEŞENLER ---
const InputField = ({ label, ...props }) => (
  <div style={{ marginBottom: '15px' }}>
    {label && <label style={styles.label}>{label}</label>}
    <input style={{...styles.input, background: props.disabled ? '#f5f5f5' : 'white'}} {...props} />
  </div>
);

const TextAreaField = ({ label, ...props }) => (
  <div style={{ marginBottom: '15px' }}>
    {label && <label style={styles.label}>{label}</label>}
    <textarea style={{ ...styles.input, minHeight: '80px', fontFamily: 'inherit' }} {...props} />
  </div>
);

const SelectField = ({ label, options, ...props }) => (
  <div style={{ marginBottom: '15px' }}>
    {label && <label style={styles.label}>{label}</label>}
    <select style={styles.input} {...props}>
      {options.map(opt => (
        <option key={opt.value} value={opt.value}>{opt.label}</option>
      ))}
    </select>
  </div>
);

// --- ANA COMPONENT ---
function ProfilePage() {
  const [form, setForm] = useState(emptyProfile);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const navigate = useNavigate();

  // ---------------- DATA FETCHING ----------------
  useEffect(() => {
    const fetchProfile = async () => {
      try {
        setLoading(true);
        const res = await profileAPI.getMe();
        const data = res.data || {};
        
        // Formu doldur
        setForm(prev => ({
          ...prev,
          ...data,
          educations: data.educations || [], // YENİ: Listeyi al
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

  // ---------------- GENEL HANDLERS ----------------
  
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : (name === 'totalExperienceYear' ? (parseInt(value) || 0) : value)
    }));
  };

  const addItem = (field) => {
    setForm(prev => ({
      ...prev,
      [field]: [...prev[field], { ...TEMPLATES[field] }]
    }));
  };

  const removeItem = (field, index) => {
    setForm(prev => ({
      ...prev,
      [field]: prev[field].filter((_, i) => i !== index)
    }));
  };

  const updateItem = (field, index, subField, value) => {
    setForm(prev => {
      const newList = [...prev[field]];
      newList[index] = { ...newList[index], [subField]: value };
      return { ...prev, [field]: newList };
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
        projects: form.projects.map(p => ({ ...p, endDate: p.isOngoing ? '' : p.endDate })),
        // Educations listesi olduğu gibi gider
      };

      const res = await profileAPI.updateMe(payload);
      
      if (res.data) setForm(prev => ({ ...prev, ...res.data }));
      
      alert('Profil başarıyla güncellendi!');
      // İsteğe bağlı: dashboard'a yönlendir veya kal
      // setTimeout(() => navigate('/cv-builder'), 1000); 
    } catch (err) {
      console.error('Kaydetme hatası:', err);
      const msg = err.response?.data?.message || err.message || 'Bir hata oluştu';
      alert(`Kaydedilirken hata oluştu: ${msg}`);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div style={{ textAlign: 'center', padding: '50px', color: '#666' }}>Profil yükleniyor...</div>;

  return (
    <div style={styles.container}>
      <h1 style={styles.header}>Profil Bilgileri</h1>

      {/* --- KİŞİSEL BİLGİLER --- */}
      <section style={styles.section}>
        <h3 style={styles.subHeader}>Kişisel Bilgiler</h3>
        <div style={styles.grid2}>
          <InputField label="Ad Soyad" name="fullName" value={form.fullName} onChange={handleChange} />
          <InputField label="Telefon" name="phone" value={form.phone} onChange={handleChange} />
          <InputField label="Lokasyon" name="location" value={form.location} onChange={handleChange} />
          <InputField label="Email" value={form.email} disabled />
        </div>
        <div style={styles.grid3}>
          <InputField label="LinkedIn URL" name="linkedinUrl" placeholder="https://linkedin.com/..." value={form.linkedinUrl} onChange={handleChange} />
          <InputField label="GitHub URL" name="githubUrl" placeholder="https://github.com/..." value={form.githubUrl} onChange={handleChange} />
          <InputField label="Website URL" name="websiteUrl" placeholder="https://example.com" value={form.websiteUrl} onChange={handleChange} />
        </div>
      </section>

      {/* --- ÖZET --- */}
      <section style={styles.section}>
        <h3 style={styles.subHeader}>Profesyonel Özet</h3>
        <InputField label="Ünvan" name="title" placeholder="Örn: Backend Developer" value={form.title} onChange={handleChange} />
        <InputField label="Toplam Deneyim (Yıl)" type="number" name="totalExperienceYear" min="0" value={form.totalExperienceYear} onChange={handleChange} />
        <TextAreaField label="Özet Metni" name="summary" placeholder="Kısa özetiniz..." value={form.summary} onChange={handleChange} />
      </section>

      {/* --- YENİ: EĞİTİM BİLGİLERİ (LİSTE) --- */}
      <section style={styles.section}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
          <h3 style={styles.subHeader}>Eğitim Bilgileri</h3>
          <button onClick={() => addItem('educations')} style={{ ...styles.button, background: '#28a745' }}>+ Eğitim Ekle</button>
        </div>
        {form.educations.map((edu, idx) => (
          <div key={idx} style={{ marginBottom: '20px', padding: '20px', border: '1px solid #ddd', borderRadius: '8px', background: 'white' }}>
            <div style={styles.grid2}>
              <InputField label="Okul / Üniversite" value={edu.schoolName} onChange={(e) => updateItem('educations', idx, 'schoolName', e.target.value)} />
              <InputField label="Bölüm" value={edu.department} onChange={(e) => updateItem('educations', idx, 'department', e.target.value)} />
            </div>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '15px' }}>
               <div>
                 <label style={styles.label}>Derece</label>
                 <select style={styles.input} value={edu.degree} onChange={(e) => updateItem('educations', idx, 'degree', e.target.value)}>
                    <option value="Önlisans">Önlisans</option>
                    <option value="Lisans">Lisans</option>
                    <option value="Yüksek Lisans">Yüksek Lisans</option>
                    <option value="Doktora">Doktora</option>
                 </select>
               </div>
               <InputField label="Başlangıç Yılı" placeholder="2018" value={edu.startYear} onChange={(e) => updateItem('educations', idx, 'startYear', e.target.value)} />
               <InputField label="Mezuniyet Yılı" placeholder="2022 veya Devam" value={edu.graduationYear} onChange={(e) => updateItem('educations', idx, 'graduationYear', e.target.value)} />
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '10px' }}>
                <div style={{width: '30%'}}>
                    <InputField label="Not Ortalaması (GPA)" placeholder="3.50" value={edu.gpa} onChange={(e) => updateItem('educations', idx, 'gpa', e.target.value)} style={{marginBottom:0}} />
                </div>
                <button onClick={() => removeItem('educations', idx)} style={{ ...styles.button, background: '#dc3545', height: '40px', marginTop: '10px' }}>Okulu Sil</button>
            </div>
          </div>
        ))}
        {form.educations.length === 0 && <div style={{ textAlign: 'center', color: '#666' }}>Henüz eğitim bilgisi eklenmemiş.</div>}
      </section>

      {/* --- YETENEKLER --- */}
      <section style={styles.section}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
          <h3 style={styles.subHeader}>Yetenekler</h3>
          <button onClick={() => addItem('skills')} style={{ ...styles.button, background: '#28a745' }}>+ Yetenek Ekle</button>
        </div>
        {form.skills.map((skill, idx) => (
          <div key={idx} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr auto', gap: '10px', marginBottom: '10px' }}>
            <input style={styles.input} placeholder="Yetenek" value={skill.skillName} onChange={(e) => updateItem('skills', idx, 'skillName', e.target.value)} />
            <select style={styles.input} value={skill.level} onChange={(e) => updateItem('skills', idx, 'level', e.target.value)}>
              <option value="BEGINNER">Başlangıç</option>
              <option value="INTERMEDIATE">Orta</option>
              <option value="ADVANCED">İleri</option>
            </select>
            <input style={styles.input} type="number" placeholder="Yıl" value={skill.years} onChange={(e) => updateItem('skills', idx, 'years', e.target.value)} />
            <button onClick={() => removeItem('skills', idx)} style={{ ...styles.button, background: '#dc3545' }}>Sil</button>
          </div>
        ))}
        {form.skills.length === 0 && <div style={{ textAlign: 'center', color: '#666' }}>Henüz yetenek eklenmemiş.</div>}
      </section>

      {/* --- DENEYİMLER --- */}
      <section style={styles.section}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
          <h3 style={styles.subHeader}>Deneyimler</h3>
          <button onClick={() => addItem('experiences')} style={{ ...styles.button, background: '#28a745' }}>+ Deneyim Ekle</button>
        </div>
        {form.experiences.map((exp, idx) => (
          <div key={idx} style={{ marginBottom: '20px', padding: '20px', border: '1px solid #ddd', borderRadius: '8px', background: 'white' }}>
            <div style={styles.grid3}>
              <InputField label="Pozisyon" value={exp.position} onChange={(e) => updateItem('experiences', idx, 'position', e.target.value)} />
              <InputField label="Şirket" value={exp.company} onChange={(e) => updateItem('experiences', idx, 'company', e.target.value)} />
              <InputField label="Şehir" value={exp.city} onChange={(e) => updateItem('experiences', idx, 'city', e.target.value)} />
            </div>
            <div style={styles.grid2}>
              <InputField label="Başlangıç" placeholder="2022-01" value={exp.startDate} onChange={(e) => updateItem('experiences', idx, 'startDate', e.target.value)} />
              <InputField label="Bitiş" placeholder="2023-01" value={exp.endDate} onChange={(e) => updateItem('experiences', idx, 'endDate', e.target.value)} />
            </div>
            <TextAreaField label="Açıklama" value={exp.description} onChange={(e) => updateItem('experiences', idx, 'description', e.target.value)} />
            <div style={{ textAlign: 'right' }}>
              <button onClick={() => removeItem('experiences', idx)} style={{ ...styles.button, background: '#dc3545' }}>Deneyimi Sil</button>
            </div>
          </div>
        ))}
      </section>

      {/* --- PROJELER --- */}
      <section style={styles.section}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
          <h3 style={styles.subHeader}>Projeler</h3>
          <button onClick={() => addItem('projects')} style={{ ...styles.button, background: '#28a745' }}>+ Proje Ekle</button>
        </div>
        {form.projects.map((project, idx) => (
          <div key={idx} style={{ marginBottom: '20px', padding: '20px', border: '1px solid #ddd', borderRadius: '8px', background: 'white' }}>
            <div style={styles.grid2}>
              <InputField label="Proje Adı" value={project.projectName} onChange={(e) => updateItem('projects', idx, 'projectName', e.target.value)} style={{marginBottom:0}}/>
              <InputField label="Başlangıç Tarihi" placeholder="2023-01" value={project.startDate} onChange={(e) => updateItem('projects', idx, 'startDate', e.target.value)} style={{marginBottom:0}}/>
            </div>
            <div style={{ display: 'flex', gap: '20px', alignItems: 'center', marginBottom: '15px', marginTop:'15px' }}>
              <label style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px' }}>
                <input type="checkbox" checked={project.isOngoing} onChange={(e) => updateItem('projects', idx, 'isOngoing', e.target.checked)} />
                <span style={{ fontWeight: '500' }}>Devam ediyor</span>
              </label>
              {!project.isOngoing && (
                <div style={{ flex: 1 }}>
                  <InputField label="Bitiş Tarihi" placeholder="2023-06" value={project.endDate} onChange={(e) => updateItem('projects', idx, 'endDate', e.target.value)} />
                </div>
              )}
            </div>
            <TextAreaField label="Proje Açıklaması" value={project.description} onChange={(e) => updateItem('projects', idx, 'description', e.target.value)} />
            <div style={{ textAlign: 'right' }}>
              <button onClick={() => removeItem('projects', idx)} style={{ ...styles.button, background: '#dc3545' }}>Projeyi Sil</button>
            </div>
          </div>
        ))}
      </section>

      {/* --- DİLLER & SERTİFİKALAR (Kısaltıldı, yapı aynı) --- */}
      <section style={styles.section}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
          <h3 style={styles.subHeader}>Yabancı Diller</h3>
          <button onClick={() => addItem('languages')} style={{ ...styles.button, background: '#28a745' }}>+ Dil Ekle</button>
        </div>
        {form.languages.map((lang, idx) => (
          <div key={idx} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr auto', gap: '10px', marginBottom: '10px' }}>
            <input style={styles.input} placeholder="Dil" value={lang.language} onChange={(e) => updateItem('languages', idx, 'language', e.target.value)} />
            <select style={styles.input} value={lang.level} onChange={(e) => updateItem('languages', idx, 'level', e.target.value)}>
              <option value="Beginner">Başlangıç</option>
              <option value="Intermediate">Orta</option>
              <option value="Advanced">İleri</option>
              <option value="Native">Anadil</option>
            </select>
            <button onClick={() => removeItem('languages', idx)} style={{ ...styles.button, background: '#dc3545' }}>Sil</button>
          </div>
        ))}
      </section>

      <section style={styles.section}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
          <h3 style={styles.subHeader}>Sertifikalar</h3>
          <button onClick={() => addItem('certificates')} style={{ ...styles.button, background: '#28a745' }}>+ Sertifika Ekle</button>
        </div>
        {form.certificates.map((cert, idx) => (
          <div key={idx} style={{ marginBottom: '15px', padding: '15px', border: '1px solid #ddd', background: 'white', borderRadius: '5px' }}>
            <div style={styles.grid2}>
              <InputField label="Sertifika Adı" value={cert.name} onChange={(e) => updateItem('certificates', idx, 'name', e.target.value)} style={{marginBottom:0}} />
              <InputField label="Veren Kurum" value={cert.issuer} onChange={(e) => updateItem('certificates', idx, 'issuer', e.target.value)} style={{marginBottom:0}} />
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr auto', gap: '10px', alignItems: 'flex-end', marginTop:'15px' }}>
              <div style={{marginBottom: '0'}}>
                 <label style={styles.label}>Tarih</label>
                 <input style={styles.input} placeholder="2023-05" value={cert.date} onChange={(e) => updateItem('certificates', idx, 'date', e.target.value)} />
              </div>
              <div style={{marginBottom: '0'}}>
                 <label style={styles.label}>Link</label>
                 <input style={styles.input} placeholder="https://..." value={cert.url} onChange={(e) => updateItem('certificates', idx, 'url', e.target.value)} />
              </div>
              <button onClick={() => removeItem('certificates', idx)} style={{ ...styles.button, background: '#dc3545', height: '38px' }}>Sil</button>
            </div>
          </div>
        ))}
      </section>

      {/* --- SAVE BUTTON --- */}
      <div style={{ position: 'sticky', bottom: 0, background: 'white', padding: '20px 0', borderTop: '1px solid #eee' }}>
        <button onClick={handleSave} disabled={saving} style={{ ...styles.button, width: '100%', padding: '15px', fontSize: '16px', background: saving ? '#6c757d' : '#1890ff' }}>
          {saving ? '⏳ Kaydediliyor...' : '💾 Tüm Değişiklikleri Kaydet'}
        </button>
      </div>
    </div>
  );
}

export default ProfilePage;