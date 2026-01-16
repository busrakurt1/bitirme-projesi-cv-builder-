import { useState, useEffect, useRef } from 'react';
import { chatbotAPI } from '../../services/api';
import { userManager } from '../../services/api';
import { useTheme } from '../../contexts/ThemeContext';
import './Chatbot.css';

const Chatbot = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const messagesEndRef = useRef(null);
  const { theme } = useTheme();

  useEffect(() => {
    // Kullanıcı giriş durumunu kontrol et
    const checkAuth = () => {
      const loggedIn = userManager.isLoggedIn();
      const userId = userManager.getUserId();
      console.log('Chatbot: Auth kontrolü', { loggedIn, userId });
      setIsLoggedIn(loggedIn);
    };
    checkAuth();
    // Her 1 saniyede bir kontrol et (kullanıcı giriş yaptığında chatbot görünsün)
    const interval = setInterval(checkAuth, 1000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (isOpen) {
      loadChatHistory();
    }
  }, [isOpen]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const loadChatHistory = async () => {
    try {
      const userId = userManager.getUserId();
      if (!userId) {
        console.log('Chatbot: Kullanıcı ID bulunamadı');
        return;
      }

      const response = await chatbotAPI.getHistory();
      if (response && response.data) {
        // Mesajları user ve assistant olarak ayır
        const formattedMessages = [];
        response.data.forEach((msg) => {
          if (msg.message && msg.message.trim()) {
            formattedMessages.push({
              role: 'user',
              text: msg.message,
              timestamp: msg.createdAt,
            });
          }
          if (msg.response && msg.response.trim()) {
            formattedMessages.push({
              role: 'assistant',
              text: msg.response,
              timestamp: msg.createdAt,
            });
          }
        });
        setMessages(formattedMessages);
      }
    } catch (error) {
      console.error('Chat geçmişi yüklenemedi:', error);
      // Hata durumunda boş mesaj listesi göster
      setMessages([]);
    }
  };

  const sendMessage = async () => {
    if (!inputMessage.trim() || isLoading) return;

    const userMessage = inputMessage.trim();
    setInputMessage('');
    
    // Kullanıcı mesajını ekle
    const newUserMessage = {
      role: 'user',
      text: userMessage,
      timestamp: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, newUserMessage]);
    setIsLoading(true);

    try {
      const userId = userManager.getUserId();
      if (!userId) {
        throw new Error('Kullanıcı ID bulunamadı. Lütfen giriş yapın.');
      }

      console.log('Chatbot: Mesaj gönderiliyor...', { userId, message: userMessage });
      const response = await chatbotAPI.sendMessage(userMessage, userId);
      console.log('Chatbot: Yanıt alındı', response);
      
      // AI yanıtını al ve temizle
      let responseText = response?.data?.response || response?.response || '';
      
      // Eğer yanıt sadece parantez veya boşluk içeriyorsa
      if (!responseText || responseText.trim().match(/^[\s\{\}\[\]\(\)]*$/)) {
        console.warn('Chatbot: Geçersiz yanıt alındı:', responseText);
        responseText = 'Üzgünüm, yanıt alınamadı. Lütfen tekrar deneyin.';
      }
      
      // AI yanıtını ekle
      const assistantMessage = {
        role: 'assistant',
        text: responseText.trim(),
        timestamp: response?.data?.createdAt || new Date().toISOString(),
      };
      setMessages((prev) => [...prev, assistantMessage]);
    } catch (error) {
      console.error('Mesaj gönderilemedi:', error);
      const errorText = error.response?.data?.message || error.message || 'Üzgünüm, bir hata oluştu. Lütfen tekrar deneyin.';
      const errorMessage = {
        role: 'assistant',
        text: errorText,
        timestamp: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const clearHistory = async () => {
    try {
      await chatbotAPI.clearHistory();
      setMessages([]);
    } catch (error) {
      console.error('Geçmiş temizlenemedi:', error);
      const errorText = error.response?.data?.message || error.message || 'Geçmiş temizlenirken bir hata oluştu.';
      alert(errorText);
    }
  };

  // Sadece giriş yapmış kullanıcılara göster
  if (!isLoggedIn) {
    return null;
  }

  if (!isOpen) {
    return (
      <button
        className={`chatbot-toggle theme-${theme}`}
        onClick={() => setIsOpen(true)}
        aria-label="Chatbot'u aç"
      >
        <svg
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <path
            d="M20 2H4C2.9 2 2 2.9 2 4V22L6 18H20C21.1 18 22 17.1 22 16V4C22 2.9 21.1 2 20 2Z"
            fill="currentColor"
          />
        </svg>
      </button>
    );
  }

  return (
    <div className={`chatbot-container theme-${theme}`}>
      <div className={`chatbot-header theme-${theme}`}>
        <div className="chatbot-header-content">
          <div className="chatbot-title">
            <span className="chatbot-icon">💬</span>
            <span>CV Builder Asistanı</span>
          </div>
          <div className="chatbot-actions">
            <button
              className="chatbot-clear-btn"
              onClick={clearHistory}
              title="Geçmişi temizle"
            >
              🗑️
            </button>
            <button
              className="chatbot-close-btn"
              onClick={() => setIsOpen(false)}
              aria-label="Chatbot'u kapat"
            >
              ✕
            </button>
          </div>
        </div>
      </div>

      <div className={`chatbot-messages theme-${theme}`}>
        {messages.length === 0 ? (
          <div className="chatbot-welcome">
            <p>👋 Merhaba! CV Builder asistanına hoş geldiniz.</p>
            <p>Size nasıl yardımcı olabilirim?</p>
            <div className="chatbot-suggestions">
              <button
                onClick={() => setInputMessage('CV oluşturma konusunda yardım almak istiyorum')}
              >
                CV oluşturma konusunda yardım
              </button>
              <button
                onClick={() => setInputMessage('İş başvurusu için önerileriniz neler?')}
              >
                İş başvurusu önerileri
              </button>
              <button
                onClick={() => setInputMessage('Kariyer gelişimi için tavsiyeleriniz neler?')}
              >
                Kariyer gelişimi tavsiyeleri
              </button>
            </div>
          </div>
        ) : (
          messages.map((msg, index) => (
            <div
              key={index}
              className={`chatbot-message chatbot-message-${msg.role} theme-${theme}`}
            >
              <div className="chatbot-message-content">
                {msg.text}
              </div>
            </div>
          ))
        )}
        {isLoading && (
          <div className={`chatbot-message chatbot-message-assistant theme-${theme}`}>
            <div className="chatbot-message-content">
              <div className="chatbot-typing">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className={`chatbot-input-container theme-${theme}`}>
        <textarea
          className={`chatbot-input theme-${theme}`}
          value={inputMessage}
          onChange={(e) => setInputMessage(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="Mesajınızı yazın..."
          rows="1"
          disabled={isLoading}
        />
        <button
          className={`chatbot-send-btn theme-${theme}`}
          onClick={sendMessage}
          disabled={!inputMessage.trim() || isLoading}
        >
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M2 21L23 12L2 3V10L17 12L2 14V21Z"
              fill="currentColor"
            />
          </svg>
        </button>
      </div>
    </div>
  );
};

export default Chatbot;

