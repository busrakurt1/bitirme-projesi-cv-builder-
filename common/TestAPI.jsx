// src/components/TestAPI.jsx
import { useState, useEffect } from 'react';
import { authAPI, userAPI, templateAPI, analysisAPI, userManager } from '../../services/api';

const TestAPI = () => {
  const [results, setResults] = useState({});
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  // Basit API test fonksiyonu
  const testAPIs = async () => {
    setLoading(true);
    setMessage('');
    const testResults = {};

    try {
      // Auth API Test
      try {
        const authResponse = await authAPI.healthCheck();
        testResults.auth = { status: '✅ BAŞARILI', data: authResponse.data };
      } catch (error) {
        testResults.auth = { status: '❌ HATA', error: error.message };
      }

      // Users API Test
      try {
        const usersResponse = await userAPI.healthCheck();
        testResults.users = { status: '✅ BAŞARILI', data: usersResponse.data };
      } catch (error) {
        testResults.users = { status: '❌ HATA', error: error.message };
      }

      // Templates API Test
      try {
        const templatesResponse = await templateAPI.healthCheck();
        testResults.templates = { status: '✅ BAŞARILI', data: templatesResponse.data };
      } catch (error) {
        testResults.templates = { status: '❌ HATA', error: error.message };
      }

      // Analysis API Test
      try {
        const analysisResponse = await analysisAPI.healthCheck();
        testResults.analysis = { status: '✅ BAŞARILI', data: analysisResponse.data };
      } catch (error) {
        testResults.analysis = { status: '❌ HATA', error: error.message };
      }

      setResults(testResults);
      setMessage('✅ API testleri tamamlandı!');

    } catch (error) {
      setMessage('❌ Test sırasında hata oluştu: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  // Kullanıcı listesini getir
  const fetchUsers = async () => {
    try {
      const response = await userAPI.getAllUsers();
      setResults(prev => ({
        ...prev,
        usersList: { status: '✅ BAŞARILI', data: response.data }
      }));
    } catch (error) {
      setResults(prev => ({
        ...prev,
        usersList: { status: '❌ HATA', error: error.message }
      }));
    }
  };

  // Template listesini getir
  const fetchTemplates = async () => {
    try {
      const response = await templateAPI.getAllTemplates();
      setResults(prev => ({
        ...prev,
        templatesList: { status: '✅ BAŞARILI', data: response.data }
      }));
    } catch (error) {
      setResults(prev => ({
        ...prev,
        templatesList: { status: '❌ HATA', error: error.message }
      }));
    }
  };

  // LocalStorage'ı temizle
  const clearStorage = () => {
    userManager.removeUser();
    localStorage.clear();
    setMessage('✅ LocalStorage temizlendi!');
    setResults({});
  };

  // Mevcut kullanıcıyı göster
  const showCurrentUser = () => {
    const user = userManager.getUser();
    if (user) {
      setResults(prev => ({
        ...prev,
        currentUser: { status: '✅ KULLANICI BULUNDU', data: user }
      }));
    } else {
      setMessage('❌ LocalStorage\'da kullanıcı bulunamadı');
    }
  };

  useEffect(() => {
    // Sayfa yüklendiğinde mevcut kullanıcıyı göster
    showCurrentUser();
  }, []);

  return (
    <div style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
      <h1>🔧 API Test Paneli</h1>
      
      {message && (
        <div style={{
          padding: '10px',
          margin: '10px 0',
          background: message.includes('✅') ? '#d4edda' : '#f8d7da',
          border: `1px solid ${message.includes('✅') ? '#c3e6cb' : '#f5c6cb'}`,
          borderRadius: '4px',
          color: message.includes('✅') ? '#155724' : '#721c24'
        }}>
          {message}
        </div>
      )}

      <div style={{ marginBottom: '20px', display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
        <button 
          onClick={testAPIs}
          disabled={loading}
          style={{ 
            padding: '10px 15px', 
            background: loading ? '#6c757d' : '#007bff', 
            color: 'white', 
            border: 'none', 
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer'
          }}
        >
          {loading ? '⏳ Test Ediliyor...' : '🧪 API Testleri Çalıştır'}
        </button>

        <button 
          onClick={fetchUsers}
          style={{ 
            padding: '10px 15px', 
            background: '#28a745', 
            color: 'white', 
            border: 'none', 
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          👥 Kullanıcıları Getir
        </button>

        <button 
          onClick={fetchTemplates}
          style={{ 
            padding: '10px 15px', 
            background: '#ffc107', 
            color: 'black', 
            border: 'none', 
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          📄 Template'leri Getir
        </button>

        <button 
          onClick={showCurrentUser}
          style={{ 
            padding: '10px 15px', 
            background: '#17a2b8', 
            color: 'white', 
            border: 'none', 
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          👤 Mevcut Kullanıcıyı Göster
        </button>

        <button 
          onClick={clearStorage}
          style={{ 
            padding: '10px 15px', 
            background: '#dc3545', 
            color: 'white', 
            border: 'none', 
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          🗑️ Storage'ı Temizle
        </button>
      </div>

      {/* Sonuçları Göster */}
      {Object.keys(results).length > 0 && (
        <div style={{ marginTop: '20px' }}>
          <h3>Test Sonuçları:</h3>
          {Object.entries(results).map(([key, result]) => (
            <div key={key} style={{ 
              marginBottom: '10px', 
              padding: '10px', 
              border: '1px solid #ddd', 
              borderRadius: '4px',
              background: result.status.includes('✅') ? '#f8f9fa' : '#fff3cd'
            }}>
              <strong>{key}:</strong> {result.status}
              {result.data && (
                <pre style={{ 
                  margin: '5px 0', 
                  padding: '5px', 
                  background: '#f8f9fa', 
                  borderRadius: '3px',
                  fontSize: '12px',
                  overflow: 'auto'
                }}>
                  {JSON.stringify(result.data, null, 2)}
                </pre>
              )}
              {result.error && (
                <div style={{ color: '#dc3545', fontSize: '14px' }}>
                  Hata: {result.error}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* API Bilgileri */}
      <div style={{ marginTop: '30px', padding: '15px', background: '#e9ecef', borderRadius: '4px' }}>
        <h4>🔗 API Endpoint'leri:</h4>
        <ul style={{ listStyle: 'none', padding: 0 }}>
          <li>✅ <strong>Auth:</strong> POST /api/auth/login, POST /api/auth/register</li>
          <li>✅ <strong>Users:</strong> GET /api/users, PUT /api/users/{'{id}'}</li>
          <li>✅ <strong>Templates:</strong> GET /api/templates</li>
          <li>✅ <strong>Analysis:</strong> POST /api/analysis/analyze-job-match</li>
          <li>✅ <strong>CV:</strong> POST /api/cv/generate</li>
          <li>✅ <strong>Skills:</strong> GET /api/skills</li>
        </ul>
      </div>
    </div>
  );
};

export default TestAPI;