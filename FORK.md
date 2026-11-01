# Panduan fork (GitHub + Vercel)

## 1. Build APK di GitHub
1. Push folder ini ke repo GitHub Anda sendiri (public atau private).
2. Actions → **build-android** → *Run workflow*. Isi `abis` (`arm64-v8a` cukup untuk HP modern, build pertama ±1 jam karena ffmpeg dikompilasi; berikutnya memakai cache).
3. APK ada di *Artifacts → android-apks*. Tag `v1.0.0` membuat build semua ABI + GitHub Release otomatis.
4. Secrets opsional (Settings → Secrets → Actions):
   - `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`: kunci tanda tangan Anda. Tanpa ini APK ditandatangani debug.
   - `YMUSIC_REMOTE_CONFIG_URL` (+ `YMUSIC_BUNDLE_PUBLIC_KEY`, `YMUSIC_UPDATE_PUBLIC_KEY`): server `ymusic-server`. **Tanpa ini remote config, update dalam aplikasi, SponsorBlock, dan tautan perangkat via QR nonaktif**, karena fitur-fitur itu memanggil server tersebut. `secrets.enc` bawaan terenkripsi dan tidak bisa dibuka tanpa passphrase pemilik asli.
5. `release.yml` bawaan (macOS DMG, tes, cek kompatibilitas tag) tetap ada dan hanya jalan pada tag `release-*`.

## 2. Deploy ke Vercel
Import repo di Vercel. `vercel.json` sudah mengarah ke `web/` tanpa build step. Ubah `web/config.js` ke `OWNER/REPO` fork Anda. Situs berisi unduhan APK terbaru dan Theme Studio web.

Situs ini **bukan** pemutar musik di browser: mesin aplikasi (QuickJS native, ffmpeg, MSE host C) tidak bisa berjalan di browser/Vercel tanpa penulisan ulang besar.

## 3. Theme Studio (di aplikasi)
Pengaturan → Tampilan → **Studio tema**: preset, warna latar, ukuran teks (85–130%), kebulatan sudut (0–200%), dan kode tema `ymtheme1|mode|aksen|latar|teks|sudut` yang bisa dibuat di situs web lalu ditempel di aplikasi.

## 4. Build cepat
`YMUSIC_ABIS=arm64-v8a ./gradlew :androidApp:assembleRelease` hanya mengompilasi satu ABI (default tetap empat).

Lisensi: GPL-3.0 (lihat `LICENSE`). Jika membagikan APK, sumbernya harus tersedia.
