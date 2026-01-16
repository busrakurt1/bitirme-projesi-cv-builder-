// src/components/auth/Register.jsx
import { useState } from "react";
import { authAPI, userManager } from "../../services/api";
import { useTheme } from "../../contexts/ThemeContext";
import "./login.css";

const Register = ({ onSwitchToLogin }) => {
  const { theme } = useTheme();
  const [formData, setFormData] = useState({
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
    location: "",
    phone: "",
    educationLevel: "",
  });

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const turkiyeSehirleri = [
    "",
    "Adana","Adıyaman","Afyonkarahisar","Ağrı","Amasya","Ankara","Antalya",
    "Artvin","Aydın","Balıkesir","Bilecik","Bingöl","Bitlis","Bolu",
    "Burdur","Bursa","Çanakkale","Çankırı","Çorum","Denizli","Diyarbakır",
    "Edirne","Elazığ","Erzincan","Erzurum","Eskişehir","Gaziantep",
    "Giresun","Gümüşhane","Hakkari","Hatay","Isparta","Mersin","İstanbul",
    "İzmir","Kars","Kastamonu","Kayseri","Kırklareli","Kırşehir","Kocaeli",
    "Konya","Kütahya","Malatya","Manisa","Kahramanmaraş","Mardin","Muğla",
    "Muş","Nevşehir","Niğde","Ordu","Rize","Sakarya","Samsun","Siirt",
    "Sinop","Sivas","Tekirdağ","Tokat","Trabzon","Tunceli","Şanlıurfa",
    "Uşak","Van","Yozgat","Zonguldak","Aksaray","Bayburt","Karaman",
    "Kırıkkale","Batman","Şırnak","Bartın","Ardahan","Iğdır","Yalova",
    "Karabük","Kilis","Osmaniye","Düzce",
  ];

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage("");

    if (formData.password !== formData.confirmPassword) {
      setMessage("❌ Şifreler eşleşmiyor!");
      setLoading(false);
      return;
    }

    const userData = {
      fullName: formData.fullName,
      email: formData.email,
      password: formData.password,
      location: formData.location,
      phone: formData.phone,
      educationLevel: formData.educationLevel,
      enabled: true,
    };

    try {
      const response = await authAPI.register(userData);

      if (response.data.success) {
        const u = response.data.data;
        userManager.setUser(u);

        setMessage("✅ Kayıt başarılı! Giriş sayfasına yönlendiriliyorsunuz...");
        setTimeout(() => onSwitchToLogin(), 1600);
      } else {
        setMessage("❌ Kayıt başarısız: " + (response.data.message || "Bilinmeyen hata"));
      }
    } catch (error) {
      const errorMsg =
        error.response?.data?.message ||
        error.response?.data?.error ||
        "Kayıt sırasında hata oluştu!";
      setMessage(`❌ ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData((p) => ({
      ...p,
      [e.target.name]: e.target.value,
    }));
  };

  return (
    <div className={`auth-wrapper theme-${theme}`} data-theme={theme}>
      <div className="auth-card">
        <div className="auth-head">
          <div className="auth-dot" aria-hidden="true" />
          <div>
            <h2 className="auth-title">Kayıt</h2>
            <p className="auth-subtitle">Yeni hesap oluşturun</p>
          </div>
        </div>

        {message && (
          <div className={`auth-message ${message.includes("✅") ? "success" : "error"}`}>
            {message}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          {/* 2'li grid: Ad Soyad + Telefon */}
          <div className="auth-row">
            <div className="form-group">
              <label className="form-label">Ad Soyad</label>
              <input
                type="text"
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                className="form-input"
                placeholder="Adınız ve soyadınız"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Telefon</label>
              <input
                type="tel"
                name="phone"
                value={formData.phone}
                onChange={handleChange}
                className="form-input"
                placeholder="555-123-4567"
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Email</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              className="form-input"
              placeholder="email@example.com"
              required
            />
          </div>

          {/* 2'li grid: Şifre + Şifre Tekrar */}
          <div className="auth-row">
            <div className="form-group">
              <label className="form-label">Şifre</label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                className="form-input"
                placeholder="Şifreniz"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Şifre Tekrar</label>
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                className="form-input"
                placeholder="Şifre tekrar"
                required
              />
            </div>
          </div>

          {/* 2'li grid: Konum + Eğitim */}
          <div className="auth-row">
            <div className="form-group">
              <label className="form-label">Konum</label>
              <select
                name="location"
                value={formData.location}
                onChange={handleChange}
                className="form-select"
                required
              >
                {turkiyeSehirleri.map((sehir, idx) => (
                  <option key={idx} value={sehir}>
                    {sehir === "" ? "Şehrinizi seçin" : sehir}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Eğitim Seviyesi</label>
              <select
                name="educationLevel"
                value={formData.educationLevel}
                onChange={handleChange}
                className="form-select"
              >
                <option value="">Seçiniz</option>
                <option value="İlkokul">İlkokul</option>
                <option value="Ortaokul">Ortaokul</option>
                <option value="Lise">Lise</option>
                <option value="Ön Lisans">Ön Lisans</option>
                <option value="Lisans">Lisans</option>
                <option value="Yüksek Lisans">Yüksek Lisans</option>
                <option value="Doktora">Doktora</option>
              </select>
            </div>
          </div>

          <button type="submit" disabled={loading} className="auth-button">
            {loading ? "⏳ Kayıt Yapılıyor..." : "Hesap Oluştur"}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Zaten hesabınız var mı?{" "}
            <button type="button" onClick={onSwitchToLogin} className="auth-link">
              GİRİŞ YAP
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;
