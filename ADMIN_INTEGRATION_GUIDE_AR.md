# دليل ربط تطبيق FlixTV (Android) مع لوحة التحكم (PHP Backend Guide)

> **ملاحظة:** تم تحديث هذا الدليل ليدعم لغة PHP مع إضافة ميزة استيراد اشتراكات Xtream المخصصة لكل مستخدم من قبل الأدمن.

---

## 1. عنوان لوحة التحكم (Base URL)
يجب على المستخدم وضع رابط السيرفر الخاص بك في إعدادات التطبيق (خانة "عنوان لوحة الإدارة"):
مثال: `https://app.flixplayer.pro` (بدون `/` في النهاية).

---

## 2. الواجهات البرمجية المطلوبة (Endpoints)

### أ. تسجيل مستخدم جديد (`register.php`)
عند إنشاء حساب جديد، يرسل التطبيق طلب `POST` لتسجيل البيانات بانتظار التفعيل.

*   **الرابط:** `POST /api/register.php`
*   **Request Body (JSON):**
    ```json
    {
      "fullName": "الاسم الكامل",
      "email": "user@example.com",
      "username": "username123",
      "phoneNumber": "0500000000",
      "activationCode": "FLIX-XXXXX"
    }
    ```
*   **الرد المتوقع (PHP JSON Response):**
    ```json
    {
      "success": true,
      "message": "تم تسجيل العضو بانتظار التفعيل",
      "activationCode": "FLIX-XXXXX"
    }
    ```

### ب. التحقق من حالة التفعيل وبيانات Xtream (`status.php`)
يُستدعى عند الضغط على زر "تحقق من التفعيل 🔄". إذا كان الأدمن قد وضع بيانات Xtream للمستخدم، سيقوم التطبيق بعرض زر الاستيراد التلقائي.

*   **الرابط:** `GET /api/status.php?username={username}&code={activationCode}`
*   **الرد المتوقع (حساب نشط مع بيانات Xtream مخصصة):**
    ```json
    {
      "activated": true,
      "status": "active",
      "message": "تم تفعيل الاشتراك بنجاح! شكراً لثقتكم بنا.",
      "expiresAt": 1798310400000,
      "xtreamHost": "http://line.dndnscloud.ru",
      "xtreamUsername": "4357d392ea",
      "xtreamPassword": "dd828ce13049"
    }
    ```
    > **ملاحظة:** حقول `xtreamHost`, `xtreamUsername`, `xtreamPassword` هي التي تفعّل زر **"استيراد وتفعيل البث الخاص بي"** داخل التطبيق.

---

## 3. مثال كود PHP متكامل (backend)

يمكنك استخدام هذا الكود كأساس لبناء لوحة التحكم الخاصة بك:

### `api/register.php`
```php
<?php
header("Content-Type: application/json");
$data = json_decode(file_get_contents("php://input"), true);

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = $data['username'];
    $fullName = $data['fullName'];
    $activationCode = $data['activationCode'];

    // 1. تحقق من عدم وجود المستخدم مسبقاً في قاعدة بياناتك
    // 2. احفظ المستخدم بحالة 'PENDING'
    
    echo json_encode([
        "success" => true,
        "message" => "تم تسجيل العضو بانتظار التفعيل",
        "activationCode" => $activationCode
    ]);
}
?>
```

### `api/status.php`
```php
<?php
header("Content-Type: application/json");
$username = $_GET['username'] ?? '';
$code = $_GET['code'] ?? '';

// استعلم من قاعدة البيانات عن المستخدم بواسطة username و activationCode
// $user = $db->query("SELECT * FROM users WHERE username = '$username' AND code = '$code'")->fetch();

if (!$user) {
    echo json_encode(["activated" => false, "message" => "بيانات الاشتراك غير صحيحة"]);
    exit;
}

if ($user['status'] === 'ACTIVE') {
    echo json_encode([
        "activated" => true,
        "status" => "active",
        "message" => "الاشتراك نشط ومفعل! 🚀",
        "expiresAt" => $user['expires_at'], // Timestamp بالميلي ثانية
        "xtreamHost" => $user['xtream_host'] ?? "",
        "xtreamUsername" => $user['xtream_user'] ?? "",
        "xtreamPassword" => $user['xtream_pass'] ?? ""
    ]);
} else {
    echo json_encode([
        "activated" => false,
        "status" => "pending",
        "message" => "اشتراكك غير نشط. يرجى إرسال الكود $code للمسؤول."
    ]);
}
?>
```

---

## 4. نصائح للتطوير (Best Practices)
1. **قاعدة البيانات:** أضف حقول `xtream_host`, `xtream_username`, `xtream_password` إلى جدول المستخدمين في MySQL.
2. **لوحة الإدارة:** في صفحة تعديل المستخدم، أضف 3 حقول نصية للأدمن ليضع فيها بيانات Xtream الخاصة بالمستخدم بعد التفعيل.
3. **الأمان:** يفضل استخدام `HTTPS` لجميع الطلبات لحماية بيانات المستخدمين واشتراكاتهم.
4. **التجربة:** يمكنك دائماً استخدام اسم المستخدم `activate_me` في التطبيق لاختبار ميزة التفعيل التلقائي (Mock) قبل ربط سيرفرك الحقيقي.
