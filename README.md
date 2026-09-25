---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: c226c988c4ae1226f6aeeb801a0caa8b_3dec220cb87811f1b24b525400ea19b7
    ReservedCode1: hOk5gHEydnxsw2gOGZRr6/AKHcUoPWvi/BGLS/STJdVmfIGeNLuEK6tyYQ1tEfPIoNfRVbDzExbnyP6GXD7ksr4AL1ci5jBs6v86waMdTrccJcfP5u/AR2urFZ8mQUybnpm12Wxz0Dgg/qE7r3q5iH9s9+GRR5M8h0k+osrj1WGo1+oiSRHN+107LBU=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: c226c988c4ae1226f6aeeb801a0caa8b_3dec220cb87811f1b24b525400ea19b7
    ReservedCode2: hOk5gHEydnxsw2gOGZRr6/AKHcUoPWvi/BGLS/STJdVmfIGeNLuEK6tyYQ1tEfPIoNfRVbDzExbnyP6GXD7ksr4AL1ci5jBs6v86waMdTrccJcfP5u/AR2urFZ8mQUybnpm12Wxz0Dgg/qE7r3q5iH9s9+GRR5M8h0k+osrj1WGo1+oiSRHN+107LBU=
---

# 极简取件码识别 APP（PickupCode）

一个**纯本地、极简**的安卓取件码识别应用：手动输入快递取件码后，打开相机**实时扫描**驿站货架上的快递面单，OCR 自动识别面单上的取件码并与目标码**容错匹配**，命中即高亮提示，帮你快速定位目标快递。

无广告、无联网请求、无数据上传，所有识别都在设备本地完成。

- 开发语言：Kotlin
- 最低系统：Android 8.0（API 26）
- 界面：Jetpack Compose + Material 3 Expressive
- 相机：CameraX（实时预览 + 逐帧分析）
- OCR：Google ML Kit 中文文本识别（离线模型）

---

## 一、功能一览

| 功能 | 说明 |
| --- | --- |
| 相机实时扫描 | 打开相机对着货架面单扫一圈，逐帧 OCR 自动匹配目标取件码 |
| 多取件码列表 | 一次可输入多个取件码，轮流扫描匹配，全部可复用保存 |
| OCR 容错匹配 | 0/O、1/I/L、5/S、8/B、2/Z、6/B/G、9/G/Q 等易混淆字符自动纠错，可在设置中开关 |
| 命中震动提醒 | 匹配到目标取件码时设备震动 + 界面高亮，无需一直盯着屏幕 |
| 相册识别 | 从相册选择面单照片离线识别取件码 |
| 已取件标记 | 命中后一键标记已取，列表状态一目了然 |
| 自动跳过空闲帧 | 未命中时按帧率间隔分析，省电省算力 |
| 纯本地运行 | 图片、识别结果不出设备，无需任何联网权限 |

---

## 二、项目结构

```
qjmsb/
├── settings.gradle.kts              # Gradle 设置（阿里云镜像 + Google/Maven 仓库）
├── build.gradle.kts                 # 根构建脚本（插件版本声明）
├── gradle.properties                # Gradle 全局配置
├── local.properties                 # 本机 SDK 路径（不入库）
├── gradlew.bat / gradlew            # Gradle Wrapper 脚本
├── gradle/
│   ├── libs.versions.toml           # 版本目录（所有依赖版本集中管理）
│   └── wrapper/                     # Wrapper 配置（Gradle 8.14.5）
├── README.md                        # 本说明文件
└── app/
    ├── build.gradle.kts             # app 模块构建脚本（SDK 版本、依赖）
    ├── proguard-rules.pro           # 混淆规则（未启用混淆）
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml  # 清单：权限、Activity
        │   ├── java/com/example/pickupcode/
        │   │   ├── MainActivity.kt             # 单 Activity 入口
        │   │   ├── PickupCodeExtractor.kt      # 取件码提取器（正则规则）
        │   │   ├── CodeMatcher.kt              # ★ OCR 容错匹配器（混淆字符纠错）
        │   │   ├── CodeRepository.kt           # 取件码本地存储（SharedPreferences）
        │   │   ├── receiver/SmsReceiver.kt     # 短信广播接收器
        │   │   └── ui/compose/
        │   │       ├── AppRoot.kt              # Compose 根导航
        │   │       ├── HomeScreen.kt           # ★ 主界面（取件码列表 + 相机实时扫描）
        │   │       ├── ResultScreen.kt         # 识别结果页
        │   │       ├── SettingsScreen.kt       # 设置页（容错开关等）
        │   │       └── theme/                  # Compose 主题（Color/Shape/Type/Theme）
        │   └── res/
        │       ├── values/                     # strings / colors / themes
        │       ├── xml/file_paths.xml          # FileProvider 路径声明
        │       ├── drawable/                   # 图标前景等
        │       └── mipmap-*/                   # 应用图标（adaptive icon）
        ├── test/                              # 单元测试
        └── androidTest/                       # 仪器测试
```

---

## 三、技术栈版本

| 组件 | 版本 |
| --- | --- |
| Kotlin | 2.0.21 |
| Android Gradle Plugin (AGP) | 8.7.3 |
| Gradle | 8.14.5 |
| compileSdk / minSdk / targetSdk | 35 / 26 / 34 |
| Jetpack Compose BOM | 2025.06.01 |
| Material 3 | 1.4.0-alpha04（Expressive） |
| CameraX | 1.3.4 |
| ML Kit 中文识别 | com.google.mlkit:text-recognition-chinese:16.0.1 |

---

## 四、环境要求

| 项目 | 要求 |
| --- | --- |
| Android Studio | 2024.1（Koala）及以上，建议最新稳定版 |
| JDK | 17+（Android Studio 自带 JBR，无需单独安装） |
| Android SDK | Platform 35（compileSdk = 35） |
| Gradle | 8.14.5（首次同步自动下载） |
| 真机 | Android 8.0 及以上（实时扫描依赖相机，建议真机测试） |

> 首次同步时 Gradle 会自动下载依赖（ML Kit、CameraX、Compose 等），项目已配置阿里云镜像加速，耗时取决于网络。

---

## 五、构建与运行

**方式一：Android Studio**

1. 打开项目：`File → Open` 选择项目根目录，等待 Gradle 同步完成。
2. 连接开启 USB 调试的真机（或创建 API 26+ 模拟器）。
3. 点击顶栏绿色三角形 ▶ 运行。

**方式二：命令行**

```powershell
# Windows（使用 Android Studio 自带 JDK）
$env:JAVA_HOME = "H:\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-<versionName>.apk`（如 `app-1.0.9.apk`）。

---

## 六、使用说明（新手向）

1. **添加取件码**：打开 App，在输入框填写短信/App 里的取件码，可连续添加多个。
2. **开始扫描**：点「相机扫描」，把手机对着驿站货架的面单区域缓慢移动，App 会逐帧 OCR 识别并自动匹配。
3. **命中提示**：识别到目标取件码时设备震动、界面高亮，点按即可查看/标记已取。
4. **相册识别**：若面单是照片，可从相册选择图片离线识别。
5. **标记已取**：取件完成后在列表点选标记，下次扫描自动跳过已取码。

---

## 七、权限说明

| 权限 | 用途 | 备注 |
| --- | --- | --- |
| CAMERA | 相机实时扫描面单 | 运行时弹窗申请 |
| READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE | 相册选图识别 | Android 13+ 使用系统 Photo Picker，无需存储权限 |

App 不申请联网权限，识别过程全程离线。

---

## 八、OCR 容错匹配说明

OCR 对相似字符经常误识别，本项目内置混淆字符映射表，开启容错后：

| 易混淆组 | 说明 |
| --- | --- |
| `0` ↔ `O` / `D` | 数字零与字母 O/D |
| `1` ↔ `I` / `L` | 数字一与字母 I/L |
| `5` ↔ `S` | 数字五与字母 S |
| `8` ↔ `B` | 数字八与字母 B |
| `2` ↔ `Z` | 数字二与字母 Z |
| `6` ↔ `B` / `G` | 数字六与字母 B/G |
| `9` ↔ `G` / `Q` | 数字九与字母 G/Q |
| `7` ↔ `T` | 数字七与字母 T |
| `4` ↔ `A` | 数字四与字母 A |

匹配规则：长度必须一致；忽略大小写；混淆字符差异不计入差异数；其余字符最多允许 1 处差异。可在设置页关闭容错，切换为严格精确匹配。

---

## 九、常见问题（FAQ）

| 问题 | 解决方案 |
| --- | --- |
| Gradle 同步很慢 / 卡住 | 项目已配阿里云镜像；若仍慢可挂代理后 `File → Sync Project with Gradle Files` 重试 |
| 扫描识别不到取件码 | 保持面单平整、光线充足、正对取件码区域；缓慢移动让面单在画面中停留 1-2 秒 |
| 提示「需要相机权限」 | 到系统设置确认「取件码识别 → 权限 → 相机」已开启 |
| 想改成自己的包名 | 全局搜索替换 `com.example.pickupcode`（含 settings 与 build 文件） |
| 模拟器无法使用相机 | 实时扫描依赖真实相机，建议使用真机测试 |

---

## 十、隐私说明

- 全程离线：无任何联网权限，图片与识别结果不出设备。
- 取件码仅保存在应用私有目录（SharedPreferences），卸载即清除。
- 开源仅用于学习交流，请勿用于非法用途。

*（内容由 AI 生成，仅供参考，请以实际代码为准）*
*（内容由AI生成，仅供参考）*
