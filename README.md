# Info Ringkas 📖⚡

**Info Ringkas** adalah aplikasi berita modern untuk Android yang dirancang untuk memberikan informasi terbaru dari Indonesia. Aplikasi ini tidak hanya menampilkan berita, tetapi juga merangkumnya menggunakan AI, sehingga pengguna tidak harus membaca artikel sepenuhnya.

![Contoh Tampilan Aplikasi Info Ringkas](https://placehold.co/800x400/FFFFFF/000000?text=Tampilan+Aplikasi+Info+Ringkas)

---

## ✨ Fitur Utama

-   **Berita Terkini**: Mengambil berita terbaru langsung dari [NewsData.io API](https://newsdata.io/).
-   **Rangkuman Berita dengan AI**: Terintegrasi dengan **Google Gemini AI** untuk memberikan rangkuman singkat dari artikel berita, hanya dengan satu ketukan.
-   **Tampilan Modern**: Dibangun dengan komponen **Material You (Material 3)** dari Google, memberikan UI yang bersih, modern, dan adaptif.
-   **Tema Terang & Gelap**: Tema aplikasi secara otomatis beradaptasi dengan pengaturan sistem pengguna untuk kenyamanan membaca pengguna.
-   **Akses Offline**: Semua berita yang pernah dimuat maupun dirangkum disimpan di database **SQLite** lokal.
-   **Manajemen Favorit**: Pengguna dapat menandai berita penting untuk dibaca kembali nanti di tab favorit.
-   **Pencarian Cepat**: Fitur pencarian *real-time* agar pengguna dengan mudah menemukan berita berdasarkan judul.
-   **Performa Optimal**: Menggunakan `DiffUtil` pada `RecyclerView` untuk pembaruan UI yang efisien dan animasi yang mulus.

---

## 🛠️ Teknologi & Dependensi

Proyek ini dibangun menggunakan teknologi dan library sesuai pengembangan Android.

-   **Bahasa**: Java
-   **Arsitektur**: Desain terstruktur dengan Fragment dan Activity
-   **Networking**: [Retrofit 2](https://square.github.io/retrofit/) & OkHttp3
-   **Parsing JSON**: [Gson](https://github.com/google/gson)
-   **UI & Desain**:
    -   [Material Components for Android (Material 3)](https://material.io/develop/android/docs/getting-started)
    -   ViewBinding
    -   [LottieFiles](https://lottiefiles.com/)
-   **Manajemen Database**: SQLite (melalui `SQLiteOpenHelper`)
-   **Pemuat Gambar**: [Glide](https://github.com/bumptech/glide)
-   **Asynchronous**: Handler & ExecutorService

---

## 🚀 Konfigurasi Proyek

Repositori ini berisi **sumber kode utama** (`/main`) dari aplikasi. Untuk menjalankan kode ini, Anda perlu membuat proyek Android Studio baru dan menyalin file-file ini ke dalamnya.

#### Langkah 1: Buat Proyek Baru

1.  Buka Android Studio.
2.  Pilih **New Project** -> **Empty Views Activity**.
3.  Konfigurasikan proyek:
    -   **Name**: InfoRingkas
    -   **Package name**: `com.example.inforingkas`
    -   **Language**: Java
    -   **Minimum SDK**: API 31 (S) atau lebih tinggi
4.  Klik **Finish** dan tunggu hingga proyek selesai dibuat.

#### Langkah 2: Salin Kode Sumber

1.  Unduh atau clone repositori ini ke komputer Anda.
2.  Buka direktori proyek Android Studio baru Anda di file explorer.
3.  **Hapus** folder `java` dan `res` di dalam `YourNewProject/app/src/main/`.
4.  **Salin** folder `java` dan `res` dari repositori ini ke dalam `YourNewProject/app/src/main/`.

#### Langkah 3: Konfigurasi Dependensi

1.  Buka file `build.gradle` **(Module :app)** di proyek Android Studio Anda.
2.  Pastikan blok `dependencies` Anda berisi semua library yang diperlukan:
    ```gradle
    dependencies {
        implementation libs.appcompat
        implementation libs.material
        implementation libs.activity
        implementation libs.constraintlayout
        implementation libs.navigation.fragment
        implementation libs.navigation.ui
        implementation libs.core.splashscreen
        implementation libs.retrofit
        implementation libs.converter.gson
        implementation libs.logging.interceptor
        implementation libs.glide
        implementation libs.lottie
    }
    ```
3.  Pastikan juga Anda sudah mengaktifkan `viewBinding` dan `buildConfig` di file yang sama:
    ```gradle
    android {
        // ...
        buildFeatures {
            viewBinding true
            buildConfig true
        }
    }
    ```

#### Langkah 4: Tambahkan API Key

1.  Di direktori **root** proyek Android Studio baru Anda, buat file bernama `local.properties`.
2.  Tambahkan API key Anda ke dalam file ini:
    ```properties
    NEWS_API_KEY="YOUR_NEWSDATA_API_KEY"
    GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
    ```

#### Langkah 5: Sinkronkan dan Jalankan

1.  Di Android Studio, klik **"Sync Now"** atau **`File > Sync Project with Gradle Files`**.
2.  Setelah selesai, pilih **`Build > Rebuild Project`**.
3.  Jalankan aplikasi.