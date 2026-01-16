import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
  Platform,
} from 'react-native';
import * as FileSystem from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import * as Print from 'expo-print';
import { toByteArray, fromByteArray } from 'base64-js';
import { useTheme } from '../contexts/ThemeContext';
import { cvAPI, userManager, profileAPI } from '../services/api';

const CVBuilderScreen = () => {
  const { theme } = useTheme();
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState({});
  const [cvs, setCvs] = useState([]);
  const [selectedCV, setSelectedCV] = useState(null); // Preview için seçilen CV
  const [profileData, setProfileData] = useState(null); // Profil verileri
  const [summaryIndex, setSummaryIndex] = useState(0); // Özet seçimi için index
  
  // CV değiştiğinde summary index'i sıfırla
  useEffect(() => {
    if (selectedCV) {
      setSummaryIndex(0);
    }
  }, [selectedCV]);
  
  // Debug için CV listesi değişikliklerini logla
  React.useEffect(() => {
    console.log('CV listesi güncellendi, yeni uzunluk:', cvs.length);
    if (cvs.length > 0) {
      console.log('CV listesi içeriği:', cvs.map(cv => ({ id: cv.id || cv.cvId, createdAt: cv.createdAt || cv.created_at })));
    }
  }, [cvs]);

  useEffect(() => {
    loadCVs();
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      const response = await profileAPI.getMe();
      setProfileData(response.data.data || response.data);
    } catch (err) {
      console.log('Profil yüklenemedi:', err.message);
    }
  };

  const loadCVs = async () => {
    try {
      console.log('CV listesi yükleniyor...');
      const userId = await userManager.getUserId();
      console.log('User ID:', userId);
      
      const response = await cvAPI.getMyCVs();
      console.log('CV listesi response:', response);
      console.log('CV listesi response.data:', response.data);
      
      // Farklı response formatlarını kontrol et
      let data = null;
      if (response.data) {
        if (Array.isArray(response.data)) {
          data = response.data;
        } else if (response.data.data && Array.isArray(response.data.data)) {
          data = response.data.data;
        } else if (response.data.cvs && Array.isArray(response.data.cvs)) {
          data = response.data.cvs;
        } else if (Array.isArray(response.data.list)) {
          data = response.data.list;
        }
      }
      
      const cvList = Array.isArray(data) ? data : [];
      console.log('Yüklenen CV sayısı:', cvList.length);
      if (cvList.length > 0) {
        console.log('CV örnekleri:', cvList.slice(0, 2));
        // Mevcut listeyle birleştir (yeni CV'ler eklenmiş olabilir)
        setCvs(prev => {
          // Yeni CV'leri mevcut listeye ekle (duplicate kontrolü ile)
          const merged = [...prev];
          cvList.forEach(newCv => {
            const exists = merged.some(cv => 
              (cv.id === newCv.id || cv.cvId === newCv.cvId || cv.id === newCv.cvId || cv.cvId === newCv.id)
            );
            if (!exists) {
              merged.push(newCv);
            }
          });
          // ID'ye göre sırala (en yeni önce)
          merged.sort((a, b) => {
            const aId = a.id || a.cvId || 0;
            const bId = b.id || b.cvId || 0;
            return bId - aId;
          });
          return merged;
        });
      } else {
        // Backend'den boş liste geldi ama mevcut listeyi koru
        console.log('Backend\'den boş liste geldi, mevcut liste korunuyor');
      }
      
      if (cvList.length === 0) {
        console.log('CV listesi boş - bu normal olabilir (henüz CV oluşturulmamış)');
      }
    } catch (error) {
      // 404 hatası normal olabilir (kullanıcının henüz CV'si yoksa veya endpoint yoksa)
      // 404 durumunda mevcut listeyi koru, sıfırlama ve hata gösterme!
      if (error.response?.status === 404) {
        console.log('404 hatası - CV listesi endpoint\'i bulunamadı. Mevcut liste korunuyor.');
        // setCvs([]) çağrılmayacak - mevcut liste korunacak
        // Hata mesajı da gösterilmeyecek (sessizce handle edilecek)
      } else {
        console.error('CV listesi yükleme hatası:', error);
        console.error('Error response:', error.response?.data);
        console.error('Error status:', error.response?.status);
        // Sadece 404 dışındaki hatalarda log göster
      }
    }
  };

  const handleDownloadCV = async (cvId) => {
    if (downloading[cvId]) {
      return; // Zaten indiriliyor
    }

    try {
      setDownloading(prev => ({ ...prev, [cvId]: true }));
      
      console.log('CV indiriliyor, ID:', cvId);
      
      // Backend'de download endpoint'i yok, bu yüzden CV verilerini alıp PDF oluşturuyoruz
      // Önce CV verilerini bul (liste içinde)
      const cvData = cvs.find(cv => (cv.id === cvId || cv.cvId === cvId));
      
      if (!cvData) {
        Alert.alert('Hata', 'CV verileri bulunamadı. Lütfen sayfayı yenileyin.');
        setDownloading(prev => {
          const newState = { ...prev };
          delete newState[cvId];
          return newState;
        });
        return;
      }

      console.log('CV verileri bulundu:', cvData);

      // Profil verilerini al (CV oluşturulurken kullanılan veriler)
      let profileData = null;
      try {
        console.log('Profil verileri alınıyor...');
        const profileResponse = await profileAPI.getMe();
        profileData = profileResponse.data.data || profileResponse.data;
        console.log('Profil verileri alındı');
      } catch (err) {
        console.log('Profil verileri alınamadı, CV verileri kullanılacak:', err.message);
      }

      // CV verilerini kullanarak HTML oluştur
      console.log('HTML içeriği oluşturuluyor...');
      const htmlContent = generateCVHTML(cvData, profileData);
      console.log('HTML içeriği oluşturuldu, uzunluk:', htmlContent.length);
      
      // PDF oluştur - timeout ile
      console.log('PDF oluşturuluyor...');
      
      // Daha basit print options - daha hızlı
      const printOptions = {
        html: htmlContent,
        base64: false,
      };

      // Timeout ekle (15 saniye) - daha kısa timeout
      console.log('PDF oluşturma başlatıldı, bekleniyor...');
      
      const pdfPromise = Print.printToFileAsync(printOptions);
      const timeoutPromise = new Promise((_, reject) => 
        setTimeout(() => {
          console.error('PDF oluşturma timeout!');
          reject(new Error('PDF oluşturma zaman aşımına uğradı (15 saniye). Lütfen tekrar deneyin veya daha sonra deneyin.'));
        }, 15000)
      );

      const result = await Promise.race([pdfPromise, timeoutPromise]);
      const uri = result.uri || result;
      console.log('CV PDF oluşturuldu:', uri);

      // PDF'i direkt Downloads klasörüne kaydet
      try {
        const fileName = `CV_${cvId}_${Date.now()}.pdf`;
        let destinationUri;
        
        if (Platform.OS === 'android') {
          // Android için Downloads klasörüne kaydetmeyi dene
          // Android'de genellikle /storage/emulated/0/Download/ veya /sdcard/Download/ kullanılır
          // Ancak expo-file-system ile doğrudan erişim sınırlı olabilir
          try {
            // Önce Downloads klasörüne direkt kaydetmeyi dene
            const downloadsPath = '/storage/emulated/0/Download/';
            destinationUri = downloadsPath + fileName;
            
            // Dosyayı kopyala
            await FileSystem.copyAsync({
              from: uri,
              to: destinationUri,
            });
            
            console.log('CV PDF Downloads klasörüne kaydedildi:', destinationUri);
            Alert.alert(
              'Başarılı', 
              `CV başarıyla PDF olarak indirildi!\n\nDosya: ${fileName}\n\nDosya yöneticisinde Downloads klasöründe bulabilirsiniz.`,
              [{ text: 'Tamam' }]
            );
            return; // Başarılı oldu, çık
          } catch (directSaveError) {
            console.log('Direkt Downloads klasörüne kaydetme başarısız, alternatif yöntem deneniyor:', directSaveError);
            // Direkt kaydetme başarısız oldu, cache klasörüne kaydet
            destinationUri = FileSystem.cacheDirectory + fileName;
          }
        } else {
          // iOS için Documents klasörüne kaydet
          destinationUri = FileSystem.documentDirectory + fileName;
        }
        
        // Dosyayı kopyala (cache veya Documents klasörüne)
        await FileSystem.copyAsync({
          from: uri,
          to: destinationUri,
        });
        
        console.log('CV PDF kaydedildi:', destinationUri);
        
        // Paylaşım menüsünü aç - kullanıcı "Save" veya "Download" seçeneğini seçebilir
        const isAvailable = await Sharing.isAvailableAsync();
        if (isAvailable) {
          await Sharing.shareAsync(destinationUri, {
            mimeType: 'application/pdf',
            dialogTitle: Platform.OS === 'android' ? 'CV\'yi Downloads klasörüne kaydet' : 'CV\'yi kaydet',
            UTI: 'com.adobe.pdf',
          });
          Alert.alert(
            'Bilgi', 
            Platform.OS === 'android' 
              ? 'Paylaşım menüsünden "Kaydet" veya "İndir" seçeneğini seçerek CV\'nizi Downloads klasörüne kaydedebilirsiniz.'
              : 'Paylaşım menüsünden "Files" uygulamasına kaydedebilirsiniz.',
            [{ text: 'Tamam' }]
          );
        } else {
          Alert.alert('Bilgi', `CV PDF oluşturuldu:\n${destinationUri}`);
        }
      } catch (saveError) {
        console.error('Dosya kaydetme hatası:', saveError);
        // Kaydetme başarısız olursa orijinal URI'yi paylaş
        const isAvailable = await Sharing.isAvailableAsync();
        if (isAvailable) {
          await Sharing.shareAsync(uri, {
            mimeType: 'application/pdf',
            dialogTitle: 'CV\'nizi paylaşın veya kaydedin',
          });
          Alert.alert('Bilgi', 'CV PDF oluşturuldu. Paylaşım menüsünden kaydedebilirsiniz.');
        } else {
          Alert.alert('Bilgi', `CV PDF oluşturuldu:\n${uri}`);
        }
      }
    } catch (error) {
      console.error('CV indirme hatası:', error);
      console.error('Hata detayları:', JSON.stringify(error, null, 2));
      
      let errorMessage = 'CV PDF oluşturulamadı';
      
      if (error.message) {
        errorMessage = error.message;
      } else if (error.toString) {
        errorMessage = error.toString();
      }
      
      Alert.alert('Hata', `CV PDF oluşturulurken hata oluştu:\n\n${errorMessage}\n\nLütfen tekrar deneyin.`);
    } finally {
      setDownloading(prev => {
        const newState = { ...prev };
        delete newState[cvId];
        return newState;
      });
    }
  };

  // CV verilerini HTML'e çevir
  const generateCVHTML = (cvData, profileData) => {
    const safe = (v) => (v && String(v).trim() ? String(v).trim() : 'Belirtilmemiş');
    
    // Web versiyonundaki cleanText fonksiyonu
    const cleanText = (text) => {
      if (!text) return "";
      return text.replace(/[@#*_`>]/g, "").replace(/\s{2,}/g, " ").trim();
    };
    
    // Özet metninden açıklama metinlerini temizle
    const cleanSummary = (summary) => {
      if (!summary) return "";
      let cleaned = String(summary).trim();
      
      // "Aşağıdaki özetler" gibi açıklama metinlerini kaldır
      const patterns = [
        /^Aşağıdaki\s+\d+\s+farklı\s+özete\s+örnek\s+olarak\s+sunulmaktadır:\s*/i,
        /^Aşağıdaki\s+özetler[^:]*:\s*/i,
        /^Aşağıdaki\s+[^:]*:\s*/i,
        /^Bu\s+özet[^:]*:\s*/i,
        /^Özet[^:]*:\s*/i,
      ];
      
      patterns.forEach(pattern => {
        cleaned = cleaned.replace(pattern, '');
      });
      
      return cleanText(cleaned);
    };
    
    const fullName = profileData?.fullName || cvData?.fullName || 'CV';
    const title = profileData?.title || '';
    const email = profileData?.email || '';
    
    // Özet - seçilen özeti kullan ve temizle
    const baseSummary = cvData?.tailoredSummary || profileData?.summary || '';
    const summaries = cvData?.summaries && cvData.summaries.length > 0
      ? cvData.summaries.map(s => cleanSummary(s))
      : baseSummary ? [cleanSummary(baseSummary)] : [];
    const currentSummary = summaries[summaryIndex] || '';
    
    // Tekrar eden cümleleri temizle
    const cleanDescription = (desc) => {
      if (!desc) return null;
      if (Array.isArray(desc)) {
        const unique = [];
        const seen = new Set();
        desc.forEach(d => {
          const normalized = String(d).trim().toLowerCase();
          if (!seen.has(normalized) && normalized.length > 0) {
            seen.add(normalized);
            unique.push(d);
          }
        });
        return unique.length > 0 ? unique : null;
      }
      return desc;
    };
    
    const skills = cvData?.prioritizedSkills || profileData?.skills?.map(s => s.skillName || s) || [];
    const experiences = (cvData?.optimizedExperiences || profileData?.experiences || []).map(exp => ({
      ...exp,
      description: cleanDescription(exp.description)
    }));
    const educations = cvData?.optimizedEducation || profileData?.educations || [];
    const projects = (cvData?.optimizedProjects || profileData?.projects || []).map(proj => ({
      ...proj,
      description: cleanDescription(proj.description)
    }));
    const languages = cvData?.optimizedLanguages || profileData?.languages || [];
    const certificates = cvData?.optimizedCertificates || profileData?.certificates || [];

    return `
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="UTF-8">
          <style>
            @page {
              margin: 15mm;
            }
            body {
              font-family: Arial, sans-serif;
              padding: 10px;
              color: #333;
              line-height: 1.5;
              font-size: 12px;
            }
            .header {
              text-align: center;
              margin-bottom: 30px;
              border-bottom: 2px solid #000000;
              padding-bottom: 20px;
            }
            .header h1 {
              margin: 0;
              font-size: 28px;
              color: #000000;
              font-weight: bold;
            }
            .header p {
              margin: 5px 0;
              color: #333333;
            }
            .section {
              margin-bottom: 25px;
            }
            .section-title {
              font-size: 18px;
              font-weight: bold;
              color: #000000;
              margin-bottom: 10px;
              border-left: 4px solid #000000;
              padding-left: 10px;
              text-transform: uppercase;
            }
            .item {
              margin-bottom: 15px;
            }
            .item-title {
              font-weight: bold;
              font-size: 16px;
              color: #000000;
            }
            .item-subtitle {
              color: #333333;
              font-size: 14px;
            }
            .item-date {
              color: #666666;
              font-size: 12px;
            }
            .skills-list {
              margin: 10px 0;
            }
            .skill-tag {
              display: inline-block;
              background: #f5f5f5;
              padding: 4px 8px;
              margin: 4px 4px 4px 0;
              border: 1px solid #cccccc;
              border-radius: 3px;
              font-size: 12px;
              color: #000000;
            }
            ul {
              margin: 5px 0;
              padding-left: 20px;
            }
            li {
              margin-bottom: 5px;
            }
          </style>
        </head>
        <body>
          <div class="header">
            <h1>${safe(fullName)}</h1>
            <p>${safe(title)}</p>
            ${email ? `<p>${safe(email)}</p>` : ''}
          </div>

          ${currentSummary ? `
          <div class="section">
            <div class="section-title">PROFESYONEL ÖZET</div>
            <p>${currentSummary}</p>
          </div>
          ` : ''}

          ${skills.length > 0 ? `
          <div class="section">
            <div class="section-title">YETENEKLER</div>
            <div class="skills-list">
              ${skills.map(skill => `<span class="skill-tag">${safe(skill)}</span>`).join(' ')}
            </div>
          </div>
          ` : ''}

          ${experiences.length > 0 ? `
          <div class="section">
            <div class="section-title">DENEYİM</div>
            ${experiences.map(exp => `
              <div class="item">
                <div class="item-title">${safe(exp.title || exp.position)}</div>
                <div class="item-subtitle">${safe(exp.subtitle || exp.company)}</div>
                <div class="item-date">${safe(exp.date || `${exp.startDate || ''} - ${exp.endDate || ''}`)}</div>
                ${exp.description ? `
                  <ul>
                    ${Array.isArray(exp.description) 
                      ? exp.description.map(d => `<li>${safe(d)}</li>`).join('')
                      : `<li>${safe(exp.description)}</li>`}
                  </ul>
                ` : ''}
              </div>
            `).join('')}
          </div>
          ` : ''}

          ${educations.length > 0 ? `
          <div class="section">
            <div class="section-title">EĞİTİM</div>
            ${educations.map(edu => `
              <div class="item">
                <div class="item-title">${safe(edu.schoolName)}</div>
                <div class="item-subtitle">${safe(edu.department)} - ${safe(edu.degree)}</div>
                <div class="item-date">${safe(edu.startYear)} - ${safe(edu.graduationYear || 'Devam')}</div>
                ${edu.gpa ? `<div class="item-date">GPA: ${safe(edu.gpa)}</div>` : ''}
              </div>
            `).join('')}
          </div>
          ` : ''}

          ${projects.length > 0 ? `
          <div class="section">
            <div class="section-title">PROJELER</div>
            ${projects.map(proj => `
              <div class="item">
                <div class="item-title">${safe(proj.title || proj.projectName)}</div>
                <div class="item-subtitle">${safe(proj.subtitle || 'Proje')}</div>
                <div class="item-date">${safe(proj.date || `${proj.startDate || ''} - ${proj.endDate || ''}`)}</div>
                ${proj.description ? `
                  <ul>
                    ${Array.isArray(proj.description) 
                      ? proj.description.map(d => `<li>${safe(d)}</li>`).join('')
                      : `<li>${safe(proj.description)}</li>`}
                  </ul>
                ` : ''}
              </div>
            `).join('')}
          </div>
          ` : ''}

          ${languages.length > 0 ? `
          <div class="section">
            <div class="section-title">DİLLER</div>
            ${languages.map(lang => `
              <div class="item">
                <span class="item-title">${safe(lang.language)}</span>
                <span class="item-subtitle"> - ${safe(lang.level)}</span>
              </div>
            `).join('')}
          </div>
          ` : ''}

          ${certificates.length > 0 ? `
          <div class="section">
            <div class="section-title">SERTİFİKALAR</div>
            ${certificates.map(cert => `
              <div class="item">
                <div class="item-title">${safe(cert.name)}</div>
                <div class="item-subtitle">${safe(cert.issuer)}</div>
                ${cert.date ? `<div class="item-date">${safe(cert.date)}</div>` : ''}
              </div>
            `).join('')}
          </div>
          ` : ''}
        </body>
      </html>
    `;
  };

  const handleGenerateCV = async () => {
    try {
      setLoading(true);
      const userId = await userManager.getUserId();
      if (!userId) {
        Alert.alert('Hata', 'Kullanıcı bilgisi bulunamadı. Lütfen tekrar giriş yapın.');
        setLoading(false);
        return;
      }
      
      console.log('CV oluşturuluyor, userId:', userId);
      
      // CV oluşturma işlemi uzun sürebilir (5 dakika timeout var)
      const response = await cvAPI.generateCV(userId);
      console.log('CV oluşturma response:', response.data);
      
      const responseData = response.data?.data || response.data;
      console.log('CV oluşturma response data:', responseData);
      
      // Yeni CV'yi direkt listeye ekle (response'dan)
      if (responseData) {
        const cvId = responseData.cvId || responseData.id || Date.now();
        const newCV = {
          id: cvId,
          cvId: cvId,
          createdAt: responseData.createdAt || responseData.created_at || new Date().toISOString(),
          updatedAt: responseData.updatedAt || responseData.updated_at,
          templateName: responseData.templateName || 'ATS_SMART_FULL_V3',
          ...responseData
        };
        
        console.log('Yeni CV listeye ekleniyor:', newCV);
        
        // State'i güncelle - önceki listeyi koru ve yeni CV'yi başa ekle
        setCvs(prev => {
          // Aynı ID'ye sahip CV varsa güncelle, yoksa ekle
          const existingIndex = prev.findIndex(cv => 
            (cv.id === newCV.id || cv.cvId === newCV.cvId || cv.id === newCV.cvId || cv.cvId === newCV.id)
          );
          
          if (existingIndex >= 0) {
            const updated = [...prev];
            updated[existingIndex] = newCV;
            console.log('CV güncellendi, yeni liste uzunluğu:', updated.length);
            return updated;
          }
          
          const newList = [newCV, ...prev];
          console.log('CV eklendi, yeni liste uzunluğu:', newList.length);
          console.log('Yeni liste içeriği:', newList.map(cv => ({ id: cv.id, cvId: cv.cvId })));
          return newList;
        });
        
        Alert.alert(
          'Başarılı', 
          `CV başarıyla oluşturuldu!\n\nCV ID: ${cvId}\nOluşturulma: ${new Date(newCV.createdAt).toLocaleString('tr-TR')}`,
          [{ 
            text: 'Tamam',
            onPress: () => {
              console.log('CV oluşturuldu');
            }
          }]
        );
      } else {
        // Response'da CV verisi yoksa, manuel olarak oluştur
        const manualCV = {
          id: Date.now(),
          cvId: Date.now(),
          createdAt: new Date().toISOString(),
          templateName: 'ATS_SMART_FULL_V3',
        };
        setCvs(prev => {
          const newList = [manualCV, ...prev];
          console.log('Manuel CV eklendi, yeni liste uzunluğu:', newList.length);
          return newList;
        });
        
        Alert.alert('Başarılı', 'CV oluşturuldu!');
      }
      
      // CV zaten response'dan listeye eklendi, backend'den tekrar yüklemeye gerek yok
      // (404 hatası alınıyor ve liste sıfırlanıyordu)
    } catch (error) {
      console.error('CV oluşturma hatası:', error);
      let errorMessage = 'CV oluşturulamadı';
      
      if (error.response) {
        // Sunucudan gelen hata
        const status = error.response.status;
        const data = error.response.data;
        
        if (status === 400) {
          errorMessage = data?.message || 'Geçersiz istek. Profil bilgilerinizi kontrol edin.';
        } else if (status === 404) {
          errorMessage = 'CV oluşturma servisi bulunamadı. Backend\'i kontrol edin.';
        } else if (status === 500) {
          errorMessage = 'Sunucu hatası. Lütfen daha sonra tekrar deneyin.';
        } else {
          errorMessage = data?.message || data?.error || `Sunucu hatası: ${status}`;
        }
      } else if (error.request) {
        // İstek gönderildi ama yanıt alınamadı
        errorMessage = 'Backend\'e ulaşılamadı. Backend\'in çalıştığından ve aynı ağda olduğunuzdan emin olun.';
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      Alert.alert('Hata', errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // CV Preview Render Fonksiyonu
  const renderCVPreview = () => {
    if (!selectedCV) return null;

    const cvData = selectedCV;
    const safe = (v) => (v && String(v).trim() ? String(v).trim() : 'Belirtilmemiş');
    
    // Web versiyonundaki cleanText fonksiyonu
    const cleanText = (text) => {
      if (!text) return "";
      return text.replace(/[@#*_`>]/g, "").replace(/\s{2,}/g, " ").trim();
    };
    
    // Özet metninden açıklama metinlerini temizle
    const cleanSummary = (summary) => {
      if (!summary) return "";
      let cleaned = String(summary).trim();
      
      // "Aşağıdaki özetler" gibi açıklama metinlerini kaldır
      const patterns = [
        /^Aşağıdaki\s+\d+\s+farklı\s+özete\s+örnek\s+olarak\s+sunulmaktadır:\s*/i,
        /^Aşağıdaki\s+özetler[^:]*:\s*/i,
        /^Aşağıdaki\s+[^:]*:\s*/i,
        /^Bu\s+özet[^:]*:\s*/i,
        /^Özet[^:]*:\s*/i,
      ];
      
      patterns.forEach(pattern => {
        cleaned = cleaned.replace(pattern, '');
      });
      
      return cleanText(cleaned);
    };
    
    const fullName = profileData?.fullName || cvData?.fullName || 'CV';
    const title = profileData?.title || '';
    const email = profileData?.email || '';
    
    // Özet listesi - web'deki gibi
    const baseSummary = cvData?.tailoredSummary || profileData?.summary || '';
    const summaries = cvData?.summaries && cvData.summaries.length > 0
      ? cvData.summaries.map(s => cleanSummary(s))
      : baseSummary ? [cleanSummary(baseSummary)] : [];
    const currentSummary = summaries[summaryIndex] || '';
    
    const handleChangeSummary = () => {
      if (summaries.length <= 1) return;
      setSummaryIndex((prev) => (prev + 1) % summaries.length);
    };
    
    const skills = cvData?.prioritizedSkills || profileData?.skills?.map(s => s.skillName || s) || [];
    const experiences = cvData?.optimizedExperiences || profileData?.experiences || [];
    const educations = cvData?.optimizedEducation || profileData?.educations || [];
    const projects = cvData?.optimizedProjects || profileData?.projects || [];
    const languages = cvData?.optimizedLanguages || profileData?.languages || [];
    const certificates = cvData?.optimizedCertificates || profileData?.certificates || [];
    const cvId = cvData.id || cvData.cvId;

    return (
      <View style={styles.previewContainer}>
        <View style={styles.previewHeader}>
          <Text style={styles.previewTitle}>CV Önizleme</Text>
          <TouchableOpacity
            style={styles.closeButton}
            onPress={() => setSelectedCV(null)}
          >
            <Text style={styles.closeButtonText}>✕ Kapat</Text>
          </TouchableOpacity>
        </View>

        <ScrollView style={styles.previewContent} contentContainerStyle={styles.previewContentContainer}>
          {/* Header */}
          <View style={styles.cvHeader}>
            <Text style={styles.cvHeaderName}>{safe(fullName)}</Text>
            {title ? <Text style={styles.cvHeaderTitle}>{safe(title)}</Text> : null}
            <View style={styles.cvHeaderContact}>
              {email ? <Text style={styles.cvHeaderText}>{safe(email)}</Text> : null}
            </View>
          </View>

          {/* Summary */}
          {currentSummary ? (
            <View style={styles.cvSection}>
              <View style={styles.cvSectionHeader}>
                <Text style={styles.cvSectionTitle}>PROFESYONEL ÖZET</Text>
                {summaries.length > 1 && (
                  <TouchableOpacity
                    style={styles.changeSummaryButton}
                    onPress={handleChangeSummary}
                  >
                    <Text style={styles.changeSummaryButtonText}>
                      Metni Değiştir ({summaryIndex + 1}/{summaries.length})
                    </Text>
                  </TouchableOpacity>
                )}
              </View>
              <Text style={styles.cvSectionText}>{currentSummary}</Text>
            </View>
          ) : null}

          {/* Skills */}
          {skills.length > 0 ? (
            <View style={styles.cvSection}>
              <Text style={styles.cvSectionTitle}>YETENEKLER</Text>
              <View style={styles.skillsContainer}>
                {skills.map((skill, idx) => (
                  <View key={idx} style={styles.skillTag}>
                    <Text style={styles.skillTagText}>{safe(skill)}</Text>
                  </View>
                ))}
              </View>
            </View>
          ) : null}

          {/* Experiences */}
          {experiences.length > 0 ? (
            <View style={styles.cvSection}>
              <Text style={styles.cvSectionTitle}>DENEYİM</Text>
              {experiences.map((exp, idx) => {
                // Tekrar eden cümleleri temizle
                const cleanDescription = (desc) => {
                  if (!desc) return null;
                  if (Array.isArray(desc)) {
                    // Array'deki tekrar eden cümleleri kaldır
                    const unique = [];
                    const seen = new Set();
                    desc.forEach(d => {
                      const normalized = String(d).trim().toLowerCase();
                      if (!seen.has(normalized) && normalized.length > 0) {
                        seen.add(normalized);
                        unique.push(d);
                      }
                    });
                    return unique.length > 0 ? unique : null;
                  }
                  return desc;
                };
                
                const cleanDesc = cleanDescription(exp.description);
                
                return (
                  <View key={idx} style={styles.cvItem}>
                    <Text style={styles.cvItemTitle}>{safe(exp.title || exp.position)}</Text>
                    <Text style={styles.cvItemSubtitle}>{safe(exp.subtitle || exp.company)}</Text>
                    <Text style={styles.cvItemDate}>{safe(exp.date || `${exp.startDate || ''} - ${exp.endDate || ''}`)}</Text>
                    {cleanDesc ? (
                      <View style={styles.cvItemDescription}>
                        {Array.isArray(cleanDesc) ? (
                          cleanDesc.map((d, i) => (
                            <Text key={i} style={styles.cvItemBullet}>• {safe(d)}</Text>
                          ))
                        ) : (
                          <Text style={styles.cvItemBullet}>• {safe(cleanDesc)}</Text>
                        )}
                      </View>
                    ) : null}
                  </View>
                );
              })}
            </View>
          ) : null}

          {/* Education */}
          {educations.length > 0 ? (
            <View style={styles.cvSection}>
              <Text style={styles.cvSectionTitle}>EĞİTİM</Text>
              {educations.map((edu, idx) => (
                <View key={idx} style={styles.cvItem}>
                  <Text style={styles.cvItemTitle}>{safe(edu.schoolName)}</Text>
                  <Text style={styles.cvItemSubtitle}>{safe(edu.department)} - {safe(edu.degree)}</Text>
                  <Text style={styles.cvItemDate}>{safe(edu.startYear)} - {safe(edu.graduationYear || 'Devam')}</Text>
                  {edu.gpa ? <Text style={styles.cvItemDate}>GPA: {safe(edu.gpa)}</Text> : null}
                </View>
              ))}
            </View>
          ) : null}

          {/* Projects */}
          {projects.length > 0 ? (
            <View style={styles.cvSection}>
              <Text style={styles.cvSectionTitle}>PROJELER</Text>
              {projects.map((proj, idx) => {
                // Tekrar eden cümleleri temizle
                const cleanDescription = (desc) => {
                  if (!desc) return null;
                  if (Array.isArray(desc)) {
                    const unique = [];
                    const seen = new Set();
                    desc.forEach(d => {
                      const normalized = String(d).trim().toLowerCase();
                      if (!seen.has(normalized) && normalized.length > 0) {
                        seen.add(normalized);
                        unique.push(d);
                      }
                    });
                    return unique.length > 0 ? unique : null;
                  }
                  return desc;
                };
                
                const cleanDesc = cleanDescription(proj.description);
                
                return (
                  <View key={idx} style={styles.cvItem}>
                    <Text style={styles.cvItemTitle}>{safe(proj.title || proj.projectName)}</Text>
                    <Text style={styles.cvItemSubtitle}>{safe(proj.subtitle || 'Proje')}</Text>
                    <Text style={styles.cvItemDate}>{safe(proj.date || `${proj.startDate || ''} - ${proj.endDate || ''}`)}</Text>
                    {cleanDesc ? (
                      <View style={styles.cvItemDescription}>
                        {Array.isArray(cleanDesc) ? (
                          cleanDesc.map((d, i) => (
                            <Text key={i} style={styles.cvItemBullet}>• {safe(d)}</Text>
                          ))
                        ) : (
                          <Text style={styles.cvItemBullet}>• {safe(cleanDesc)}</Text>
                        )}
                      </View>
                    ) : null}
                  </View>
                );
              })}
            </View>
          ) : null}

          {/* Languages */}
          {languages.length > 0 ? (
            <View style={styles.cvSection}>
              <Text style={styles.cvSectionTitle}>DİLLER</Text>
              {languages.map((lang, idx) => (
                <View key={idx} style={styles.cvItem}>
                  <Text style={styles.cvItemTitle}>{safe(lang.language)}</Text>
                  <Text style={styles.cvItemSubtitle}> - {safe(lang.level)}</Text>
                </View>
              ))}
            </View>
          ) : null}

          {/* Certificates */}
          {certificates.length > 0 ? (
            <View style={styles.cvSection}>
              <Text style={styles.cvSectionTitle}>SERTİFİKALAR</Text>
              {certificates.map((cert, idx) => (
                <View key={idx} style={styles.cvItem}>
                  <Text style={styles.cvItemTitle}>{safe(cert.name)}</Text>
                  <Text style={styles.cvItemSubtitle}>{safe(cert.issuer)}</Text>
                  {cert.date ? <Text style={styles.cvItemDate}>{safe(cert.date)}</Text> : null}
                </View>
              ))}
            </View>
          ) : null}
        </ScrollView>

        {/* Action Buttons */}
        <View style={styles.previewActions}>
          <TouchableOpacity
            style={styles.previewDownloadButton}
            onPress={() => handleDownloadCV(cvId)}
            disabled={downloading[cvId]}
          >
            {downloading[cvId] ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.previewDownloadButtonText}>📥 PDF İndir</Text>
            )}
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.previewCloseButton}
            onPress={() => setSelectedCV(null)}
          >
            <Text style={styles.previewCloseButtonText}>Kapat</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  };

  return (
    <View style={[styles.container, theme === 'dark' && styles.containerDark]}>
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.content}
      >
      <View style={styles.card}>
        <Text style={styles.title}>CV Oluştur</Text>
        <Text style={styles.description}>
          Profesyonel CV'nizi oluşturun veya mevcut CV'lerinizi görüntüleyin
        </Text>

        <View style={styles.infoBox}>
          <Text style={styles.infoText}>
            💡 CV oluşturma işlemi birkaç dakika sürebilir. Lütfen bekleyin.
          </Text>
        </View>

        <TouchableOpacity
          style={[styles.button, loading && styles.buttonDisabled]}
          onPress={handleGenerateCV}
          disabled={loading}
        >
          {loading ? (
            <View style={styles.loadingContainer}>
              <ActivityIndicator color="#fff" />
              <Text style={styles.loadingText}>CV oluşturuluyor...</Text>
            </View>
          ) : (
            <Text style={styles.buttonText}>📄 Yeni CV Oluştur</Text>
          )}
        </TouchableOpacity>
      </View>

      <View style={styles.card}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Mevcut CV'lerim ({cvs.length})</Text>
          <TouchableOpacity
            style={styles.refreshButton}
            onPress={loadCVs}
          >
            <Text style={styles.refreshButtonText}>🔄 Yenile</Text>
          </TouchableOpacity>
        </View>
        
        {cvs.length > 0 ? (
          cvs.map((cv, index) => {
            const cvId = cv.id || cv.cvId || index;
            const createdAt = cv.createdAt || cv.created_at || new Date().toISOString();
            const templateName = cv.templateName || 'ATS_SMART_FULL_V3';
            
            return (
              <View key={`cv-${cvId}-${index}`} style={styles.cvItem}>
                <View style={styles.cvInfo}>
                  <Text style={styles.cvTitle}>
                    CV #{index + 1}
                  </Text>
                  <Text style={styles.cvDate}>
                    {new Date(createdAt).toLocaleDateString('tr-TR', {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </Text>
                  <Text style={styles.cvTemplate}>
                    Şablon: {templateName}
                  </Text>
                </View>
                <TouchableOpacity
                  style={[styles.downloadButton, downloading[cvId] && styles.downloadButtonDisabled]}
                  onPress={() => handleDownloadCV(cvId)}
                  disabled={downloading[cvId]}
                >
                  {downloading[cvId] ? (
                    <ActivityIndicator color="#fff" size="small" />
                  ) : (
                    <Text style={styles.downloadButtonText}>📥 İndir</Text>
                  )}
                </TouchableOpacity>
              </View>
            );
          })
        ) : (
          <View style={styles.emptyState}>
            <Text style={styles.emptyStateText}>Henüz CV oluşturulmamış</Text>
            <Text style={styles.emptyStateSubtext}>
              Yukarıdaki butona tıklayarak ilk CV'nizi oluşturabilirsiniz
            </Text>
          </View>
        )}
      </View>
        </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#e3f2fd',
  },
  containerDark: {
    backgroundColor: '#1a202c',
  },
  scrollView: {
    flex: 1,
  },
  content: {
    padding: 20,
  },
  previewContainer: {
    flex: 1,
    backgroundColor: '#fff',
  },
  previewHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#f5f5f5',
    borderBottomWidth: 1,
    borderBottomColor: '#ddd',
  },
  previewTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  closeButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  closeButtonText: {
    fontSize: 16,
    color: '#666',
  },
  previewContent: {
    flex: 1,
  },
  previewContentContainer: {
    padding: 20,
  },
  cvHeader: {
    alignItems: 'center',
    marginBottom: 24,
    paddingBottom: 16,
    borderBottomWidth: 2,
    borderBottomColor: '#000',
  },
  cvHeaderName: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#000',
    marginBottom: 8,
  },
  cvHeaderTitle: {
    fontSize: 16,
    color: '#333',
    marginBottom: 8,
  },
  cvHeaderContact: {
    marginTop: 8,
  },
  cvHeaderText: {
    fontSize: 14,
    color: '#333',
  },
  cvSection: {
    marginBottom: 20,
  },
  cvSectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  cvSectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#000',
    textTransform: 'uppercase',
    borderLeftWidth: 4,
    borderLeftColor: '#000',
    paddingLeft: 10,
    flex: 1,
  },
  changeSummaryButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    backgroundColor: '#f0f0f0',
    borderRadius: 4,
    marginLeft: 10,
  },
  changeSummaryButtonText: {
    fontSize: 12,
    color: '#2196F3',
    fontWeight: '500',
  },
  cvSectionText: {
    fontSize: 14,
    color: '#333',
    lineHeight: 22,
    textAlign: 'left',
    marginTop: 8,
    paddingRight: 4,
  },
  skillsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 8,
  },
  skillTag: {
    backgroundColor: '#f5f5f5',
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 4,
    paddingHorizontal: 10,
    paddingVertical: 6,
    marginRight: 8,
    marginBottom: 8,
  },
  skillTagText: {
    fontSize: 12,
    color: '#000',
  },
  cvItem: {
    marginBottom: 16,
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  cvItemTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#000',
    marginBottom: 4,
    marginTop: 4,
  },
  cvItemSubtitle: {
    fontSize: 14,
    color: '#333',
    marginBottom: 4,
    marginTop: 2,
  },
  cvItemDate: {
    fontSize: 12,
    color: '#666',
    marginBottom: 8,
    marginTop: 2,
  },
  cvItemDescription: {
    marginTop: 8,
    marginLeft: 8,
  },
  cvItemBullet: {
    fontSize: 13,
    color: '#333',
    marginBottom: 4,
    lineHeight: 20,
  },
  cvItemDescription: {
    marginTop: 8,
  },
  cvItemBullet: {
    fontSize: 13,
    color: '#333',
    marginBottom: 4,
    lineHeight: 18,
  },
  previewActions: {
    flexDirection: 'row',
    padding: 16,
    backgroundColor: '#f5f5f5',
    borderTopWidth: 1,
    borderTopColor: '#ddd',
  },
  previewDownloadButton: {
    flex: 1,
    backgroundColor: '#4CAF50',
    padding: 14,
    borderRadius: 8,
    alignItems: 'center',
    marginRight: 8,
  },
  previewDownloadButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  previewCloseButton: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    padding: 14,
    borderRadius: 8,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#ddd',
  },
  previewCloseButtonText: {
    color: '#666',
    fontSize: 16,
    fontWeight: 'bold',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 24,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 8,
  },
  description: {
    fontSize: 14,
    color: '#666',
    marginBottom: 24,
  },
  button: {
    backgroundColor: '#2196F3',
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  infoBox: {
    backgroundColor: '#e3f2fd',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
  },
  infoText: {
    fontSize: 13,
    color: '#1976d2',
    lineHeight: 18,
  },
  loadingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  loadingText: {
    color: '#fff',
    fontSize: 14,
    marginLeft: 8,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  refreshButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    backgroundColor: '#e3f2fd',
    borderRadius: 8,
  },
  refreshButtonText: {
    fontSize: 14,
    color: '#1976d2',
    fontWeight: 'bold',
  },
  cvItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  cvInfo: {
    flex: 1,
    marginRight: 12,
  },
  cvTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 4,
  },
  cvDate: {
    fontSize: 12,
    color: '#666',
    marginBottom: 4,
  },
  cvTemplate: {
    fontSize: 11,
    color: '#999',
    fontStyle: 'italic',
  },
  emptyState: {
    padding: 24,
    alignItems: 'center',
  },
  emptyStateText: {
    fontSize: 16,
    color: '#666',
    marginBottom: 8,
  },
  emptyStateSubtext: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
  },
  downloadButton: {
    backgroundColor: '#4CAF50',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
    minWidth: 80,
    alignItems: 'center',
    justifyContent: 'center',
  },
  downloadButtonDisabled: {
    opacity: 0.6,
  },
  downloadButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
  },
});

export default CVBuilderScreen;

