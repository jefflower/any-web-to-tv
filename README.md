# 网页电视 (Any-Web-to-TV)

一个 **Android TV** 应用：把任意网页当成原生 app 来用。

- 在首页用一组「磁贴」管理常用网址
- 选中即全屏打开，遥控器按键操作
- 多页面可同时驻留，遥控器一键切换（类似浏览器多 Tab）
- 支持添加 / 编辑 / 删除书签，favicon 自动抓取

适合给家里电视上的 Bilibili 网页版、YouTube 网页版、各种自部署面板等做"伪原生"快捷入口。

## 遥控器操作

| 按键 | 行为 |
|---|---|
| OK / DPAD_CENTER | 选中并打开磁贴；切换 Tab 概览中的 Tab |
| 方向键 | 在磁贴 / 页面元素 / Tab 概览之间移动焦点；页面无可聚焦元素时上下滚动 |
| MENU | 在网页中唤出 Tab 概览（或关闭概览） |
| CHANNEL_UP / DOWN（或 PageUp/Down） | 在 Tab 之间快速切换 |
| BACK 短按 | 网页内：后退一步；无历史则关闭页面回到首页 |
| BACK 长按 | 任何时候：直接回到首页 |
| 磁贴上长按 OK | 弹出「打开 / 编辑 / 删除」菜单 |

## 技术栈

- Kotlin 2.0
- Android Gradle Plugin 8.7 / Gradle 8.9 / JDK 17 编译
- AndroidX Leanback Launcher Intent + 传统 View 系统
- Room 2.6 持久化书签
- OkHttp 抓 favicon
- minSdk 23 / target+compile 35

## 本地构建

需要：Android Studio + JDK 17（或 21）+ Android SDK 34/35。

```bash
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

将 APK 安装到 Android TV：

```bash
adb connect <TV-IP>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

或拷贝 APK 到 U 盘，用 TV 上的"文件管理器/X-plore"打开安装。

## 自动发布

打 tag 触发 GitHub Actions 自动构建并发布到 Releases：

```bash
git tag v0.1.0
git push origin v0.1.0
```

打开 `https://github.com/<you>/any-web-to-tv/releases` 下载 `app-debug.apk`。

## 已知限制

- **不支持 DRM 流媒体**（Netflix / Disney+ 等需要 Widevine L1，WebView 默认 L3，会被拒）
- 部分网站会按 UA 检测拒绝 WebView（少数银行 / 视频站）
- 多 Tab 上限 5；超过时按 LRU 销毁最久未用 Tab
- 当前 v1 用 debug 签名，仅适合 sideload

## 路线图

- v0.2：release keystore（GitHub Secrets）+ 支持每个书签强制桌面 UA 开关
- v0.3：Tab 截图缩略图（取代纯文字）
- v0.4：通过 Cast / DLNA 接收手机推过来的链接

## License

MIT
