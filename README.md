<div align="center">
    <img alt="logo" width="102px" src="https://github.com/ichenhe/halo-twikoo/assets/10266066/10d1c1af-b6d3-45c3-ae54-cb74e3dbbe4d">
    <h1>Halo - Twikoo</h1>
    <p>将 <a href="https://twikoo.js.org/">Twikoo</a> 评论系统集成到 <a href="https://www.halo.run/">Halo</a>。</p>
    <p align="center">
        <a href="https://www.halo.run/store/apps/app-FgtLY"><img alt="Halo App Store" src="https://img.shields.io/badge/Halo-%E5%BA%94%E7%94%A8%E5%B8%82%E5%9C%BA-%230A81F5?style=flat-square&logo=appstore&logoColor=%23fff" /></a>
        <a href="//github.com/jeio258/halo-twikoo/releases"><img alt="GitHub Release" src="https://img.shields.io/github/v/release/jeio258/halo-twikoo?style=flat-square&logo=github" /></a>
        <a href="//github.com/jeio258/halo-twikoo/actions/workflows/ci.yaml"><img alt="GitHub Actions Workflow Status" src="https://img.shields.io/github/actions/workflow/status/jeio258/halo-twikoo/ci.yaml?style=flat-square&label=build" /></a>
        <a href="./LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/jeio258/halo-twikoo?style=flat-square" /></a>
    </p>
</div>

本插件理论上支持所有使用 Halo 默认评论系统的主题。

## ✅ 环境要求

| 项目 | 要求 |
| --- | --- |
| Halo | `>= 2.25.0` |
| JDK（仅构建需要） | `21` 或更高 |

## 🛠 构建

```bash
./gradlew clean build
```

构建产物位于 `build/libs/`，将 JAR 上传到 Halo 后台「插件」中启用即可。

> 若 `services.gradle.org` 下载缓慢或不可达，可将 `gradle/wrapper/gradle-wrapper.properties`
> 中的 `distributionUrl` 替换为国内镜像，例如
> `https://mirrors.cloud.tencent.com/gradle/gradle-8.14.5-bin.zip`。

## 📖 使用说明

### 激活插件

#### Halo v2.17+

安装并启用插件后，进入 Halo 后台「插件-扩展配置-评论组件」，选中本插件。

<img width="1591" alt="image" src="https://github.com/user-attachments/assets/de178382-50f6-4e4e-afd0-d1894dc0d48d">


#### < Halo v2.17

请禁用其他评论插件（例如「评论组件」）确保本插件生效


### 前端脚本地址

输入 Twikoo js 脚本的链接，通常使用默认格式即可，但请注意修改版本号，与后端匹配。

```
https://cdn.jsdelivr.net/npm/twikoo@1.7.19/dist/twikoo.min.js
                                    | 修改这里 |
```

> **关于 `twikoo.min.js` 与 `twikoo.all.min.js`**
>
> - `twikoo.min.js`（约 466 KB，默认）：**精简版**，不含腾讯云 cloudbase SDK。仅适用于 envId 为访问地址的场景（Vercel / 自托管）。
> - `twikoo.all.min.js`（约 754 KB）：**完整版**，内置 cloudbase SDK。若 envId 填的是腾讯云云开发环境 ID，必须使用此版本，否则评论无法加载（控制台会提示 `Please import cloudbase firstly`）。
>
> 若你的 envId 是腾讯云环境 ID，请把地址改为：
> `https://cdn.jsdelivr.net/npm/twikoo@1.7.19/dist/twikoo.all.min.js`

### envId

这里要填写的值根据后端 Twikoo 部署方式的不同而不同。

- 腾讯云云函数部署：腾讯云的 envId
- Vercel 部署：`https://xxx.vercel.app`
- 自托管部署：Twikoo 服务的访问地址 (`https://xxxx`)

其他部署方式请参考 Twikoo 的文档。
