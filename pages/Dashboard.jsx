import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import './Dashboard.css';

const Dashboard = () => {
  const [user, setUser] = useState(null);
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();

  useEffect(() => {
    const userData = localStorage.getItem('user');
    if (userData) {
      setUser(JSON.parse(userData));
    } else {
      navigate('/');
    }
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem('user');
    navigate('/');
  };

  if (!user) {
    return (
      <div className={`dashboard-container theme-${theme}`} style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: theme === 'light' 
          ? '#ffffff'
          : '#1a202c',
      }}>
        <div style={{
          color: theme === 'light' ? '#2c3e50' : '#ffffff',
          fontSize: '24px',
          fontWeight: 600,
        }}>
          Yükleniyor...
        </div>
      </div>
    );
  }

  return (
    <div className={`dashboard-container theme-${theme}`} style={{
      minHeight: '100vh',
      width: '100%',
      background: theme === 'light' 
        ? '#ffffff'
        : '#1a202c',
      padding: 0,
      margin: 0
    }}>
      {/* Navigation Bar */}
      <nav className={`dashboard-nav theme-${theme}`} style={{
        background: theme === 'light'
          ? '#ffffff'
          : 'rgba(15, 32, 39, 0.95)',
        padding: '0 40px',
        height: 72,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        boxShadow: theme === 'light'
          ? '0 2px 12px rgba(0, 0, 0, 0.08)'
          : '0 2px 12px rgba(0, 0, 0, 0.3)',
        position: 'sticky',
        top: 0,
        zIndex: 100,
        borderBottom: theme === 'light' 
          ? '1px solid rgba(0, 0, 0, 0.06)'
          : '1px solid rgba(255, 255, 255, 0.1)'
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 16
        }}>
          <div 
            onClick={() => navigate('/dashboard')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              cursor: 'pointer'
            }}
          >
            <div style={{
              width: 40,
              height: 40,
              borderRadius: 10,
              background: '#00BCD4',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 20,
              fontWeight: 'bold',
              color: '#ffffff',
              flexShrink: 0
            }}>
              CV
            </div>
            <span style={{
              fontWeight: 700,
              fontSize: 22,
              color: theme === 'light' ? '#00838F' : '#ffffff',
              letterSpacing: '-0.5px',
              whiteSpace: 'nowrap'
            }}>
              CV Builder
            </span>
          </div>
        </div>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12
        }}>
          <button
            onClick={() => navigate('/profile')}
            style={{
              padding: '10px 20px',
              background: theme === 'light' 
                ? 'rgba(0, 0, 0, 0.05)' 
                : 'rgba(255, 255, 255, 0.1)',
              border: `1px solid ${theme === 'light' ? 'rgba(0, 0, 0, 0.1)' : 'rgba(255, 255, 255, 0.2)'}`,
              borderRadius: 10,
              color: theme === 'light' ? '#2c3e50' : '#ffffff',
              fontWeight: 600,
              cursor: 'pointer',
              fontSize: 14
            }}
          >
            Profilim
          </button>
          <button
            onClick={toggleTheme}
            style={{
              background: theme === 'light' 
                ? 'rgba(0, 0, 0, 0.05)' 
                : 'rgba(255, 255, 255, 0.1)',
              border: `1px solid ${theme === 'light' ? 'rgba(0, 0, 0, 0.1)' : 'rgba(255, 255, 255, 0.2)'}`,
              borderRadius: 10,
              width: '40px',
              height: '40px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              fontSize: '18px',
            }}
          >
            {theme === 'light' ? '🌙' : '☀️'}
          </button>
          <button
            onClick={handleLogout}
            style={{
              padding: '10px 20px',
              background: 'rgba(220, 53, 69, 0.1)',
              border: '1px solid rgba(220, 53, 69, 0.3)',
              borderRadius: 10,
              color: '#dc3545',
              fontWeight: 600,
              cursor: 'pointer',
              fontSize: 14
            }}
          >
            Çıkış
          </button>
        </div>
      </nav>

      {/* Main Content */}
      <div style={{
        maxWidth: 1200,
        margin: '0 auto',
        padding: '48px 32px',
        position: 'relative',
        zIndex: 1
      }}>
        {/* Welcome Section */}
        <div style={{
          marginBottom: 48
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: 16,
            flexWrap: 'wrap',
            gap: 16
          }}>
            <div>
              <h1 style={{
                fontSize: 'clamp(2rem, 4vw, 2.75rem)',
                fontWeight: 800,
                color: theme === 'light' ? '#00838F' : '#ffffff',
                marginBottom: 8,
                lineHeight: 1.2,
                letterSpacing: '-0.5px'
              }}>
                Hoş Geldiniz, {user.fullName}! 👋
              </h1>
              <p style={{
                fontSize: 'clamp(15px, 1.8vw, 18px)',
                color: theme === 'light' ? '#2c3e50' : 'rgba(255, 255, 255, 0.8)',
                fontWeight: 400,
                lineHeight: 1.6
              }}>
                CV'nizi oluşturmaya başlamak için profilinizi tamamlayın ve ardından AI destekli CV oluşturucuyu kullanın.
              </p>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div style={{
          marginTop: 48
        }}>
          <h2 style={{
            fontSize: 20,
            fontWeight: 700,
            color: theme === 'light' ? '#2c3e50' : '#ffffff',
            marginBottom: 20,
            letterSpacing: '-0.3px'
          }}>
            Hızlı İşlemler
          </h2>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
            gap: 24
          }}>
            {/* İş Analizi Kartı */}
            <button
              onClick={() => navigate('/job-analysis')}
              style={{
                background: theme === 'light' 
                  ? '#ffffff' 
                  : 'rgba(255, 255, 255, 0.05)',
                border: theme === 'light'
                  ? '1px solid rgba(0, 0, 0, 0.1)'
                  : '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: 16,
                padding: '32px',
                boxShadow: theme === 'light'
                  ? '0 4px 12px rgba(0, 0, 0, 0.08)'
                  : '0 4px 12px rgba(0, 0, 0, 0.2)',
                cursor: 'pointer',
                textAlign: 'left',
                color: theme === 'light' ? '#00838F' : '#ffffff',
                position: 'relative',
                overflow: 'hidden'
              }}
            >
              <div style={{
                width: 56,
                height: 56,
                borderRadius: 14,
                background: '#00BCD4',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 28,
                marginBottom: 20
              }}>
                📊
              </div>
              <h3 style={{
                fontSize: 22,
                fontWeight: 700,
                marginBottom: 8,
                color: theme === 'light' ? '#2c3e50' : '#ffffff'
              }}>
                İş Analizi
              </h3>
              <p style={{
                fontSize: 15,
                color: theme === 'light' ? '#2c3e50' : 'rgba(255, 255, 255, 0.7)',
                marginBottom: 16,
                lineHeight: 1.6
              }}>
                İş ilanlarını analiz edin, gerekli becerileri keşfedin ve kariyer hedeflerinize uygun pozisyonları bulun.
              </p>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                color: '#00ACC1',
                fontWeight: 600,
                fontSize: 14
              }}>
                Analiz Yapmaya Başla
                <span>→</span>
              </div>
            </button>

            {/* Profil Kartı */}
            <button
              onClick={() => navigate('/profile')}
              style={{
                background: theme === 'light' 
                  ? '#ffffff' 
                  : 'rgba(255, 255, 255, 0.05)',
                border: theme === 'light'
                  ? '1px solid rgba(0, 0, 0, 0.1)'
                  : '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: 16,
                padding: '32px',
                boxShadow: theme === 'light'
                  ? '0 4px 12px rgba(0, 0, 0, 0.08)'
                  : '0 4px 12px rgba(0, 0, 0, 0.2)',
                cursor: 'pointer',
                textAlign: 'left',
                color: theme === 'light' ? '#00838F' : '#ffffff',
                position: 'relative',
                overflow: 'hidden'
              }}
            >
              <div style={{
                width: 56,
                height: 56,
                borderRadius: 14,
                background: '#00BCD4',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 28,
                marginBottom: 20
              }}>
                👤
              </div>
              <h3 style={{
                fontSize: 22,
                fontWeight: 700,
                marginBottom: 8,
                color: theme === 'light' ? '#2c3e50' : '#ffffff'
              }}>
                Profil
              </h3>
              <p style={{
                fontSize: 15,
                color: theme === 'light' ? '#2c3e50' : 'rgba(255, 255, 255, 0.7)',
                marginBottom: 16,
                lineHeight: 1.6
              }}>
                Kişisel bilgilerinizi, eğitim geçmişinizi ve iş deneyimlerinizi ekleyerek profilinizi tamamlayın.
              </p>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                color: '#00ACC1',
                fontWeight: 600,
                fontSize: 14
              }}>
                Profili Düzenle
                <span>→</span>
              </div>
            </button>

            {/* CV Oluşturma Kartı */}
            <button
              onClick={() => navigate('/cv-builder')}
              style={{
                background: theme === 'light' 
                  ? '#ffffff' 
                  : 'rgba(255, 255, 255, 0.05)',
                border: theme === 'light'
                  ? '1px solid rgba(0, 0, 0, 0.1)'
                  : '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: 16,
                padding: '32px',
                boxShadow: theme === 'light'
                  ? '0 4px 12px rgba(0, 0, 0, 0.08)'
                  : '0 4px 12px rgba(0, 0, 0, 0.2)',
                cursor: 'pointer',
                textAlign: 'left',
                color: theme === 'light' ? '#00838F' : '#ffffff',
                position: 'relative',
                overflow: 'hidden'
              }}
            >
              <div style={{
                width: 56,
                height: 56,
                borderRadius: 14,
                background: '#00BCD4',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 28,
                marginBottom: 20
              }}>
                📄
              </div>
              <h3 style={{
                fontSize: 22,
                fontWeight: 700,
                marginBottom: 8,
                color: theme === 'light' ? '#2c3e50' : '#ffffff'
              }}>
                CV Oluşturma
              </h3>
              <p style={{
                fontSize: 15,
                color: theme === 'light' ? '#2c3e50' : 'rgba(255, 255, 255, 0.7)',
                marginBottom: 16,
                lineHeight: 1.6
              }}>
                AI destekli CV oluşturucu ile profesyonel CV'nizi hazırlayın ve kariyerinize yön verin.
              </p>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                color: '#00ACC1',
                fontWeight: 600,
                fontSize: 14
              }}>
                CV Oluşturmaya Başla
                <span>→</span>
              </div>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
