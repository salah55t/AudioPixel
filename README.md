# 🎙️ AudioPixel — حوّل صوتك إلى صورة مشفّرة

> تطبيق أندرويد أصلي (Native Kotlin + Jetpack Compose) يقوم بتسجيل الصوت، تشفيره بصيغة صورة PNG، ثم استرجاعه كصوت مُشغّل من الصورة المولّدة.

<p align="center">
  <strong>صوت ← صورة ← صوت</strong>
</p>

<p align="center">
  <a href="https://github.com/salah55t/AudioPixel/actions/workflows/build-apk.yml">
    <img alt="Build APK" src="https://github.com/salah55t/AudioPixel/actions/workflows/build-apk.yml/badge.svg" />
  </a>
  <a href="https://github.com/salah55t/AudioPixel/releases">
    <img alt="Latest Release" src="https://img.shields.io/github/v/release/salah55t/AudioPixel?include_prereleases&label=Release" />
  </a>
  <a href="https://github.com/salah55t/AudioPixel/blob/main/LICENSE">
    <img alt="License" src="https://img.shields.io/github/license/salah55t/AudioPixel?label=License" />
  </a>
  <img alt="Min Android" src="https://img.shields.io/badge/Min%20Android-8.0%20(API%2026)-00E5FF" />
  <img alt="Target Android" src="https://img.shields.io/badge/Target%20Android-15%20(API%2035)-7C4DFF" />
</p>

---

## 🚀 تثبيت سريع (بدون بناء)

> **لا تريد البناء بنفسك؟** حمّل APK جاهزاً من تبويب [Actions](https://github.com/salah55t/AudioPixel/actions) (اختر آخر run ناجح ← Artifacts) أو من [Releases](https://github.com/salah55t/AudioPixel/releases) إن وُجد إصدار مُرقّم.

1. افتح https://github.com/salah55t/AudioPixel/actions
2. اختر آخر بناء أخضر في قائمة "Build APK"
3. انزل لأسفل قسم **Artifacts** وحمّل `AudioPixel-debug-apk`
4. فعّل "تثبيت من مصادر غير معروفة" في إعدادات أندرويد
5. افتح ملف APK من مدير الملفات للتثبيت

---

## 📖 الفكرة

`AudioPixel` يحوّل **بايتات PCM الصوتية الخام** إلى **بكسلات في صورة PNG** (طريقة *Byte-to-Pixel Lossless*)، ثم يعيد استرجاعها بدقة 100% عند فك التشفير. الهدف هو حماية الصوت بصيغة بصرية يصعب تمييزها على أنها ملف صوتي — نوع من التشفير البصري.

### آلية التشفير
1. **الترويسة (Header) — 16 بايت:**
   - 8 بايت: بصمة سحرية `"AudioPix"` (للتحقق عند فك التشفير).
   - 8 بايت: طول بيانات الصوت الأصلي (long).
2. **الحمولة (Payload):** بايتات الصوت الخام تُوزّع على البكسلات ثلاثياً ثلاثياً (`R,G,B` لكل بكسل).
3. **الصورة الناتجة:** PNG lossless (لا فقدان للبيانات عند الضغط)، بأبعاد شبه مربّعة تُحسب تلقائياً.
4. **عند فك التشفير:** يقرأ التطبيق البكسلات، يتحقق من البصمة السحرية، يستخرج الطول، ثم يقطع أي بايت زائد.

---

## ✨ المميزات

- ✅ **تسجيل صوتي حقيقي** عبر `AudioRecord` بمعيّنات PCM 16-bit Mono 44.1kHz.
- ✅ **تحويل Byte-to-Pixel Lossless** بدون أي فقدان للجودة.
- ✅ **صور PNG قابلة للمشاركة** عبر واتساب، تيليجرام، البريد… وتُفك شفرتها فقط داخل التطبيق.
- ✅ **مكتبة تسجيلات** محفوظة محلياً (WAV + PNG) مع صور مصغّرة ومعلومات (التاريخ، المدة، الحجم).
- ✅ **شاشة فك تشفير مستقلة** تستورد أي صورة من المعرض وتحاول استرجاع الصوت.
- ✅ **واجهة عربية كاملة** مع دعم RTL وثيم Material 3 داكن.
- ✅ **بنية Android حديثة:** Kotlin 2.1, Jetpack Compose BOM 2024.12, AGP 8.7, Compose Navigation, Coil.

---

## 🛠️ البناء

### المتطلّبات
- Android Studio Ladybug (2024.2.1) أو أحدث
- JDK 17
- Android SDK 35 (compileSdk)
- Gradle 8.9

### خطوات البناء
1. استنساخ المستودع:
   ```bash
   git clone https://github.com/<username>/AudioPixel.git
   cd AudioPixel
   ```
2. فتح المشروع في Android Studio: `File → Open → اختر مجلد AudioPixel`.
3. عند أول فتح، سيُنزّل Android Studio تلقائياً: Gradle Wrapper, AndroidX, Compose dependencies.
4. **ملاحظة:** ملف `gradle/wrapper/gradle-wrapper.jar` غير مُدرج في git (راجع `.gitignore`). لتوليده:
   ```bash
   gradle wrapper --gradle-version 8.9
   ```
   أو دع Android Studio يقوم بذلك تلقائياً عند أول `Sync`.
5. اختيار جهاز (محرّك أو هاتف حقيقي) ثم `Run ▶`.

### البناء عبر CLI
```bash
./gradlew assembleRelease      # يولّد APK في app/build/outputs/apk/release/
./gradlew installDebug        # يثبّت debug-APK على جهاز موصول
```

---

## 📱 استخدام التطبيق

### 1. شاشة التسجيل (Record)
- اضغط زر **بدء التسجيل** — سيبدأ مؤشّر النبض البصري بالظهور.
- تحدّث بالميكروفون… سيظهر مستوى الصوت في موجات نابضة.
- اضغط **إيقاف وحفظ** — سيتم:
  - حفظ ملف WAV في `filesDir/audio/rec_<timestamp>.wav`
  - توليد صورة PNG في `filesDir/images/rec_<timestamp>.png`
  - تسجيل دخلة في قاعدة بيانات TSV بسيطة في `filesDir/recordings.tsv`
- يمكنك **تشغيل آخر تسجيلة** مباشرة من الزر السفلي.

### 2. شاشة المكتبة (Library)
- اضغط **المكتبة** من الشاشة الرئيسية.
- ستجد قائمة بكل التسجيلات، مع:
  - صورة مصغّرة للصورة المشفّرة.
  - اسم التسجيلة + التاريخ + المدة + الحجم.
  - أزرار: ▶ تشغيل، ↗ مشاركة، 🗑 حذف.

### 3. شاشة فك التشفير (Decode)
- اضغط **فك تشفير**.
- اختر صورة PNG (مولّدة بواسطة AudioPixel) من المعرض.
- سيحاول التطبيق قراءة البصمة السحرية، فإن نجح سيظهر زر **▶ تشغيل الصوت المفكوك**.
- إن كانت الصورة ليست AudioPixel-encoded ستظهر رسالة خطأ واضحة.

---

## 🏗️ بنية المشروع

```
AudioPixel/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/audiopixel/app/
│       │   ├── AudioPixelApp.kt          # Application class + repository holder
│       │   ├── MainActivity.kt           # NavHost + permission launcher
│       │   ├── audio/
│       │   │   ├── AudioRecorder.kt      # AudioRecord + PCM 16-bit 44.1kHz
│       │   │   └── AudioPlayer.kt        # MediaPlayer + AudioTrack
│       │   ├── codec/
│       │   │   └── AudioImageCodec.kt    # Byte ↔ Bitmap lossless converter
│       │   ├── data/
│       │   │   └── RecordingRepository.kt # CRUD + WAV builder
│       │   └── ui/
│       │       ├── theme/{Color,Type,Theme}.kt
│       │       └── screens/
│       │           ├── RecordScreen.kt
│       │           ├── LibraryScreen.kt
│       │           └── DecodeScreen.kt
│       └── res/
│           ├── drawable/ic_launcher_foreground.xml
│           ├── mipmap-anydpi-v26/ic_launcher.xml + ic_launcher_round.xml
│           ├── values/{colors,strings,themes}.xml
│           └── xml/{backup_rules,data_extraction_rules,file_paths}.xml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml              # Version catalog
└── gradle/wrapper/gradle-wrapper.properties
```

---

## 🔬 تفاصيل تقنية

### تنسيق الصورة المشفّرة
| الإزاحة | الحجم | المحتوى |
|--------|------|---------|
| 0 | 8 bytes | `"AudioPix"` (ASCII) — بصمة سحرية |
| 8 | 8 bytes | طول البيانات الصوتية الأصلية (BigEndian long) |
| 16 | N bytes | بايتات PCM الصوتية الخام |
| 16 + N | 0..2 bytes | بايتات تعبئة (لا تُقرأ عند فك التشفير) |

**الحدود:**
- أبعاد الصورة: تُحسب تلقائياً (شبه مربعة بجانب حتى 2048).
- السعة القصوى: ~12.6 MB صوت خام لكل صورة (≈ 2.5 دقيقة بجودة 44.1kHz/16bit/Mono).

### الأذونات المطلوبة
| الإذن | الغرض | الإصدار |
|------|------|---------|
| `RECORD_AUDIO` | تسجيل الميكروفون | جميع الإصدارات |
| `READ_MEDIA_IMAGES` | استيراد صور للفك | Android 13+ |
| `READ_EXTERNAL_STORAGE` | استيراد صور للفك | Android ≤ 12 |
| `WRITE_EXTERNAL_STORAGE` | (لم يعد مطلوباً — نستخدم Context.filesDir) | — |

---

## 🛡️ الأمان والخصوصية

- ❗ هذه الطريقة ليست **تشفيراً قوياً** — هي مجرد **إخفاء بصري (Steganography)** للبيانات داخل صورة PNG. أي شخص يمتلك الكود يمكنه فك التشفير.
- 🔐 لتشفير حقيقي، يمكن تمديده بـ AES-256 قبل encode (انظر TODO أدناه).
- 📁 كل البيانات تُحفظ محلياً في تخزين داخلي للتطبيق (`filesDir`) — لا يوجد أي إرسال بيانات لأي خادم.

### TODO (مقترحات للتطوير)
- [ ] إضافة طبقة AES-256 اختيارية محمية بكلمة مرور.
- [ ] دعم الـ Steganography داخل صور طبيعية (إخفاء الصوت داخل صورة فوتوغرافية عادية).
- [ ] اختيار جودة التسجيل (8kHz/16kHz/44.1kHz) وأجهزة Mono/Stereo.
- [ ] دعم Cloud backup (Google Drive, Dropbox) للصور المولّدة.
- [ ] مُحرّك موجة صوتية حيّ أثناء التسجيل (Canvas متقدّم).
- [ ] دعم اللغة الإنجليزية (ثنائية اللغة عبر strings.xml).

---

## 📜 الترخيص

مُرخّص تحت رخصة MIT — راجع ملف `LICENSE`.

---

## 🤝 المساهمة

المساهمات مرحب بها! يُرجى فتح issue قبل أي PR كبير، وتأكد من اجتياز `./gradlew check`.
