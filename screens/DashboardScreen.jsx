import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useTheme } from '../contexts/ThemeContext';
import { userManager } from '../services/api';

const DashboardScreen = () => {
  const navigation = useNavigation();
  const { theme, toggleTheme } = useTheme();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadUser();
  }, []);

  const loadUser = async () => {
    try {
      const userData = await userManager.getUser();
      if (userData) {
        setUser(userData);
      } else {
        navigation.replace('Home');
      }
    } catch (error) {
      console.error('Error loading user:', error);
      navigation.replace('Home');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    Alert.alert('Çıkış Yap', 'Çıkış yapmak istediğinizden emin misiniz?', [
      { text: 'İptal', style: 'cancel' },
      {
        text: 'Çıkış Yap',
        style: 'destructive',
        onPress: async () => {
          await userManager.removeUser();
          navigation.replace('Home');
        },
      },
    ]);
  };

  if (loading) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <ActivityIndicator size="large" color="#2196F3" />
        <Text style={styles.loadingText}>Yükleniyor...</Text>
      </View>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <View
      style={[
        styles.container,
        theme === 'dark' && styles.containerDark,
      ]}
    >
      {/* Header */}
      <View
        style={[
          styles.header,
          theme === 'dark' && styles.headerDark,
        ]}
      >
        <View style={styles.headerLeft}>
          <View style={styles.logo}>
            <Text style={styles.logoText}>CV</Text>
          </View>
          <Text
            style={[
              styles.headerTitle,
              theme === 'dark' && styles.headerTitleDark,
            ]}
          >
            CV Builder
          </Text>
        </View>

        <View style={styles.headerRight}>
          <TouchableOpacity
            style={styles.iconButton}
            onPress={toggleTheme}
          >
            <Text style={styles.iconButtonText}>
              {theme === 'light' ? '🌙' : '☀️'}
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.iconButton, styles.logoutButton]}
            onPress={handleLogout}
          >
            <Text style={styles.logoutButtonText}></Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Content */}
      <ScrollView style={styles.content} contentContainerStyle={styles.contentContainer}>
        <View style={styles.welcomeCard}>
          <Text
            style={[
              styles.welcomeText,
              theme === 'dark' && styles.welcomeTextDark,
            ]}
          >
            Hoş geldiniz, {user.fullName || user.email}!
          </Text>
        </View>

        {/* Quick Actions */}
        <View style={styles.section}>
          <Text
            style={[
              styles.sectionTitle,
              theme === 'dark' && styles.sectionTitleDark,
            ]}
          >
            Hızlı İşlemler
          </Text>

          <TouchableOpacity
            style={styles.actionCard}
            onPress={() => navigation.navigate('CVBuilder')}
          >
            <Text style={styles.actionIcon}>📄</Text>
            <View style={styles.actionContent}>
              <Text style={[
                styles.actionTitle,
                theme === 'dark' && styles.actionTitleDark,
              ]}>
                CV Oluştur
              </Text>
              <Text style={[
                styles.actionDescription,
                theme === 'dark' && styles.actionDescriptionDark,
              ]}>
                Profesyonel CV'nizi oluşturun veya düzenleyin
              </Text>
            </View>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionCard}
            onPress={() => navigation.navigate('JobAnalysis')}
          >
            <Text style={styles.actionIcon}>🔍</Text>
            <View style={styles.actionContent}>
              <Text style={[
                styles.actionTitle,
                theme === 'dark' && styles.actionTitleDark,
              ]}>
                İş Analizi
              </Text>
              <Text style={[
                styles.actionDescription,
                theme === 'dark' && styles.actionDescriptionDark,
              ]}>
                İş ilanlarını analiz edin ve uygunluğunuzu öğrenin
              </Text>
            </View>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionCard}
            onPress={() => navigation.navigate('Profile')}
          >
            <Text style={styles.actionIcon}>👤</Text>
            <View style={styles.actionContent}>
              <Text style={[
                styles.actionTitle,
                theme === 'dark' && styles.actionTitleDark,
              ]}>
                Profilim
              </Text>
              <Text style={[
                styles.actionDescription,
                theme === 'dark' && styles.actionDescriptionDark,
              ]}>
                Profil bilgilerinizi görüntüleyin veya düzenleyin
              </Text>
            </View>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionCard}
            onPress={() => navigation.navigate('Chatbot')}
          >
            <Text style={styles.actionIcon}>💬</Text>
            <View style={styles.actionContent}>
              <Text style={[
                styles.actionTitle,
                theme === 'dark' && styles.actionTitleDark,
              ]}>
                Chatbot Asistanı
              </Text>
              <Text style={[
                styles.actionDescription,
                theme === 'dark' && styles.actionDescriptionDark,
              ]}>
                CV ve kariyer konularında yardım alın
              </Text>
            </View>
          </TouchableOpacity>
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
  centerContent: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  headerDark: {
    backgroundColor: '#2d3748',
    borderBottomColor: '#4a5568',
  },
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  logo: {
    marginRight: 12,
    width: 40,
    height: 40,
    borderRadius: 10,
    backgroundColor: '#667eea',
    justifyContent: 'center',
    alignItems: 'center',
  },
  logoText: {
    color: '#fff',
    fontSize: 20,
    fontWeight: 'bold',
  },
  headerTitle: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  headerTitleDark: {
    color: '#fff',
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  iconButton: {
    marginLeft: 12,
    width: 40,
    height: 40,
    borderRadius: 10,
    backgroundColor: '#f0f0f0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  iconButtonText: {
    fontSize: 18,
  },
  logoutButton: {
    backgroundColor: 'rgba(220, 53, 69, 0.1)',
    paddingHorizontal: 16,
  },
  logoutButtonText: {
    color: '#dc3545',
    fontSize: 14,
    fontWeight: 'bold',
  },
  content: {
    flex: 1,
  },
  contentContainer: {
    padding: 20,
  },
  welcomeCard: {
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 24,
    marginBottom: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  welcomeText: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  welcomeTextDark: {
    color: '#fff',
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 16,
  },
  sectionTitleDark: {
    color: '#fff',
  },
  actionCard: {
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 20,
    marginBottom: 16,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  actionIcon: {
    fontSize: 40,
    marginRight: 16,
  },
  actionContent: {
    flex: 1,
  },
  actionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 4,
  },
  actionTitleDark: {
    color: '#fff',
  },
  actionDescription: {
    fontSize: 14,
    color: '#666',
  },
  actionDescriptionDark: {
    color: '#ccc',
  },
});

export default DashboardScreen;

