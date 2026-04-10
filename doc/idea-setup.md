# IntelliJ IDEA 开发环境配置指南

本文档适用于首次在本机使用 IntelliJ IDEA 开发本项目的开发者。

> [!IMPORTANT]
> 推荐使用 **IntelliJ IDEA Ultimate** 版本，内置数据库工具、前端支持等功能。  
> 社区版（Community Edition）缺少 Spring Boot 运行配置类型，部分功能不可用。

---

## 前置条件

在配置 IDEA 之前，请确保本地已安装：

- **SDKMAN**（管理 Java 和 Maven 版本）
- **Java 21**：`sdk install java 21.0.10-tem`
- **Maven 3.9.9**：`sdk install maven 3.9.9`
- **Docker**（用于启动 MySQL 和 Redis）：`docker compose up -d mysql redis`

---

## 一、打开项目

1. 启动 IDEA，选择 **Open**
2. 选择项目根目录（包含 `backend/`、`frontend/` 的那一层）
3. 等待 IDEA 自动识别 Maven 项目并下载依赖（右下角有进度条）

---

## 二、配置 JDK 21

`File > Project Structure`（快捷键 `⌘;` / `Ctrl+Alt+Shift+S`）

### 2.1 添加 SDK

1. 左侧选择 **SDKs**
2. 点击 `+` → **Add JDK...**
3. 路径选择 SDKMAN 安装的 Java 21：

   ```
   ~/.sdkman/candidates/java/21.0.10-tem
   ```

4. 点击 **OK**，Name 填写 `temurin-21`

### 2.2 设置项目 SDK

1. 左侧选择 **Project**
2. **SDK** 下拉选择 `temurin-21`
3. **Language level** 选择 `21`
4. 点击 **Apply** → **OK**

---

## 三、安装并配置 Lombok 插件

> [!IMPORTANT]
> Lombok 配置是**必须步骤**。项目中大量使用 `@Data`、`@Builder`、`@RequiredArgsConstructor` 等注解，缺少此配置会导致代码满屏报红、无法编译。

### 3.1 安装插件

`Settings`（`⌘,` / `Ctrl+Alt+S`）→ **Plugins** → **Marketplace** 标签页

搜索 `Lombok`，找到 **Lombok** by JetBrains，点击 **Install**，安装完成后**重启 IDEA**。

### 3.2 启用注解处理

`Settings` → **Build, Execution, Deployment** → **Compiler** → **Annotation Processors**

勾选 **Enable annotation processing**，其余保持默认：

| 选项 | 值 |
|------|-----|
| Enable annotation processing | ✅ 勾选 |
| Obtain processors from project classpath | ● 选中 |
| Store generated sources relative to | `Module output directory` |

点击 **Apply** → **OK**。

---

## 四、配置 Maven

`Settings` → **Build, Execution, Deployment** → **Build Tools** → **Maven**

| 选项 | 配置值 |
|------|--------|
| Maven home path | `~/.sdkman/candidates/maven/3.9.9` |
| User settings file | 默认（`~/.m2/settings.xml`） |
| Local repository | 默认（`~/.m2/repository`） |

> 若 IDEA 已自动识别 Maven Wrapper（`./mvnw`），此步骤可跳过。  
> 判断方式：Maven 面板中能正常看到项目的 Lifecycle 和 Dependencies。

---

## 五、使用预配置的启动项

项目已提交了 Run Configuration 文件（`.idea/runConfigurations/StarterApplication.xml`），IDEA 打开项目后会自动加载。

在工具栏右侧的运行配置下拉框中，可以直接看到 **StarterApplication**，点击绿色三角形即可启动。

启动配置说明：
- **Main class**：`com.music163.starter.StarterApplication`
- **Active profiles**：`dev`（对应 `application-dev.yml`）

启动成功后访问：
- API 文档：`http://localhost:8080/api/doc.html`
- 健康检查：`http://localhost:8080/api/auth/login`（POST）

---

## 六、配置数据库工具（可选）

> IDEA Ultimate 内置 Database 工具，可直接在 IDE 内浏览表结构和执行 SQL。

1. 右侧侧边栏点击 **Database** 图标
2. 点击 `+` → **Data Source** → **MySQL**
3. 填写连接信息：

   | 字段 | 值 |
   |------|-----|
   | Host | `localhost` |
   | Port | `3306` |
   | Database | `starter_db` |
   | User | `root` |
   | Password | `root123456` |

4. 点击 **Test Connection**，显示绿色 `Successful` 后点击 **OK**

> [!NOTE]
> 数据库需要先通过 Docker 启动：`docker compose up -d mysql`

---

## 七、推荐插件

以下插件可显著提升开发效率，建议安装：

| 插件名称 | 作用 | 安装优先级 |
|----------|------|-----------|
| **MyBatisX** | Mapper 接口 ↔ XML 文件双向跳转，SQL 补全 | 强烈推荐 |
| **Alibaba Java Coding Guidelines** | 项目指定的编码规范实时检查 | 推荐 |
| **.ignore** | `.gitignore` 文件语法高亮与管理 | 推荐 |
| **Rainbow Brackets** | 括号颜色区分，减少嵌套阅读难度 | 可选 |

安装方式：`Settings` → **Plugins** → **Marketplace** → 搜索插件名 → **Install**

---

## 八、代码风格

项目根目录有 `.editorconfig` 文件，IDEA 会自动读取并应用（无需手动设置）。

验证方式：`Settings` → **Editor** → **Code Style**，顶部若显示 _"EditorConfig overrides Code Style settings"_ 的提示，则表示已生效。

主要规范（`.editorconfig` 定义）：

| 文件类型 | 缩进 |
|----------|------|
| `*.java` | 4 空格 |
| `*.yml` / `*.yaml` | 2 空格 |
| `*.xml` | 4 空格 |
| `*.{js,ts,tsx,json}` | 2 空格 |

---

## 配置完成 Checklist

完成以上步骤后，逐项确认：

- [ ] 项目可正常打开，Maven 依赖已下载完成
- [ ] JDK 版本设置为 Java 21
- [ ] Lombok 插件已安装并重启 IDEA
- [ ] Annotation Processing 已启用
- [ ] 项目代码无红色错误（`@Data`、`@Builder` 等注解正常解析）
- [ ] 运行配置下拉框中可以看到 `StarterApplication`
- [ ] 点击启动，控制台出现 `Started StarterApplication` 日志
- [ ] 访问 `http://localhost:8080/api/doc.html` 页面正常显示
