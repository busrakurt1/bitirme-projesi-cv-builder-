import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
  Switch,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { profileAPI } from '../services/api';

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

const ProfileScreen = () => {
  const navigation = useNavigation();
  const [form, setForm] = useState(emptyProfile);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      const res = await profileAPI.getMe();
      const data = res.data.data || res.data;
      setForm({
        ...emptyProfile,
        ...data,
        educations: data.educations || [],
        skills: data.skills || [],
        experiences: data.experiences || [],
        languages: data.languages || [],
        certificates: data.certificates || [],
        projects: data.projects || [],
      });
    } catch (err) {
      console.error('Profil yükleme hatası:', err);
      Alert.alert('Hata', 'Profil yüklenemedi');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (field, value) => {
    setForm(prev => ({
      ...prev,
      [field]: field === 'totalExperienceYear' ? (Number(value) || 0) : value
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

  const toggleItemOngoing = (field, index) => {
    setForm(prev => {
      const list = [...prev[field]];
      const current = list[index];
      list[index] = {
        ...current,
        isOngoing: !current.isOngoing,
        endDate: !current.isOngoing ? '' : current.endDate
      };
      return { ...prev, [field]: list };
    });
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      const payload = {
        ...form,
        totalExperienceYear: Number(form.totalExperienceYear) || 0,
        skills: form.skills.map(s => ({ ...s, years: Number(s.years) || 0 })),
      };
      await profileAPI.updateMe(payload);
      Alert.alert('Başarılı', 'Profil başarıyla kaydedildi!');
      await loadProfile();
    } catch (err) {
      console.error('Kaydetme hatası:', err);
      Alert.alert('Hata', 'Kaydetme sırasında hata oluştu.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#2196F3" />
        <Text style={styles.loadingText}>Profil yükleniyor...</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Kişisel Bilgiler */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Kişisel Bilgiler</Text>
        <View style={styles.row}>
          <View style={styles.halfWidth}>
            <Text style={styles.label}>Ad Soyad</Text>
            <TextInput
              style={styles.input}
              value={form.fullName}
              onChangeText={(text) => handleChange('fullName', text)}
              placeholder="Adınız Soyadınız"
            />
          </View>
          <View style={styles.halfWidth}>
            <Text style={styles.label}>Telefon</Text>
            <TextInput
              style={styles.input}
              value={form.phone}
              onChangeText={(text) => handleChange('phone', text)}
              placeholder="Telefon"
              keyboardType="phone-pad"
            />
          </View>
        </View>
        <View style={styles.row}>
          <View style={styles.halfWidth}>
            <Text style={styles.label}>Lokasyon</Text>
            <TextInput
              style={styles.input}
              value={form.location}
              onChangeText={(text) => handleChange('location', text)}
              placeholder="Şehir"
            />
          </View>
          <View style={styles.halfWidth}>
            <Text style={styles.label}>Email</Text>
            <TextInput
              style={[styles.input, styles.inputDisabled]}
              value={form.email}
              editable={false}
            />
          </View>
        </View>
        <View style={styles.row}>
          <View style={styles.thirdWidth}>
            <Text style={styles.label}>LinkedIn</Text>
            <TextInput
              style={styles.input}
              value={form.linkedinUrl}
              onChangeText={(text) => handleChange('linkedinUrl', text)}
              placeholder="URL"
            />
          </View>
          <View style={styles.thirdWidth}>
            <Text style={styles.label}>GitHub</Text>
            <TextInput
              style={styles.input}
              value={form.githubUrl}
              onChangeText={(text) => handleChange('githubUrl', text)}
              placeholder="URL"
            />
          </View>
          <View style={styles.thirdWidth}>
            <Text style={styles.label}>Website</Text>
            <TextInput
              style={styles.input}
              value={form.websiteUrl}
              onChangeText={(text) => handleChange('websiteUrl', text)}
              placeholder="URL"
            />
          </View>
        </View>
      </View>

      {/* Profesyonel Özet */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Profesyonel Özet</Text>
        <Text style={styles.label}>Ünvan</Text>
        <TextInput
          style={styles.input}
          value={form.title}
          onChangeText={(text) => handleChange('title', text)}
          placeholder="Örn: Full Stack Developer"
        />
        <Text style={styles.label}>Toplam Deneyim (Yıl)</Text>
        <TextInput
          style={styles.input}
          value={String(form.totalExperienceYear)}
          onChangeText={(text) => handleChange('totalExperienceYear', text)}
          placeholder="0"
          keyboardType="numeric"
        />
        <Text style={styles.label}>Özet</Text>
        <TextInput
          style={[styles.input, styles.textArea]}
          value={form.summary}
          onChangeText={(text) => handleChange('summary', text)}
          placeholder="Kariyer hedefleriniz ve yetkinlikleriniz..."
          multiline
          numberOfLines={4}
        />
      </View>

      {/* Eğitim Bilgileri */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Eğitim Bilgileri</Text>
          <TouchableOpacity style={styles.addButton} onPress={() => addItem('educations')}>
            <Text style={styles.addButtonText}>+ Eğitim Ekle</Text>
          </TouchableOpacity>
        </View>
        {form.educations.map((edu, idx) => (
          <View key={idx} style={styles.itemCard}>
            <View style={styles.row}>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Okul / Kurum</Text>
                <TextInput
                  style={styles.input}
                  value={edu.schoolName}
                  onChangeText={(text) => updateItem('educations', idx, 'schoolName', text)}
                  placeholder="Okul adı"
                />
              </View>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Bölüm / Alan</Text>
                <TextInput
                  style={styles.input}
                  value={edu.department}
                  onChangeText={(text) => updateItem('educations', idx, 'department', text)}
                  placeholder="Bölüm"
                />
              </View>
            </View>
            <Text style={styles.label}>Derece / Seviye</Text>
            <View style={styles.picker}>
              <Text style={styles.pickerText}>{edu.degree || 'Lisans'}</Text>
            </View>
            <View style={styles.row}>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Başlangıç Yılı</Text>
                <TextInput
                  style={styles.input}
                  value={edu.startYear}
                  onChangeText={(text) => updateItem('educations', idx, 'startYear', text)}
                  placeholder="2010"
                />
              </View>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Mezuniyet Yılı</Text>
                <TextInput
                  style={styles.input}
                  value={edu.graduationYear}
                  onChangeText={(text) => updateItem('educations', idx, 'graduationYear', text)}
                  placeholder="2014"
                />
              </View>
            </View>
            <View style={styles.row}>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Not Ortalaması (GPA)</Text>
                <TextInput
                  style={styles.input}
                  value={edu.gpa}
                  onChangeText={(text) => updateItem('educations', idx, 'gpa', text)}
                  placeholder="3.50"
                />
              </View>
              <TouchableOpacity
                style={styles.deleteButton}
                onPress={() => removeItem('educations', idx)}
              >
                <Text style={styles.deleteButtonText}>Sil</Text>
              </TouchableOpacity>
            </View>
          </View>
        ))}
      </View>

      {/* Yetenekler */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Yetenekler</Text>
          <TouchableOpacity style={styles.addButton} onPress={() => addItem('skills')}>
            <Text style={styles.addButtonText}>+ Yetenek Ekle</Text>
          </TouchableOpacity>
        </View>
        {form.skills.map((skill, idx) => (
          <View key={idx} style={styles.itemCard}>
            <View style={styles.row}>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Yetenek</Text>
                <TextInput
                  style={styles.input}
                  value={skill.skillName}
                  onChangeText={(text) => updateItem('skills', idx, 'skillName', text)}
                  placeholder="Yetenek adı"
                />
              </View>
              <View style={styles.quarterWidth}>
                <Text style={styles.label}>Seviye</Text>
                <View style={styles.picker}>
                  <Text style={styles.pickerText}>{skill.level || 'INTERMEDIATE'}</Text>
                </View>
              </View>
              <View style={styles.quarterWidth}>
                <Text style={styles.label}>Yıl</Text>
                <TextInput
                  style={styles.input}
                  value={String(skill.years || 0)}
                  onChangeText={(text) => updateItem('skills', idx, 'years', Number(text) || 0)}
                  placeholder="0"
                  keyboardType="numeric"
                />
              </View>
              <TouchableOpacity
                style={styles.deleteButtonSmall}
                onPress={() => removeItem('skills', idx)}
              >
                <Text style={styles.deleteButtonText}>Sil</Text>
              </TouchableOpacity>
            </View>
          </View>
        ))}
      </View>

      {/* Deneyimler */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Deneyimler</Text>
          <TouchableOpacity style={styles.addButton} onPress={() => addItem('experiences')}>
            <Text style={styles.addButtonText}>+ Deneyim Ekle</Text>
          </TouchableOpacity>
        </View>
        {form.experiences.map((exp, idx) => (
          <View key={idx} style={styles.itemCard}>
            <View style={styles.row}>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Pozisyon</Text>
                <TextInput
                  style={styles.input}
                  value={exp.position}
                  onChangeText={(text) => updateItem('experiences', idx, 'position', text)}
                  placeholder="Pozisyon"
                />
              </View>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Şirket</Text>
                <TextInput
                  style={styles.input}
                  value={exp.company}
                  onChangeText={(text) => updateItem('experiences', idx, 'company', text)}
                  placeholder="Şirket"
                />
              </View>
            </View>
            <View style={styles.row}>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Başlangıç</Text>
                <TextInput
                  style={styles.input}
                  value={exp.startDate}
                  onChangeText={(text) => updateItem('experiences', idx, 'startDate', text)}
                  placeholder="2022-01"
                />
              </View>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Bitiş</Text>
                <TextInput
                  style={styles.input}
                  value={exp.endDate}
                  onChangeText={(text) => updateItem('experiences', idx, 'endDate', text)}
                  placeholder="2023-01"
                />
              </View>
            </View>
            <Text style={styles.label}>Açıklama</Text>
            <TextInput
              style={[styles.input, styles.textArea]}
              value={exp.description}
              onChangeText={(text) => updateItem('experiences', idx, 'description', text)}
              placeholder="Açıklama"
              multiline
              numberOfLines={3}
            />
            <TouchableOpacity
              style={styles.deleteButton}
              onPress={() => removeItem('experiences', idx)}
            >
              <Text style={styles.deleteButtonText}>Sil</Text>
            </TouchableOpacity>
          </View>
        ))}
      </View>

      {/* Projeler */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Projeler</Text>
          <TouchableOpacity style={styles.addButton} onPress={() => addItem('projects')}>
            <Text style={styles.addButtonText}>+ Proje Ekle</Text>
          </TouchableOpacity>
        </View>
        {form.projects.map((proj, idx) => (
          <View key={idx} style={styles.itemCard}>
            <Text style={styles.label}>Proje Adı</Text>
            <TextInput
              style={styles.input}
              value={proj.projectName}
              onChangeText={(text) => updateItem('projects', idx, 'projectName', text)}
              placeholder="Proje adı"
            />
            <View style={styles.row}>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Başlangıç</Text>
                <TextInput
                  style={styles.input}
                  value={proj.startDate}
                  onChangeText={(text) => updateItem('projects', idx, 'startDate', text)}
                  placeholder="2023-01"
                />
              </View>
              {!proj.isOngoing && (
                <View style={styles.halfWidth}>
                  <Text style={styles.label}>Bitiş</Text>
                  <TextInput
                    style={styles.input}
                    value={proj.endDate}
                    onChangeText={(text) => updateItem('projects', idx, 'endDate', text)}
                    placeholder="2023-06"
                  />
                </View>
              )}
            </View>
            <View style={styles.row}>
              <Text style={styles.label}>Devam Ediyor</Text>
              <Switch
                value={proj.isOngoing}
                onValueChange={() => toggleItemOngoing('projects', idx)}
              />
            </View>
            <Text style={styles.label}>Açıklama</Text>
            <TextInput
              style={[styles.input, styles.textArea]}
              value={proj.description}
              onChangeText={(text) => updateItem('projects', idx, 'description', text)}
              placeholder="Proje detayı"
              multiline
              numberOfLines={3}
            />
            <TouchableOpacity
              style={styles.deleteButton}
              onPress={() => removeItem('projects', idx)}
            >
              <Text style={styles.deleteButtonText}>Sil</Text>
            </TouchableOpacity>
          </View>
        ))}
      </View>

      {/* Diller */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Yabancı Diller</Text>
          <TouchableOpacity style={styles.addButton} onPress={() => addItem('languages')}>
            <Text style={styles.addButtonText}>+ Dil Ekle</Text>
          </TouchableOpacity>
        </View>
        {form.languages.map((lang, idx) => (
          <View key={idx} style={styles.itemCard}>
            <View style={styles.row}>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Dil</Text>
                <TextInput
                  style={styles.input}
                  value={lang.language}
                  onChangeText={(text) => updateItem('languages', idx, 'language', text)}
                  placeholder="Dil"
                />
              </View>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Seviye</Text>
                <View style={styles.picker}>
                  <Text style={styles.pickerText}>{lang.level || 'Intermediate'}</Text>
                </View>
              </View>
              <TouchableOpacity
                style={styles.deleteButtonSmall}
                onPress={() => removeItem('languages', idx)}
              >
                <Text style={styles.deleteButtonText}>Sil</Text>
              </TouchableOpacity>
            </View>
          </View>
        ))}
      </View>

      {/* Sertifikalar */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Sertifikalar</Text>
          <TouchableOpacity style={styles.addButton} onPress={() => addItem('certificates')}>
            <Text style={styles.addButtonText}>+ Sertifika Ekle</Text>
          </TouchableOpacity>
        </View>
        {form.certificates.map((cert, idx) => (
          <View key={idx} style={styles.itemCard}>
            <View style={styles.row}>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Sertifika Adı</Text>
                <TextInput
                  style={styles.input}
                  value={cert.name}
                  onChangeText={(text) => updateItem('certificates', idx, 'name', text)}
                  placeholder="Sertifika adı"
                />
              </View>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Veren Kurum</Text>
                <TextInput
                  style={styles.input}
                  value={cert.issuer}
                  onChangeText={(text) => updateItem('certificates', idx, 'issuer', text)}
                  placeholder="Kurum"
                />
              </View>
            </View>
            <View style={styles.row}>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Tarih</Text>
                <TextInput
                  style={styles.input}
                  value={cert.date}
                  onChangeText={(text) => updateItem('certificates', idx, 'date', text)}
                  placeholder="2023-05"
                />
              </View>
              <View style={styles.halfWidth}>
                <Text style={styles.label}>Link</Text>
                <TextInput
                  style={styles.input}
                  value={cert.url}
                  onChangeText={(text) => updateItem('certificates', idx, 'url', text)}
                  placeholder="https://..."
                />
              </View>
              <TouchableOpacity
                style={styles.deleteButtonSmall}
                onPress={() => removeItem('certificates', idx)}
              >
                <Text style={styles.deleteButtonText}>Sil</Text>
              </TouchableOpacity>
            </View>
          </View>
        ))}
      </View>

      {/* Kaydet Butonu */}
      <TouchableOpacity
        style={[styles.saveButton, saving && styles.saveButtonDisabled]}
        onPress={handleSave}
        disabled={saving}
      >
        {saving ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.saveButtonText}>💾 PROFİLİ KAYDET</Text>
        )}
      </TouchableOpacity>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#e3f2fd',
  },
  content: {
    padding: 16,
    paddingBottom: 32,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#e3f2fd',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
  },
  section: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
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
    color: '#2196F3',
    marginBottom: 16,
  },
  row: {
    flexDirection: 'row',
    marginBottom: 12,
  },
  halfWidth: {
    flex: 1,
    marginRight: 8,
  },
  thirdWidth: {
    flex: 1,
    marginRight: 8,
  },
  quarterWidth: {
    flex: 1,
    marginRight: 8,
  },
  label: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 6,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#f9f9f9',
  },
  inputDisabled: {
    backgroundColor: '#f0f0f0',
    color: '#999',
  },
  textArea: {
    minHeight: 80,
    textAlignVertical: 'top',
  },
  picker: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    backgroundColor: '#f9f9f9',
  },
  pickerText: {
    fontSize: 16,
    color: '#2c3e50',
  },
  addButton: {
    backgroundColor: '#28a745',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
  },
  addButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
  },
  itemCard: {
    backgroundColor: '#f8f9fa',
    borderRadius: 8,
    padding: 12,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#e9ecef',
  },
  deleteButton: {
    backgroundColor: '#dc3545',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
    alignSelf: 'flex-end',
    marginTop: 8,
  },
  deleteButtonSmall: {
    backgroundColor: '#dc3545',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    justifyContent: 'center',
    marginTop: 20,
  },
  deleteButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
  },
  saveButton: {
    backgroundColor: '#667eea',
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 16,
    marginBottom: 32,
  },
  saveButtonDisabled: {
    opacity: 0.6,
  },
  saveButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
});

export default ProfileScreen;
