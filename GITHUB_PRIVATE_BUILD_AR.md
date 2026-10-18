# Explode Arena — تشغيل بناء APK في GitHub خاص

## حالة المشروع

- Mainline 0.21 Alpha يتضمن المصدر الكامل تحت `web/` ومشروع Android تحت `android/`.
- `android-debug.yml` يطلب Gradle 8.7 وAndroid SDK 35 على GitHub Actions.
- **لا يوجد APK مُثبت النجاح حتى يعمل الـworkflow فعلًا ويُستخرج ملف APK الناتج.**
- وظيفة Actions ترفض البناء إذا كانت `repository.private` لا تساوي `true`؛ لا تغيّرها إلى public. حقوق موارد اللعبة الأصلية محفوظة لأصحابها.

## خطوة الإعداد الواحدة

أنشئ مستودع GitHub جديدًا باسم `explode-arena-android-remaster` مع اختيار **Private** وتهيئته بـREADME أولي إن لزم. احتفظ به مستقلًا عن مستودعات البرامج الأخرى. بعد تجهيز المستودع، يمكن لـChatGPT Work رفع محتويات **مجلد** `Explode_Arena_Mainline` إليه (ليس رفع ZIP كما هو)، ثم تنفيذ بناء GitHub Actions.

## تشغيل البناء

1. ارفع كل محتويات مجلد `Explode_Arena_Mainline` إلى **جذر** المستودع بحيث يظهر `android/` و`web/` و`.github/workflows/` في الجذر.
2. من صفحة المستودع اختر **Actions → Build private Android debug APK → Run workflow**. وقد يبدأ عند أول دفع إلى `main` إذا تطابقت المسارات.
3. بعد اكتمال التشغيل بنجاح، افتح الـrun ثم `Artifacts` وحمّل `ExplodeArena-private-debug-apk`.
4. داخل الأرشيف الناتج: `app-debug.apk` و`app-debug.apk.sha256`. تحقّق من البصمة قبل التثبيت.
5. ثبّت APK على جهاز Android للاختبار؛ النجاح على GitHub لا يغني عن اختبار حقيقي على الجهاز.

## التسليم إلى Work

**تعليمات Work:** استخدم ZIP الأحدث الخاص بـExplode Arena Mainline 0.21؛ استخرج محتوياته؛ أنشئ أو استخدم مستودع GitHub خاصًا باسم `explode-arena-android-remaster` في الحساب المتصل؛ ارفع الملفات إلى الجذر مع حفظ `.github`؛ افحص أن visibility تساوي `private` قبل رفع موارد اللعبة؛ شغّل `Build private Android debug APK`؛ تحقق من حالة run والسجلات؛ إن نجح، نزّل artifact الفعلي وقدّم APK والبصمة ونتيجة الفحص. إن فشل، أصلح سبب الفشل في نفس Mainline وشغّل الاختبارات والبناء من جديد. لا تعلن نجاح APK قبل امتلاك ملف فعلي.
