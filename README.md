# NekoBox for Android (Modified Version)

> **致敬 / Tribute**

> 本项目是基于原版 [NekoBox for Android](https://github.com/MatsuriDayo/NekoBoxForAndroid) (作者 MatsuriDayo) 的三次修改版。感谢原作者对开源社区的巨大贡献！  
>  This project is a modified version based on the original [NekoBox for Android](https://github.com/MatsuriDayo/NekoBoxForAndroid) created by MatsuriDayo.  

> 本项目是基于二改版 [NekoBox for Android](https://github.com/starifly/NekoBoxForAndroid) (作者 starifly) 的二次修改版  
This project is a modified version based on the original [NekoBox for Android](https://github.com/starifly/NekoBoxForAndroid) created by starifly.

## 本次修改版的主要特性 / Key Improvements in this Version

1. **界面全面美化 (Material 3 UI Beautification)**
   - 对节点列表卡片等 UI 细节进行了全面重写与升级。引入了 Material 3 现代风格的 16dp 大圆角与柔和阴影，让应用整体视觉感受更加年轻、现代。
2. **中转链路与 GeoIP 深度检测 (Transit Node & GeoIP Detection)**
   - 深度改造了底层的 `libcore` (Go) 以及安卓服务端的测试逻辑。现在在测试节点时，可以智能探测“入站 IP”与“实际出口 IP”，并结合内置的 `ipinfo.io` 获取真实物理位置。
   - 无论是进行“批量延迟测试”还是“单节点连接测试”，都能直接在界面上为您呈现透明的 IP 链路，例如：`路由: 日本(入站) → 中转 → 美国(出口)`。

## 免责声明 / Disclaimer

> 免责声明：本项目仅用于技术研究与代码学习之目的，不提供任何形式的网络代理服务。请勿将本项目用于违反当地法律法规的任何活动。请勿在生产环境中使用本项目，使用者应自行承担使用本项目可能带来的全部风险。若您下载或引用本项目，请在 24 小时内自行删除相关内容，并避免长期存储、分享或传播本项目的任何部分。**作者保留随时修改、更新或移除本项目及其内容的权利，恕不另行通知。**
> 
> Disclaimer: This project is intended solely for technical research and code learning purposes and does not provide any form of network proxy service. Please do not use this project for any activities that violate local laws and regulations. Do not use this project in production environments. Users are fully responsible for any risks that may arise from using this project. If you download or reference this project, please delete all related content within 24 hours and avoid long-term storage, distribution, or dissemination of any part of this project. **The author reserves the right to modify, update, or remove any part of this project or its contents at any time without prior notice.**

## 重要提醒 / Important Notice

**Google Play 版本自 2024 年 5 月起已被第三方控制，为非开源版本，请不要下载。**

**The Google Play version has been controlled by a third party since May 2024 and is a non-open source version. Please do not download it.**

## Credits (Original upstream)

Core:
- [SagerNet/sing-box](https://github.com/SagerNet/sing-box)

Android GUI:
- [shadowsocks/shadowsocks-android](https://github.com/shadowsocks/shadowsocks-android)
- [SagerNet/SagerNet](https://github.com/SagerNet/SagerNet)
