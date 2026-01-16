// src/pages/Home.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Login from '../components/auth/Login';
import Register from '../components/auth/Register';
import TestAPI from '../components/common/TestAPI';
import { useTheme } from '../contexts/ThemeContext';
import './Home.css';

const Home = () => {
  const [currentView, setCurrentView] = useState('home');
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();

  return (
    <div className={`home-container theme-${theme}`} style={{
      minHeight: '100vh',
      width: '100vw',
      margin: 0,
      padding: 0,
      overflow: 'hidden',
      background: theme === 'light' 
        ? '#ffffff'
        : 'linear-gradient(135deg, #0d47a1 0%, #1565c0 20%, #1976d2 40%, #1e88e5 60%, #2196f3 80%, #42a5f5 100%)',
      display: 'flex',
      flexDirection: 'column',
      position: 'relative'
    }}>
      {/* Animated Background Elements */}
      <div className="animated-bg-elements">
        <div className="bg-circle bg-circle-1"></div>
        <div className="bg-circle bg-circle-3"></div>
        <div className="bg-gradient-layer bg-gradient-layer-1"></div>
        <div className="bg-gradient-layer bg-gradient-layer-2"></div>
      </div>

      {/* Navigation */}
      <nav className={`navbar theme-${theme}`}
        style={{
          background: theme === 'light'
            ? '#ffffff'
            : 'linear-gradient(135deg, rgba(13, 71, 161, 0.95) 0%, rgba(21, 101, 192, 0.9) 25%, rgba(25, 118, 210, 0.9) 50%, rgba(30, 136, 229, 0.9) 75%, rgba(33, 150, 243, 0.95) 100%)',
          backdropFilter: 'blur(20px)',
          borderBottom: 'none',
          padding: '0 32px', 
          height: 80, 
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          zIndex: 100,
          width: '100%',
          boxShadow: 'none',
          animation: 'slideDownNav 0.6s ease-out'
        }}
      >
        <div
          className="nav-logo"
          style={{
            fontWeight: 700,
            fontSize: 32,
            letterSpacing: '.5px',
            display: 'flex',
            alignItems: 'center',
            background: 'linear-gradient(135deg, #1565c0 0%, #1976d2 50%, #2196f3 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
            cursor: 'pointer',
            animation: 'fadeInLeft 0.8s ease-out 0.2s both',
            transition: 'all 0.3s ease',
            position: 'relative'
          }}
          onClick={() => setCurrentView('home')}
            onMouseEnter={(e) => {
            e.currentTarget.style.transform = 'scale(1.05)';
              e.currentTarget.style.animation = 'fadeInLeft 0.8s ease-out 0.2s both';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.transform = 'scale(1)';
              e.currentTarget.style.animation = 'fadeInLeft 0.8s ease-out 0.2s both';
          }}
        >
          <div 
            className="logo-icon"
            style={{
              width: 40, 
              height: 40, 
              marginRight: 10,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              position: 'relative',
              animation: 'logoSpin 0.8s ease-out 0.4s both, iconPulse 3s ease-in-out infinite 2s',
              transition: 'transform 0.3s ease',
              filter: 'drop-shadow(0 2px 8px rgba(0, 188, 212, 0.3))'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'rotate(15deg) scale(1.15)';
              e.currentTarget.style.filter = 'drop-shadow(0 4px 12px rgba(0, 188, 212, 0.5))';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'rotate(0deg) scale(1)';
              e.currentTarget.style.filter = 'drop-shadow(0 2px 8px rgba(0, 188, 212, 0.3))';
            }}
          >
            <svg 
              width="40" 
              height="40" 
              viewBox="0 0 40 40" 
              fill="none" 
              xmlns="http://www.w3.org/2000/svg"
              style={{
                width: '100%',
                height: '100%'
              }}
            >
              {/* Document Background */}
              <rect 
                x="8" 
                y="6" 
                width="24" 
                height="28" 
                rx="2" 
                fill={theme === 'light' ? '#00BCD4' : '#64b5f6'}
                opacity="0.9"
              />
              {/* Document Fold Corner */}
              <path 
                d="M24 6 L32 6 L32 14 L24 6 Z" 
                fill={theme === 'light' ? '#00ACC1' : '#42a5f5'}
                opacity="0.8"
              />
              {/* Lines on Document */}
              <line 
                x1="12" 
                y1="12" 
                x2="28" 
                y2="12" 
                stroke={theme === 'light' ? '#ffffff' : '#e3f2fd'}
                strokeWidth="1.5"
                strokeLinecap="round"
              />
              <line 
                x1="12" 
                y1="16" 
                x2="28" 
                y2="16" 
                stroke={theme === 'light' ? '#ffffff' : '#e3f2fd'}
                strokeWidth="1.5"
                strokeLinecap="round"
              />
              <line 
                x1="12" 
                y1="20" 
                x2="24" 
                y2="20" 
                stroke={theme === 'light' ? '#ffffff' : '#e3f2fd'}
                strokeWidth="1.5"
                strokeLinecap="round"
              />
              {/* AI Sparkle */}
              <circle 
                cx="28" 
                cy="10" 
                r="3" 
                fill={theme === 'light' ? '#ffd700' : '#ffeb3b'}
                opacity="0.9"
              />
              <path 
                d="M28 7 L28 13 M25 10 L31 10" 
                stroke={theme === 'light' ? '#ffffff' : '#ffffff'}
                strokeWidth="1"
                strokeLinecap="round"
              />
            </svg>
          </div>
          <span style={{
            background: theme === 'light'
              ? 'linear-gradient(135deg, #00838F 0%, #00ACC1 50%, #00BCD4 100%)'
              : 'linear-gradient(135deg, #00838F 0%, #00ACC1 50%, #00BCD4 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
            fontSize: 28,
            fontWeight: 800,
            letterSpacing: '-0.5px'
          }}>
           {/* CV Builder */}
          </span>
        </div>
        <div className="nav-links" style={{display: 'flex', gap: '24px', alignItems: 'center', flexWrap: 'nowrap'}}>
          <button
            className={`nav-button${currentView==='home' ? ' nav-active' : ''}`}
            onClick={() => setCurrentView('home')}
            style={{
              animation: 'fadeInRight 0.8s ease-out 0.3s both'
            }}
          >
            Ana Sayfa
          </button>
          <button
            className={`nav-button${currentView==='login' ? ' nav-active' : ''}`}
            onClick={() => setCurrentView('login')}
            style={{
              animation: 'fadeInRight 0.8s ease-out 0.4s both'
            }}
          >
            Giriş Yap
          </button>
          <button
            className={`nav-button register-btn${currentView==='register' ? ' nav-active' : ''}`}
            style={{
              background: 'linear-gradient(135deg, #00ACC1 0%, #00BCD4 50%, #4DD0E1 100%)',
              color: '#fff',
              fontWeight: 600,
              borderRadius: 12,
              padding: '10px 24px',
              boxShadow: '0 4px 15px rgba(0, 188, 212, 0.25)',
              animation: 'fadeInRight 0.8s ease-out 0.5s both',
              transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
            }}
            onClick={() => setCurrentView('register')}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'translateY(-2px) scale(1.05)';
              e.currentTarget.style.boxShadow = '0 6px 20px rgba(0, 188, 212, 0.35)';
              e.currentTarget.style.background = 'linear-gradient(135deg, #00BCD4 0%, #4DD0E1 50%, #80DEEA 100%)';
              e.currentTarget.style.animation = 'fadeInRight 0.8s ease-out 0.5s both';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'translateY(0) scale(1)';
              e.currentTarget.style.boxShadow = '0 4px 15px rgba(0, 188, 212, 0.25)';
              e.currentTarget.style.background = 'linear-gradient(135deg, #00ACC1 0%, #00BCD4 50%, #4DD0E1 100%)';
              e.currentTarget.style.animation = 'fadeInRight 0.8s ease-out 0.5s both';
            }}
          >
            Kayıt Ol
          </button>
          <button
            onClick={toggleTheme}
            className="theme-toggle-btn"
            style={{
              background: theme === 'light' 
                ? 'rgba(0, 188, 212, 0.15)' 
                : 'rgba(66, 165, 245, 0.25)',
              border: `2px solid ${theme === 'light' ? 'rgba(0, 188, 212, 0.3)' : 'rgba(66, 165, 245, 0.4)'}`,
              borderRadius: '50%',
              width: '44px',
              height: '44px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              fontSize: '20px',
              transition: 'all 0.3s ease',
              animation: 'fadeInRight 0.8s ease-out 0.6s both',
              boxShadow: '0 4px 12px rgba(0, 188, 212, 0.2)'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'scale(1.1) rotate(15deg)';
              e.currentTarget.style.boxShadow = '0 6px 20px rgba(0, 188, 212, 0.3)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'scale(1) rotate(0deg)';
              e.currentTarget.style.boxShadow = '0 4px 12px rgba(0, 188, 212, 0.2)';
            }}
            title={theme === 'light' ? 'Koyu Temaya Geç' : 'Açık Temaya Geç'}
          >
            {theme === 'light' ? '🌙' : '☀️'}
          </button>
        </div>
      </nav>

      {/* Main Content */}
      <div className="main-section" style={{
        flex: 1,
        display: 'flex',
        alignItems: 'stretch',
        justifyContent: 'stretch',
        width: '100%',
        padding: 0,
        margin: 0,
        position: 'relative',
        zIndex: 1,
        minHeight: 'calc(100vh - 80px)',
        background: 'transparent'
      }}>
        {currentView === 'home' && (
          <div className="hero-full"
            style={{
              width: '100%',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 0,
              margin: 0,
              minHeight: 'calc(100vh - 80px)'
            }}
          >
              {/* Hero Content Card - Full Width */}
            <div style={{
              width: '100%',
              background: 'transparent',
              backdropFilter: 'none',
              padding: '80px 32px 100px 32px',
              boxShadow: 'none',
              border: 'none',
              textAlign: 'center',
              minHeight: 'calc(100vh - 80px)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'flex-start',
              borderRadius: 0,
              position: 'relative',
              overflow: 'hidden'
            }}>
              {/* Features Grid - Üstte */}
              <div className="features-grid" style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                gap: 28,
                marginTop: 0,
                marginBottom: 80,
                paddingBottom: 0,
                borderBottom: 'none',
                maxWidth: 1200,
                width: '100%',
                animation: 'fadeInUp 0.8s ease-out'
              }}>
                {[
                  { 
                    icon: (
                      <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                        {/* Robot Icon */}
                        <circle cx="32" cy="32" r="28" fill="url(#robotGrad1)" opacity="0.15"/>
                        {/* Robot Head */}
                        <rect x="18" y="14" width="28" height="24" rx="4" fill="url(#robotGrad2)" stroke="url(#robotGrad3)" strokeWidth="2"/>
                        {/* Robot Eyes */}
                        <circle cx="26" cy="24" r="3" fill="url(#robotGrad4)"/>
                        <circle cx="38" cy="24" r="3" fill="url(#robotGrad4)"/>
                        {/* Robot Antenna */}
                        <circle cx="32" cy="14" r="2" fill="url(#robotGrad5)"/>
                        <line x1="32" y1="14" x2="32" y2="10" stroke="url(#robotGrad6)" strokeWidth="2" strokeLinecap="round"/>
                        {/* Robot Body */}
                        <rect x="20" y="38" width="24" height="18" rx="3" fill="url(#robotGrad2)" stroke="url(#robotGrad3)" strokeWidth="2"/>
                        {/* Robot Chest Panel */}
                        <rect x="26" y="42" width="12" height="8" rx="2" fill="url(#robotGrad7)" opacity="0.6"/>
                        {/* Robot Arms */}
                        <rect x="10" y="38" width="8" height="14" rx="2" fill="url(#robotGrad2)" stroke="url(#robotGrad3)" strokeWidth="2"/>
                        <rect x="46" y="38" width="8" height="14" rx="2" fill="url(#robotGrad2)" stroke="url(#robotGrad3)" strokeWidth="2"/>
                        {/* Robot Legs */}
                        <rect x="22" y="56" width="6" height="6" rx="1" fill="url(#robotGrad2)" stroke="url(#robotGrad3)" strokeWidth="1.5"/>
                        <rect x="36" y="56" width="6" height="6" rx="1" fill="url(#robotGrad2)" stroke="url(#robotGrad3)" strokeWidth="1.5"/>
                        <defs>
                          <linearGradient id="robotGrad1" x1="4" y1="4" x2="60" y2="60" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00BCD4"/>
                            <stop offset="1" stopColor="#4DD0E1"/>
                          </linearGradient>
                          <linearGradient id="robotGrad2" x1="10" y1="14" x2="54" y2="62" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#26C6DA"/>
                            <stop offset="1" stopColor="#80DEEA"/>
                          </linearGradient>
                          <linearGradient id="robotGrad3" x1="10" y1="14" x2="54" y2="62" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00ACC1"/>
                            <stop offset="1" stopColor="#00BCD4"/>
                          </linearGradient>
                          <linearGradient id="robotGrad4" x1="23" y1="21" x2="29" y2="27" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00838F"/>
                            <stop offset="1" stopColor="#00BCD4"/>
                          </linearGradient>
                          <linearGradient id="robotGrad5" x1="30" y1="12" x2="34" y2="16" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#FFD700"/>
                            <stop offset="1" stopColor="#FFA500"/>
                          </linearGradient>
                          <linearGradient id="robotGrad6" x1="32" y1="10" x2="32" y2="14" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#FFC107"/>
                            <stop offset="1" stopColor="#FF9800"/>
                          </linearGradient>
                          <linearGradient id="robotGrad7" x1="26" y1="42" x2="38" y2="50" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00838F"/>
                            <stop offset="1" stopColor="#26C6DA"/>
                          </linearGradient>
                        </defs>
                      </svg>
                    ),
                    title: 'AI Profil Analizi', 
                    shortDesc: 'Deneyim, eğitim ve becerilerinizi AI ile analiz ederek CV\'nizi otomatik oluşturuyoruz',
                    gradient: theme === 'light' 
                      ? 'linear-gradient(135deg, rgba(178, 235, 242, 0.6) 0%, rgba(128, 222, 234, 0.4) 100%)'
                      : 'linear-gradient(135deg, rgba(154, 226, 237, 0.4) 0%, rgba(25, 118, 210, 0.4) 100%)',
                    gradientHover: theme === 'light'
                      ? 'linear-gradient(135deg, rgba(128, 222, 234, 0.7) 0%, rgba(77, 208, 225, 0.6) 100%)'
                      : 'linear-gradient(135deg, rgba(0, 23, 49, 0.6) 0%, rgba(25, 118, 210, 0.6) 100%)'
                  },
                  { 
                    icon: (
                      <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                        {/* Target/Aim icon for optimization */}
                        <circle cx="32" cy="32" r="26" fill="url(#targetGrad1)" opacity="0.15"/>
                        {/* Outer circles */}
                        <circle cx="32" cy="32" r="24" stroke="url(#targetGrad2)" strokeWidth="2" opacity="0.6"/>
                        <circle cx="32" cy="32" r="18" stroke="url(#targetGrad3)" strokeWidth="2" opacity="0.7"/>
                        <circle cx="32" cy="32" r="12" stroke="url(#targetGrad4)" strokeWidth="2"/>
                        {/* Center dot */}
                        <circle cx="32" cy="32" r="4" fill="url(#targetGrad5)"/>
                        {/* Arrow pointing to center */}
                        <path d="M32 8L36 16L32 14L28 16L32 8Z" fill="url(#targetGrad6)"/>
                        <path d="M32 56L36 48L32 50L28 48L32 56Z" fill="url(#targetGrad6)"/>
                        <path d="M8 32L16 28L14 32L16 36L8 32Z" fill="url(#targetGrad6)"/>
                        <path d="M56 32L48 28L50 32L48 36L56 32Z" fill="url(#targetGrad6)"/>
                        <defs>
                          <linearGradient id="targetGrad1" x1="6" y1="6" x2="58" y2="58" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00BCD4"/>
                            <stop offset="1" stopColor="#4DD0E1"/>
                          </linearGradient>
                          <linearGradient id="targetGrad2" x1="8" y1="8" x2="56" y2="56" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#26C6DA"/>
                            <stop offset="1" stopColor="#80DEEA"/>
                          </linearGradient>
                          <linearGradient id="targetGrad3" x1="14" y1="14" x2="50" y2="50" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00ACC1"/>
                            <stop offset="1" stopColor="#26C6DA"/>
                          </linearGradient>
                          <linearGradient id="targetGrad4" x1="20" y1="20" x2="44" y2="44" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00838F"/>
                            <stop offset="1" stopColor="#00BCD4"/>
                          </linearGradient>
                          <linearGradient id="targetGrad5" x1="28" y1="28" x2="36" y2="36" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#FFFFFF"/>
                            <stop offset="1" stopColor="#E0F7FA"/>
                          </linearGradient>
                          <linearGradient id="targetGrad6" x1="28" y1="8" x2="36" y2="16" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00ACC1"/>
                            <stop offset="1" stopColor="#00BCD4"/>
                          </linearGradient>
                        </defs>
                      </svg>
                    ),
                    title: 'İş İlanı Optimizasyonu', 
                    shortDesc: 'Hedef iş ilanınızı analiz ederek CV\'nizi ATS uyumlu anahtar kelimelerle optimize ediyoruz',
                    gradient: theme === 'light'
                      ? 'linear-gradient(135deg, rgba(178, 235, 242, 0.6) 0%, rgba(77, 208, 225, 0.4) 100%)'
                      : 'linear-gradient(135deg, rgba(25, 118, 210, 0.4) 0%, rgba(30, 136, 229, 0.4) 100%)',
                    gradientHover: theme === 'light'
                      ? 'linear-gradient(135deg, rgba(128, 222, 234, 0.7) 0%, rgba(38, 198, 218, 0.6) 100%)'
                      : 'linear-gradient(135deg, rgba(19, 37, 103, 0.6) 0%, rgba(30, 136, 229, 0.6) 100%)'
                  },
                  { 
                    icon: (
                      <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                        {/* List/Sorting icon for prioritization */}
                        <circle cx="32" cy="32" r="28" fill="url(#listGrad1)" opacity="0.15"/>
                        {/* List items with numbers */}
                        <rect x="16" y="16" width="32" height="8" rx="2" fill="url(#listGrad2)" stroke="url(#listGrad3)" strokeWidth="1.5"/>
                        <circle cx="22" cy="20" r="2" fill="url(#listGrad4)"/>
                        <rect x="16" y="28" width="32" height="8" rx="2" fill="url(#listGrad2)" stroke="url(#listGrad3)" strokeWidth="1.5" opacity="0.8"/>
                        <circle cx="22" cy="32" r="2" fill="url(#listGrad4)"/>
                        <rect x="16" y="40" width="32" height="8" rx="2" fill="url(#listGrad2)" stroke="url(#listGrad3)" strokeWidth="1.5" opacity="0.6"/>
                        <circle cx="22" cy="44" r="2" fill="url(#listGrad4)"/>
                        {/* Arrow indicators showing priority */}
                        <path d="M42 20L46 24L42 28" stroke="url(#listGrad5)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
                        <path d="M42 32L46 36L42 40" stroke="url(#listGrad5)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" fill="none" opacity="0.7"/>
                        <path d="M42 44L46 48L42 52" stroke="url(#listGrad5)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" fill="none" opacity="0.5"/>
                        {/* Highlight on first item */}
                        <rect x="16" y="16" width="32" height="8" rx="2" fill="url(#listGrad6)" opacity="0.2"/>
                        <defs>
                          <linearGradient id="listGrad1" x1="4" y1="4" x2="60" y2="60" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00BCD4"/>
                            <stop offset="1" stopColor="#4DD0E1"/>
                          </linearGradient>
                          <linearGradient id="listGrad2" x1="16" y1="16" x2="48" y2="24" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#26C6DA"/>
                            <stop offset="1" stopColor="#80DEEA"/>
                          </linearGradient>
                          <linearGradient id="listGrad3" x1="16" y1="16" x2="48" y2="24" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00ACC1"/>
                            <stop offset="1" stopColor="#00BCD4"/>
                          </linearGradient>
                          <linearGradient id="listGrad4" x1="20" y1="18" x2="24" y2="22" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00838F"/>
                            <stop offset="1" stopColor="#00BCD4"/>
                          </linearGradient>
                          <linearGradient id="listGrad5" x1="42" y1="20" x2="46" y2="52" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#4CAF50"/>
                            <stop offset="1" stopColor="#66BB6A"/>
                          </linearGradient>
                          <linearGradient id="listGrad6" x1="16" y1="16" x2="48" y2="24" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#FFD700"/>
                            <stop offset="1" stopColor="#FFA500"/>
                          </linearGradient>
                        </defs>
                      </svg>
                    ),
                    title: 'İçerik Önceliklendirme', 
                    shortDesc: 'En önemli deneyim ve becerilerinizi öne çıkararak işe alım uzmanlarının dikkatini çekiyoruz',
                    gradient: theme === 'light'
                      ? 'linear-gradient(135deg, rgba(178, 235, 242, 0.6) 0%, rgba(38, 198, 218, 0.4) 100%)'
                      : 'linear-gradient(135deg, rgba(30, 136, 229, 0.4) 0%, rgba(33, 150, 243, 0.4) 100%)',
                    gradientHover: theme === 'light'
                      ? 'linear-gradient(135deg, rgba(128, 222, 234, 0.7) 0%, rgba(0, 188, 212, 0.6) 100%)'
                      : 'linear-gradient(135deg, rgba(19, 37, 103, 0.6) 0%, rgba(33, 150, 243, 0.6) 100%)'
                  },
                  { 
                    icon: (
                      <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <rect x="14" y="10" width="36" height="44" rx="3" fill="url(#pdfGradient1)" opacity="0.2"/>
                        <path d="M18 18H46M18 24H46M18 30H40M18 36H46M18 42H40" stroke="url(#pdfGradient2)" strokeWidth="2" strokeLinecap="round"/>
                        <path d="M50 14L50 50L14 50L14 14L50 14Z" stroke="url(#pdfGradient3)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                        <path d="M50 14L50 20L44 14L50 14Z" fill="url(#pdfGradient4)"/>
                        <circle cx="48" cy="46" r="8" fill="url(#pdfGradient5)"/>
                        <path d="M44 46L48 50L52 46" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                        <defs>
                          <linearGradient id="pdfGradient1" x1="14" y1="10" x2="50" y2="54" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00BCD4"/>
                            <stop offset="1" stopColor="#4DD0E1"/>
                          </linearGradient>
                          <linearGradient id="pdfGradient2" x1="18" y1="18" x2="46" y2="42" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00ACC1"/>
                            <stop offset="1" stopColor="#00BCD4"/>
                          </linearGradient>
                          <linearGradient id="pdfGradient3" x1="14" y1="14" x2="50" y2="50" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00838F"/>
                            <stop offset="1" stopColor="#00BCD4"/>
                          </linearGradient>
                          <linearGradient id="pdfGradient4" x1="44" y1="14" x2="50" y2="20" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#00ACC1"/>
                            <stop offset="1" stopColor="#00BCD4"/>
                          </linearGradient>
                          <linearGradient id="pdfGradient5" x1="40" y1="38" x2="56" y2="54" gradientUnits="userSpaceOnUse">
                            <stop stopColor="#4CAF50"/>
                            <stop offset="1" stopColor="#66BB6A"/>
                          </linearGradient>
                        </defs>
                      </svg>
                    ),
                    title: 'Anında PDF İndirme', 
                    shortDesc: 'Hazır CV\'nizi profesyonel PDF formatında indirip başvurularınızda kullanabilirsiniz',
                    gradient: theme === 'light'
                      ? 'linear-gradient(135deg, rgba(178, 235, 242, 0.6) 0%, rgba(0, 188, 212, 0.4) 100%)'
                      : 'linear-gradient(135deg, rgba(33, 150, 243, 0.4) 0%, rgba(66, 165, 245, 0.4) 100%)',
                    gradientHover: theme === 'light'
                      ? 'linear-gradient(135deg, rgba(128, 222, 234, 0.7) 0%, rgba(0, 172, 193, 0.6) 100%)'
                      : 'linear-gradient(135deg, rgba(19, 37, 103, 0.6) 0%, rgba(66, 165, 245, 0.6) 100%)'
                  }
                ].map((f, i) => (
                  <div 
                    key={i} 
                    className="feature-card"
                    data-gradient={f.gradient}
                    data-gradient-hover={f.gradientHover}
                    style={{
                      background: f.gradient,
                      backdropFilter: 'blur(20px)',
                      borderRadius: 28,
                      padding: '40px 32px',
                      textAlign: 'center',
                      border: theme === 'light' 
                        ? '2px solid rgba(255, 255, 255, 0.5)' 
                        : '2px solid rgba(66, 165, 245, 0.5)',
                      transition: 'all 0.5s cubic-bezier(0.4, 0, 0.2, 1)',
                      cursor: 'pointer',
                      position: 'relative',
                      overflow: 'hidden',
                      minHeight: '220px',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      animation: `fadeInUp 0.8s ease-out ${0.1 * i}s both, softGradientShift 12s ease infinite ${i * 0.5}s`,
                      boxShadow: theme === 'light'
                        ? '0 10px 40px rgba(0, 188, 212, 0.15)'
                        : '0 10px 40px rgba(13, 71, 161, 0.25)'
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.transform = 'translateY(-8px) scale(1.05)';
                      e.currentTarget.style.boxShadow = theme === 'light'
                        ? '0 16px 48px rgba(0, 188, 212, 0.2)'
                        : '0 16px 48px rgba(21, 101, 192, 0.3)';
                      e.currentTarget.style.background = f.gradientHover;
                      e.currentTarget.style.borderColor = theme === 'light'
                        ? 'rgba(255, 255, 255, 0.6)'
                        : 'rgba(66, 165, 245, 0.6)';
                      e.currentTarget.style.animation = `fadeInUp 0.8s ease-out ${0.1 * i}s both`;
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.transform = 'translateY(0) scale(1)';
                      e.currentTarget.style.boxShadow = theme === 'light'
                        ? '0 8px 32px rgba(0, 188, 212, 0.12)'
                        : '0 8px 32px rgba(13, 71, 161, 0.2)';
                      e.currentTarget.style.background = f.gradient;
                      e.currentTarget.style.borderColor = theme === 'light'
                        ? 'rgba(255, 255, 255, 0.4)'
                        : 'rgba(66, 165, 245, 0.4)';
                      e.currentTarget.style.animation = `fadeInUp 0.8s ease-out ${0.1 * i}s both`;
                    }}
                  >
                    <div>
                      <div style={{ 
                        width: 64,
                        height: 64,
                        marginBottom: 24, 
                        filter: 'drop-shadow(0 6px 20px rgba(0, 188, 212, 0.3))', 
                        lineHeight: 1,
                        animation: `iconFloat 3s ease-in-out infinite ${i * 0.3}s`,
                        transform: 'translateY(0)',
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        margin: '0 auto 24px auto',
                        transition: 'all 0.3s ease'
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.transform = 'translateY(-5px) scale(1.1)';
                        e.currentTarget.style.filter = 'drop-shadow(0 8px 25px rgba(0, 188, 212, 0.5))';
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.transform = 'translateY(0) scale(1)';
                        e.currentTarget.style.filter = 'drop-shadow(0 6px 20px rgba(0, 188, 212, 0.3))';
                      }}>
                        {f.icon}
                      </div>
                      <div style={{ 
                        fontWeight: 800, 
                        fontSize: 22, 
                        color: theme === 'light' ? '#00838F' : '#90caf9', 
                        marginBottom: 16, 
                        textShadow: theme === 'light' 
                          ? 'none' 
                          : '0 2px 10px rgba(13, 71, 161, 0.6)', 
                        letterSpacing: '0.2px',
                        lineHeight: 1.3
                      }}>
                        {f.title}
                      </div>
                      <div style={{ 
                        color: theme === 'light' ? '#5a6c7d' : '#64b5f6', 
                        fontSize: 16, 
                        textShadow: theme === 'light'
                          ? 'none'
                          : '0 1px 5px rgba(13, 71, 161, 0.5)', 
                        lineHeight: 1.7, 
                        fontWeight: 500 
                      }}>
                        {f.shortDesc}
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* Badge */}
              <span className="hero-badge" style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 10,
                color: theme === 'light' ? '#00838F' : '#90caf9',
                fontSize: 15,
                fontWeight: 700,
                letterSpacing: '1px',
                marginBottom: 40,
                background: theme === 'light' 
                  ? 'rgba(255, 255, 255, 0.95)' 
                  : 'rgba(21, 101, 192, 0.4)',
                padding: '14px 32px',
                borderRadius: 50,
                border: theme === 'light'
                  ? '2px solid rgba(0, 188, 212, 0.3)'
                  : '2px solid rgba(66, 165, 245, 0.5)',
                textTransform: 'uppercase',
                boxShadow: theme === 'light'
                  ? '0 8px 32px rgba(0, 188, 212, 0.2)'
                  : '0 8px 32px rgba(13, 71, 161, 0.3)',
                animation: 'fadeInUp 1s ease-out 0.2s both',
                backdropFilter: 'blur(10px)'
              }}>
                <span style={{ fontSize: 18 }}>✨</span>
                Yapay Zekâ ile Kişiselleştirilmiş CV
                <span style={{ fontSize: 18 }}>✨</span>
              </span>

              {/* Main Heading */}
              <h1 className="hero-title" style={{
                fontSize: 'clamp(2rem, 4vw, 3rem)',
                fontWeight: 900,
                color: theme === 'light' ? '#4a5568' : '#ffffff',
                marginBottom: 32,
                lineHeight: 1.15,
                letterSpacing: '-0.04em',
                maxWidth: 1200,
                margin: '0 auto 40px auto',
                textShadow: theme === 'light' ? 'none' : '0 4px 30px rgba(0, 0, 0, 0.3), 0 2px 10px rgba(0, 0, 0, 0.2)',
                animation: 'fadeInUp 1s ease-out 0.4s both'
              }}>
                Kariyerin İçin
                <br />
                <span style={{
                  background: theme === 'light' 
                    ? '#00BCD4'
                    : 'linear-gradient(135deg, #ffffff 0%, #e3f2fd 50%, #bbdefb 100%)',
                  color: theme === 'light' ? '#ffffff' : 'transparent',
                  WebkitBackgroundClip: theme === 'light' ? 'unset' : 'text',
                  WebkitTextFillColor: theme === 'light' ? '#ffffff' : 'transparent',
                  backgroundClip: theme === 'light' ? 'unset' : 'text',
                  textShadow: 'none',
                  display: 'inline-block',
                  position: 'relative',
                  padding: theme === 'light' ? '10px 28px' : '0',
                  borderRadius: theme === 'light' ? '8px' : '0',
                  marginTop: theme === 'light' ? '8px' : '0',
                  marginBottom: theme === 'light' ? '8px' : '0',
                  fontSize: 'inherit'
                }}>
                  Profesyonel CV Oluştur
                </span>
                <br />
                <span style={{
                  fontSize: '0.7em',
                  fontWeight: 600,
                  color: theme === 'light' ? '#5a6c7d' : 'rgba(255, 255, 255, 0.95)',
                  display: 'block',
                  marginTop: '0.3em',
                  letterSpacing: '0.02em'
                }}>
                  AI destekli, ATS uyumlu CV, dakikalar içinde hazır
                </span>
              </h1>

              {/* Description */}
              <p className="hero-description" style={{
                fontSize: 'clamp(18px, 2.5vw, 24px)',
                color: theme === 'light' ? '#5a6c7d' : 'rgba(255, 255, 255, 0.98)',
                marginBottom: 56,
                maxWidth: 900,
                margin: '0 auto 60px auto',
                lineHeight: 1.8,
                fontWeight: 400,
                textShadow: theme === 'light' ? 'none' : '0 2px 15px rgba(0, 0, 0, 0.2)',
                animation: 'fadeInUp 1s ease-out 0.6s both'
              }}>
                <strong style={{ fontWeight: 600, color: theme === 'light' ? '#4a5568' : 'inherit' }}>Yapay zeka teknolojisi</strong> ile profil bilgilerinizi ve hedef iş ilanınızı analiz ediyoruz. 
                <strong style={{ fontWeight: 600, color: theme === 'light' ? '#4a5568' : 'inherit' }}> ATS uyumlu</strong> anahtar kelimeler ve <strong style={{ fontWeight: 600, color: theme === 'light' ? '#4a5568' : 'inherit' }}>profesyonel formatlama</strong> ile 
                işe alım uzmanlarının dikkatini çekecek CV'nizi <strong style={{ fontWeight: 600, color: theme === 'light' ? '#4a5568' : 'inherit' }}>dakikalar içinde</strong> oluşturun.
              </p>

              {/* CTA Buttons */}
              <div className="hero-cta" style={{
                display: 'flex',
                gap: 24,
                justifyContent: 'center',
                flexWrap: 'wrap',
                marginBottom: 0,
                animation: 'fadeInUp 1s ease-out 0.8s both',
                alignItems: 'center'
              }}>
                <button
                  onClick={() => setCurrentView('register')}
                  className="cta-primary"
                  style={{
                    padding: '20px 56px',
                    fontSize: 20,
                    fontWeight: 700,
                    borderRadius: 50,
                    background: theme === 'light' 
                      ? 'linear-gradient(135deg, #00ACC1 0%, #00BCD4 100%)'
                      : 'linear-gradient(135deg, #ffffff 0%, #f5f5f5 100%)',
                    color: theme === 'light' ? '#ffffff' : '#00ACC1',
                    border: 'none',
                    cursor: 'pointer',
                    boxShadow: theme === 'light'
                      ? '0 12px 50px rgba(0, 188, 212, 0.4), 0 0 0 0 rgba(255, 255, 255, 0.5)'
                      : '0 12px 50px rgba(0, 188, 212, 0.3), 0 0 0 0 rgba(255, 255, 255, 0.5)',
                    transition: 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)',
                    letterSpacing: '0.8px',
                    position: 'relative',
                    overflow: 'hidden',
                    textTransform: 'uppercase'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.transform = 'translateY(-6px) scale(1.05)';
                    e.currentTarget.style.boxShadow = theme === 'light'
                      ? '0 18px 60px rgba(0, 188, 212, 0.5), 0 0 0 10px rgba(0, 188, 212, 0.15)'
                      : '0 18px 60px rgba(0, 188, 212, 0.4), 0 0 0 10px rgba(255, 255, 255, 0.25)';
                    e.currentTarget.style.background = theme === 'light'
                      ? 'linear-gradient(135deg, #00BCD4 0%, #4DD0E1 100%)'
                      : 'linear-gradient(135deg, #ffffff 0%, #e3f2fd 100%)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'translateY(0) scale(1)';
                    e.currentTarget.style.boxShadow = theme === 'light'
                      ? '0 12px 50px rgba(0, 188, 212, 0.4), 0 0 0 0 rgba(255, 255, 255, 0.5)'
                      : '0 12px 50px rgba(0, 188, 212, 0.3), 0 0 0 0 rgba(255, 255, 255, 0.5)';
                    e.currentTarget.style.background = theme === 'light'
                      ? 'linear-gradient(135deg, #00ACC1 0%, #00BCD4 100%)'
                      : 'linear-gradient(135deg, #ffffff 0%, #f5f5f5 100%)';
                  }}
                >
                  <span style={{ marginRight: 10, fontSize: 22 }}>🚀</span>
                  Başla
                </button>
                <button
                  onClick={() => setCurrentView('login')}
                  className="cta-secondary"
                  style={{
                    padding: '20px 56px',
                    fontSize: 20,
                    fontWeight: 700,
                    borderRadius: 50,
                    border: theme === 'light' 
                      ? '3px solid #00ACC1'
                      : '3px solid rgba(255, 255, 255, 0.9)',
                    background: theme === 'light'
                      ? 'rgba(255, 255, 255, 0.95)'
                      : 'rgba(255, 255, 255, 0.2)',
                    backdropFilter: 'blur(15px)',
                    color: theme === 'light' ? '#00ACC1' : '#ffffff',
                    cursor: 'pointer',
                    transition: 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)',
                    letterSpacing: '0.8px',
                    boxShadow: theme === 'light'
                      ? '0 10px 40px rgba(0, 188, 212, 0.2)'
                      : '0 10px 40px rgba(0, 0, 0, 0.2)',
                    textTransform: 'uppercase'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.background = theme === 'light'
                      ? 'rgba(0, 172, 193, 0.1)'
                      : 'rgba(255, 255, 255, 0.35)';
                    e.currentTarget.style.borderColor = theme === 'light'
                      ? '#00BCD4'
                      : 'rgba(255, 255, 255, 1)';
                    e.currentTarget.style.transform = 'translateY(-6px) scale(1.05)';
                    e.currentTarget.style.boxShadow = theme === 'light'
                      ? '0 15px 50px rgba(0, 188, 212, 0.3)'
                      : '0 15px 50px rgba(0, 0, 0, 0.3)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.background = theme === 'light'
                      ? 'rgba(255, 255, 255, 0.95)'
                      : 'rgba(255, 255, 255, 0.2)';
                    e.currentTarget.style.borderColor = theme === 'light'
                      ? '#00ACC1'
                      : 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.transform = 'translateY(0) scale(1)';
                    e.currentTarget.style.boxShadow = theme === 'light'
                      ? '0 10px 40px rgba(0, 188, 212, 0.2)'
                      : '0 10px 40px rgba(0, 0, 0, 0.2)';
                  }}
                >
                  Giriş Yap
                </button>
              </div>

            </div>
          </div>
        )}

        {currentView === 'login' && (
          <Login onSwitchToRegister={() => setCurrentView('register')} />
        )}

        {currentView === 'register' && (
          <Register onSwitchToLogin={() => setCurrentView('login')} />
        )}

        {currentView === 'test' && (
          <div className="test-section">
            <TestAPI />
          </div>
        )}
      </div>

      {/* Footer - Sadece ana sayfada göster */}
      {currentView === 'home' && (
        <footer style={{
          background: theme === 'light' 
            ? 'rgba(255, 255, 255, 0.12)' 
            : 'rgba(13, 71, 161, 0.2)',
          backdropFilter: 'blur(15px)',
          borderTop: theme === 'light'
            ? '1px solid rgba(0, 188, 212, 0.2)'
            : '1px solid rgba(66, 165, 245, 0.3)',
          padding:'32px 32px',
          marginTop:0,
          boxShadow: theme === 'light'
            ? '0 -4px 30px rgba(0, 188, 212, 0.15)'
            : '0 -4px 30px rgba(13, 71, 161, 0.25)',
          fontSize:15,
          color: theme === 'light' 
            ? '#00838F' 
            : 'rgba(144, 202, 249, 0.95)',
          width: '100%'
        }}>
          <div style={{
            maxWidth:1200, margin:'0 auto', display:'flex',
            alignItems:'center', justifyContent:'space-between', flexWrap:'wrap',
            gap: 20
          }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              flexWrap: 'wrap'
            }}>
              <div style={{
                fontSize: 18,
                fontWeight: 800,
                background: theme === 'light'
                  ? 'linear-gradient(135deg, #00838F 0%, #00ACC1 100%)'
                  : 'linear-gradient(135deg, #64b5f6 0%, #90caf9 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                backgroundClip: 'text',
                letterSpacing: '-0.3px'
              }}>
                CV Builder
              </div>
              {/* <span style={{ opacity: 0.7 }}>—</span> */}
              <span style={{ fontWeight: 500 }}>
                {/* © {new Date().getFullYear()} AI ile geleceğinize değer katın */}
              </span>
            </div>
            <div style={{
              display:'flex', 
              gap:20, 
              alignItems:'center',
              flexWrap: 'wrap'
            }}>
              <a href="#" style={{
                color: theme === 'light' ? '#00838F' : '#64b5f6',
                textDecoration:'none',
                fontWeight:500,
                transition: 'opacity 0.3s ease',
                fontSize: 14
              }}
              onMouseEnter={(e) => e.currentTarget.style.opacity = '0.7'}
              onMouseLeave={(e) => e.currentTarget.style.opacity = '1'}>
                Gizlilik Politikası
              </a>
              <a href="#" style={{
                color: theme === 'light' ? '#00838F' : '#64b5f6',
                textDecoration:'none',
                fontWeight:500,
                transition: 'opacity 0.3s ease',
                fontSize: 14
              }}
              onMouseEnter={(e) => e.currentTarget.style.opacity = '0.7'}
              onMouseLeave={(e) => e.currentTarget.style.opacity = '1'}>
                Kullanım Şartları
              </a>
              <a href="#" style={{
                color: theme === 'light' ? '#00838F' : '#64b5f6',
                textDecoration:'none',
                fontWeight:500,
                transition: 'opacity 0.3s ease',
                fontSize: 14
              }}
              onMouseEnter={(e) => e.currentTarget.style.opacity = '0.7'}
              onMouseLeave={(e) => e.currentTarget.style.opacity = '1'}>
                İletişim
              </a>
            </div>
          </div>
        </footer>
      )}
    </div>
  );
};

export default Home;