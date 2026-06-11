<h1 align="center">
  DietAI
</h1>

<p align="center">
  <b>Yapay Zekâ Destekli Yeni Nesil Diyet ve Beslenme Yönetim Platformu</b><br>
  <i>(Kotlin Multiplatform, Jetpack Compose, Firebase & Google Gemini)</i>
</p>

---

## 📖 Proje Hakkında

**DietAI**, hem kullanıcıların (danışan) günlük beslenme ve sağlık verilerini takip edebildiği hem de uzman diyetisyenlerin kendi hastalarını yönetip, yapay zekâ (Google Gemini API) destekli diyet planları oluşturabildiği **çapraz platform (iOS & Android)** bir mobil sağlık (m-Health) uygulamasıdır. 

Bu proje, bir Bilgisayar Mühendisliği Bitirme Projesi olarak geliştirilmiş olup, modern yazılım mimarisi standartlarını (**MVVM, Kotlin Multiplatform, Offline-first Database**) temel almaktadır.

---

## ✨ Temel Özellikler

Uygulama temel olarak iki rol bazlı (Danışan ve Diyetisyen) çalışmaktadır:

### 👤 Danışan (Kullanıcı) Özellikleri
- **Öğün ve Su Takibi:** Tüketilen öğünlerin manuel veya yapay zeka desteğiyle eklenmesi ve makro besin analizlerinin yapılması.
- **Egzersiz Takibi:** Günlük fiziksel aktivitelerin kaydedilmesi.
- **Kilo ve Vücut Kitle İndeksi (VKİ):** Anlık kilo verisinin grafiksel olarak (Line Chart) izlenmesi.
- **Gerçek Zamanlı Sohbet:** Atanan diyetisyen ile gerçek zamanlı güvenli mesajlaşma.

### 🩺 Diyetisyen Özellikleri
- **Danışan Yönetimi:** Kendisine atanan danışanları tek ekranda görebilme, kilo ve beslenme geçmişlerini analiz etme.
- **Yapay Zeka (Gemini) İle Diyet Planı:** Danışanın fiziksel özelliklerine (yaş, boy, kilo, VKİ) göre Google Gemini API kullanarak saniyeler içinde 7 günlük taslak diyet planı oluşturma.
- **Plan Atama:** Oluşturulan veya düzenlenen diyet planını onaylayıp danışanın ekranına yansıtma.

---

## 🛠️ Kullanılan Teknolojiler ve Mimari

Uygulamanın iş mantığı **MVVM (Model-View-ViewModel)** deseniyle organize edilmiştir. 

- **Ortak Kod (Shared):** Kotlin Multiplatform (KMP)
- **Kullanıcı Arayüzü (UI):** Compose Multiplatform (Android ve iOS ortak arayüzü)
- **Kimlik Doğrulama:** Firebase Authentication (E-posta/Şifre)
- **Veritabanı:** Cloud Firestore (NoSQL, Role-Based Access Control, Çevrimdışı önbellekleme)
- **Yapay Zeka:** Google Gemini API (`gemini-2.5-flash` modeli)
- **Ağ İstekleri:** Ktor HTTP Client
- **Asenkronite & Durum Yönetimi:** Kotlin Coroutines & StateFlow

---

## 🚀 Kurulum ve Çalıştırma

### Gereksinimler
- **Android Studio** (Koala veya daha yeni bir sürüm önerilir)
- **Java JDK 17+**
- (iOS için) **Xcode** ve macOS işletim sistemi

### Adımlar

1. **Projeyi Klonlayın:**
   ```bash
   git clone https://github.com/KULLANICI_ADINIZ/DietAiApp.git
   cd DietAiApp
   ```

2. **Google Gemini API Anahtarını Ekleyin (ÖNEMLİ):**
   Projeyi çalıştırmadan önce kendi Gemini API anahtarınızı kod içerisine eklemelisiniz.
   - `composeApp/src/commonMain/kotlin/org/dietai/project/Config.kt` dosyasını açın.
   - `GEMINI_API_KEY` değişkeninin karşısındaki `"BURAYA_KENDI_API_KEYINIZI_YAZIN"` metnini kendi anahtarınızla değiştirin.
   > **Güvenlik Uyarısı:** Kendi API anahtarınızı yazdıktan sonra bu dosyayı asla herkese açık (Public) bir GitHub deposuna commit etmeyin!

3. **Android Cihazda Çalıştırma:**
   - Android Studio üzerinden projeyi açın.
   - Gradle senkronizasyonunun bitmesini bekleyin.
   - `composeApp` modülünü seçip **Run (Shift + F10)** butonuna basarak emülatörde veya gerçek cihazda test edin. Veya terminalden:
   ```bash
   ./gradlew :composeApp:installDebug
   ```

4. **Test (Seed) Verileri:**
   Proje açıldığında Firestore veritabanında daha önceden oluşturduğunuz bir Danışan ve bir Diyetisyen hesabı ile giriş yaparak tüm iş akışlarını test edebilirsiniz.

---
*Bu proje lisans projesi kapsamında geliştirilmiştir.*