// src/components/auth/Login.jsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authAPI, userManager } from "../../services/api";
import { useTheme } from "../../contexts/ThemeContext";
import "./login.css";

const Login = ({ onSwitchToRegister }) => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const navigate = useNavigate();
  const { theme } = useTheme();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage("");

    try {
      const response = await authAPI.login(email, password);
      const userData = response.data.data;
      userManager.setUser(userData);

      setMessage("✅ Giriş başarılı!");
      setTimeout(() => navigate("/dashboard"), 900);
    } catch (error) {
      setMessage("❌ Giriş başarısız: " + (error.response?.data?.message || error.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`auth-wrapper theme-${theme}`} data-theme={theme}>
      <div className="auth-card">
        <div className="auth-head">
          <div className="auth-dot" aria-hidden="true" />
          <div>
            <h2 className="auth-title">Giriş </h2>
            <p className="auth-subtitle">Hesabınıza giriş yapın</p>
          </div>
        </div>

        {message && (
          <div className={`auth-message ${message.includes("✅") ? "success" : "error"}`}>
            {message}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label className="form-label">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="form-input"
              placeholder="email@example.com"
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Şifre</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="form-input"
              placeholder="Şifreniz"
              required
            />
          </div>

          <button type="submit" disabled={loading} className="auth-button">
            {loading ? "⏳ Giriş Yapılıyor..." : "Giriş Yap"}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Hesabın yok mu?{" "}
            <button type="button" onClick={onSwitchToRegister} className="auth-link">
              Kayıt ol
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;
