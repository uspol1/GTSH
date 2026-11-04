The Android 13–Android 17 compatible version is expected to be released on 2026/12/10.
预计于2026/12/10发布安卓13~安卓17兼容版

# GTSH - AI Persistent Memory Architecture

Have you ever experienced this: you chat with an AI for a long time, but due to the "context length limit", it suddenly forgets what you talked about earlier?

GTSH is an Android architecture based on Jetpack Compose. Its core purpose is just one: **to give AI cross-window persistent memory.**

## Core Mechanisms
- **Tree-like Diary Compression**: Retains all original conversation locally, but when sending to the cloud, it automatically compresses the dialogue into a "diary-style" macro summary, greatly saving Tokens.
- **Subconscious RAG Retrieval**: When you mention past events, the system extracts keywords, first searching in the macro summary, then diving deep into the local original words (simulating DFS retrieval).
- **Holographic Visual Memory Tree**: Displays memory nodes in a topological graph form, intuitively showing the hierarchy and association of memories.

## Requirements
⚠️ This project uses the latest Android dependency libraries and **requires `compileSdk = 37`**. 
Please ensure your Android Studio is updated to the latest preview version and has the API 37 SDK installed, otherwise it will not compile.

## How to Run
1. Download the source code and open it with Android Studio.
2. Enter your API Key and Base URL in the "Settings" (default is DeepSeek).
3. Run and experience the chat with a memory tree.

## Roadmap
- [x] Cloud memory compression and retrieval
- [x] Visual memory tree
- [ ] On-device MNN local LLM integration (In development)

---

# GTSH - AI 持久化记忆架构

你是否遇到过：和 AI 聊了很长时间，结果因为“上下文长度限制”，它突然就忘记了你们之前说过的话？

GTSH 是一套基于 Jetpack Compose 的 Android 架构。它的核心目的只有一个：**让 AI 拥有跨窗口的持久化记忆。**

## 核心机制
- **树状日记体压缩**：本地保留所有的对话原话，但传给云端时，会自动将对话压缩成“日记体”的宏观总结，极大节省 Token。
- **潜意识 RAG 检索**：当你提到过去的事情时，系统会提取关键词，先在宏观总结中查找，再深潜到本地原话中（模拟 DFS 检索）。
- **全息可视化记忆树**：将记忆节点以拓扑图的形式展示，直观看到记忆的层级与关联。

## 环境要求
⚠️ 本项目使用了最前沿的 Android 依赖库，**要求 `compileSdk = 37`**。
请确保你的 Android Studio 更新到了最新预览版，并安装了 API 37 的 SDK 包，否则将无法编译。

## 如何运行
1. 下载源码，用 Android Studio 打开。
2. 在“设置”中填入你的 API Key 及 Base URL（默认接入 DeepSeek）。
3. 运行即可体验带记忆树的聊天。

## Roadmap (路线图)
- [x] 云端记忆压缩与检索
- [x] 树状记忆可视化
- [ ] 端侧 MNN 本地大模型接入 (开发中)

---
*This project is open-sourced under the Apache 2.0 License.*
*本项目采用 Apache 2.0 协议开源。*
