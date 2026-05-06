# doc-server

基于 Spring Boot 3.5.10 的文档处理服务，提供：

- DOCX 模板填充（`{{变量名}}` 占位符，兼容旧版 `${变量名}`）
- Word（`.doc` / `.docx`）转 PDF
- Docker 镜像打包运行

## 技术栈

- Java 17
- Spring Boot 3.5.10
- Apache POI（模板填充）
- JODConverter + LibreOffice（Word 转 PDF）

## 本地启动

```bash
mvn clean package
java -jar target/doc_server.jar
```

服务默认端口：`8080`

若出现 `repo.maven.apache.org ... 403` 或 `present, but unavailable`：Maven 已把失败结果缓存在本地仓库。请先删掉对应目录再构建，例如：

```bash
rm -rf /Users/liguanglong/data/service/maven/mavenjar/org/springframework/boot/spring-boot-starter-parent/3.5.10
rm -rf /Users/liguanglong/data/service/maven/mavenjar/org/springframework/boot/spring-boot-maven-plugin/3.5.10
mvn -U clean package -DskipTests
```

本项目已提供 `.mvn/settings.xml`（Central 走阿里云镜像）与 `.mvn/maven.config`；在**项目根目录**执行 `mvn` 时会自动使用该 settings。若 IntelliJ 仍强制使用全局 `-s .../settings.xml`，请在 **Settings → Build → Maven → User settings file** 中临时改为本项目的 `.mvn/settings.xml`，或把其中的 `<mirrors>` 合并进你的全局 `settings.xml`。

## API

### 1) 模板填充

- URL: `POST /api/docs/fill-template`
- Content-Type: `multipart/form-data`
- 参数：
  - `template`: 模板文件（`.docx`）
  - `variables`: JSON 字符串，例如 `{"plateNum":"云A12345","ownerName":"张三"}`（与模板中 `{{plateNum}}`、`{{ownerName}}` 对应）

示例：

```bash
curl -X POST "http://localhost:8080/api/docs/fill-template" \
  -F "template=@./template.docx" \
  -F 'variables={"name":"张三","amount":"1000"}'
```

### 2) Word 转 PDF

- URL: `POST /api/docs/word-to-pdf`
- Content-Type: `multipart/form-data`
- 参数：
  - `file`: Word 文件（`.doc` 或 `.docx`）

示例：

```bash
curl -X POST "http://localhost:8080/api/docs/word-to-pdf" \
  -F "file=@./sample.docx" \
  --output converted.pdf
```

## Docker 打包与运行

构建镜像：

```bash
docker build -t doc-server:latest .
```

如需显式指定固定版本（默认已在 Dockerfile 锁定）：

```bash
docker build \
  --build-arg RUNTIME_IMAGE=eclipse-temurin:17.0.14_7-jre-jammy \
  --build-arg LIBREOFFICE_VERSION=1:7.3.7-0ubuntu0.22.04.10 \
  -t doc-server:latest .
```

运行容器：

```bash
docker run --rm -p 8080:8080 doc-server:latest
```

## 注意事项

- 模板填充当前是基础版替换逻辑，适合简单占位符场景。
- 复杂样式/跨 run 占位符（Word 内部拆分）可在后续迭代中增强。
- 转换能力依赖 LibreOffice；Docker 镜像中已安装 `fonts-noto-cjk` 与 `fontconfig`，避免 PDF 中文变成方框。
