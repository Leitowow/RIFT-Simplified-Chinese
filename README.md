### RIFT Intel Fusion Tool

RIFT是一个EVE Online的第三方工具。
本项目是RIFT的汉化版，由Leito维护。但在功能性上与原版并未完全一致。删除掉了部分功能，并添加了部分功能。
此版本不会与RIFT进行同步更新，但会在主体功能上尽量保持与RIFT的版本同步。

### 联系方式

- 游戏内：Leito Arthur
- QQ：15667921

### 下载地址

在[Releases](https://github.com/Leitowow/RIFT-Simplified-Chinese/releases)中下载最新版本。

### 构建方法

运行项目，执行：`./gradlew run`。这将下载所有依赖，构建并运行项目。

### QQMonitor API 同步说明

QQMonitor 对外校验接口 `/qqmonitor/api/v1/verify-qq/` 已同步新增返回字段：

- `main_account_id`：该 QQ 号码关联的主账户ID（未命中为 `null`）
- `nickname`：该 QQ 号码关联昵称（未命中为 `null`）

### 工具

本项目使用[IntelliJ IDEA](https://www.jetbrains.com/idea/)开发。
