# GitHub 上传操作指南

以下命令在项目根目录执行：

```powershell
cd E:\javacode\daily
```

## 1. 清理本地构建产物

```powershell
.\clean-artifacts.bat
```

如果你还想保留本地已打包好的安装包，可以不执行这一步；`.gitignore` 已经会排除这些文件。

## 2. 检查 Git 状态

```powershell
git status
```

如果提示 `git` 不存在，需要先安装 Git for Windows，并重新打开终端。

## 3. 如果大文件已经被 Git 跟踪，先移出索引

只从 Git 索引移除，不删除本地文件：

```powershell
git rm -r --cached target dist dist-app tools
git rm --cached installer-run.log daily.iml
```

如果某些路径提示不存在或未跟踪，可以忽略。

## 4. 添加源码和文档

```powershell
git add .gitignore README.md GITHUB_UPLOAD.md pom.xml package-windows.bat run-dev.bat test.bat clean-artifacts.bat src
```

如果你也想上传开发计划文档：

```powershell
git add PLANS.md AGENTS.md
```

## 5. 提交

```powershell
git commit -m "Prepare Daily Cost Calculator for GitHub"
```

## 6. 连接 GitHub 仓库

在 GitHub 创建空仓库后，把下面地址替换成你的仓库地址：

```powershell
git branch -M main
git remote add origin https://github.com/<your-name>/<repo-name>.git
git push -u origin main
```

如果已经添加过 `origin`：

```powershell
git remote set-url origin https://github.com/<your-name>/<repo-name>.git
git push -u origin main
```

## 7. 上传 release 安装包

安装包不要提交进 Git 仓库。需要发布 exe/msi 时，在 GitHub 仓库页面创建 Release，然后手动上传：

- `dist/DailyCostCalculator-Setup.exe`
- `dist/DailyCostCalculator-Setup.msi`
- `dist/Install-to-D-daily.bat`

## 8. 推荐提交前检查

```powershell
git status
git diff --cached --stat
```

确认不要出现这些文件：

- `.exe`
- `.msi`
- `.zip`
- `target/`
- `dist/`
- `dist-app/`
- `tools/`
- `.idea/`
- `*.iml`
- `*.log`
