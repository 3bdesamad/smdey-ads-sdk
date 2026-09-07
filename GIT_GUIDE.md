# 📘 Complete Git Workflow Guide for Beginners

This guide covers all Git commands you need to manage your project, make commits, merge branches, and publish releases.

---

## 🌳 Branch Structure Overview

* **`develop`**: Your everyday working branch. All new code, tests, and improvements go here first.
* **`main`**: Your stable production branch. Code here is tested, production-ready, and used by JitPack releases.

---

## 🛠️ Workflow 1: How to Commit & Push Your Work to `develop`

Use this every time you make code changes, fix bugs, or add features.

### Step 1: Check your current status
See which branch you are on and what files were modified:
```bash
git status
```

### Step 2: Ensure you are on the `develop` branch
If you are not on `develop`, switch to it:
```bash
git checkout develop
```

### Step 3: Stage all your changes
This prepares all modified, deleted, and new files to be committed:
```bash
git add .
```

### Step 4: Commit your changes with a message
Save a snapshot of your staged changes with a clear explanation:
```bash
git commit -m "feat: add your short description here"
```

> **Commit Message Prefixes (Best Practice):**
> * `feat:` for a new feature (e.g., `git commit -m "feat: add grace period support"`)
> * `fix:` for fixing a bug or layout issue (e.g., `git commit -m "fix: dialog full width layout"`)
> * `chore:` for updating build files or dependencies (e.g., `git commit -m "chore: update gradle"`)
> * `docs:` for modifying README or documentation (e.g., `git commit -m "docs: update install guide"`)

### Step 5: Push your commit to GitHub
Upload your changes to GitHub:
```bash
git push origin develop
```

---

## 🚀 Workflow 2: How to Merge `develop` into `main`

Use this when your changes on `develop` are tested, working, and ready to go into the production `main` branch.

### Step 1: Ensure all work on `develop` is committed and pushed
```bash
git status
```
*(Make sure it says: `nothing to commit, working tree clean`)*

### Step 2: Switch to the `main` branch
```bash
git checkout main
```

### Step 3: Pull the latest updates for `main`
Ensure your local `main` branch is completely up to date with GitHub:
```bash
git pull origin main
```

### Step 4: Merge `develop` into `main`
Bring all commits from `develop` into `main`:
```bash
git merge develop
```

### Step 5: Push the updated `main` to GitHub
```bash
git push origin main
```

### Step 6: Switch back to `develop` to continue your work
Always keep working on `develop` for daily tasks:
```bash
git checkout develop
```

---

## 🏷️ Workflow 3: Creating a Release Tag for JitPack

JitPack creates library builds whenever you publish a release tag on GitHub.

### Step 1: Switch to `main`
```bash
git checkout main
```

### Step 2: Create a tag with your version number
*(Replace `1.0.0` with the version you want to release, matching `ads/build.gradle`)*:
```bash
git tag -a 1.0.0 -m "Release v1.0.0"
```

### Step 3: Push the tag to GitHub
```bash
git push origin 1.0.0
```

### Step 4: Switch back to `develop`
```bash
git checkout develop
```

---

## 🛟 Workflow 4: Helpful Commands & Safety Fixes

### 1. View recent commit history
```bash
git log --oneline -n 5
```

### 2. Discard uncommitted changes in a specific file
If you made changes to a file and want to reset it back to how it was:
```bash
git restore path/to/filename.java
```

### 3. Discard ALL uncommitted changes in your workspace
⚠️ *Warning: This erases all unstaged edits since your last commit:*
```bash
git restore .
```

### 4. Check all branches (local and remote)
```bash
git branch -a
```

---

## 📋 Quick Cheat Sheet

| Action | Command Line |
| :--- | :--- |
| **Check what changed** | `git status` |
| **Stage everything** | `git add .` |
| **Commit staged changes** | `git commit -m "your message"` |
| **Push to develop** | `git push origin develop` |
| **Switch to main** | `git checkout main` |
| **Merge develop into current branch** | `git merge develop` |
| **Push to main** | `git push origin main` |
| **Switch to develop** | `git checkout develop` |
| **Create release tag** | `git tag -a 1.0.0 -m "Release 1.0.0"` |
| **Push release tag** | `git push origin 1.0.0` |
| **View last 5 commits** | `git log --oneline -n 5` |
