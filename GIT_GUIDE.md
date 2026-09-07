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

## 🏷️ Workflow 3: Understanding Releases & JitPack Tags

### 🤔 What is a "Release Tag"?
Think of a **Tag** as a permanent stamp or bookmark on a specific commit in your `main` branch. 

When you push a tag like `1.0.1` to GitHub:
1. GitHub creates a **Release snapshot** of your repository at that exact moment.
2. **JitPack** automatically detects that tag, compiles your Android SDK `.aar` library, and serves it to anyone who writes:
   ```groovy
   implementation 'com.github.3bdesamad:smdey-ads-sdk:1.0.1'
   ```
3. ⚠️ **Permanent Cache Rule**: JitPack permanently caches version numbers. Once a version (e.g. `1.0.0`) is built on JitPack, you cannot overwrite its code. Whenever you add new features or bug fixes, you **increment the version number** (e.g., `1.0.0` ➔ `1.0.1` ➔ `1.0.2`).

---

### 🔢 How Version Numbers Work (Semantic Versioning)
Version numbers follow: `MAJOR . MINOR . PATCH` (e.g., `1 . 0 . 1`)
* **Patch (`1.0.0` ➔ `1.0.1`)**: For bug fixes, layout tweaks, or small non-breaking updates.
* **Minor (`1.0.1` ➔ `1.1.0`)**: For adding new features (like Grace Period or a new ad format).
* **Major (`1.1.0` ➔ `2.0.0`)**: For major redesigns or breaking changes to SDK method names.

---

### 🚀 Step-by-Step: How to Publish a New Version (e.g., `1.0.1`)

#### Step 1: Update the version number in your project files
1. Open `ads/build.gradle` and change the version under `publishing`:
   ```groovy
   version = '1.0.1'
   ```
2. Open `app/build.gradle` and bump `versionCode` and `versionName`:
   ```groovy
   versionCode 2
   versionName "1.0.1"
   ```
3. Open `README.md` and update the dependency snippet to `1.0.1`.

#### Step 2: Commit and push changes to `develop`
```bash
git checkout develop
git add .
git commit -m "chore: bump version to 1.0.1"
git push origin develop
```

#### Step 3: Merge into `main` and push
```bash
git checkout main
git pull origin main
git merge develop
git push origin main
```

#### Step 4: Create and push the Git Tag
You can do this using either **Method A (Terminal)** or **Method B (GitHub Website)**:

##### 💻 Method A: Using Terminal Commands (Fastest)
```bash
# 1. Create the annotated tag
git tag -a 1.0.1 -m "1.0.1"

# 2. Push the tag to GitHub
git push origin 1.0.1

# 3. Switch back to develop for everyday coding
git checkout develop
```

##### 🌐 Method B: Using the GitHub Website (Easiest for Beginners)
1. Open your browser and go to your GitHub repository:  
   `https://github.com/3bdesamad/smdey-ads-sdk`
2. On the right-hand sidebar, click **"Releases"** (or click **"Create a new release"**).
3. Click the button **"Draft a new release"**.
4. Click **"Choose a tag"**, type `1.0.1`, and click **"+ Create new tag: 1.0.1 on main"**.
5. Set the Title: `1.0.1`.
6. Click **"Generate release notes"** (GitHub will automatically list all commits since the last release).
7. Click the green button **"Publish release"**.

#### Step 5: Verify the Build on JitPack
1. Open [jitpack.io](https://jitpack.io) in your browser.
2. In the search box, paste your repo: `3bdesamad/smdey-ads-sdk` and click **"Look up"**.
3. You will see `1.0.1` appear in the releases list.
4. Click the **"Get it"** button. JitPack will begin compiling the library.
5. In ~1 to 2 minutes, the status icon will turn **Green**. Once it is green, your new version is live for anyone around the world!

---

### 🔄 What if you want to update tag `1.0.0` to the latest commit instead?
If you already pushed tag `1.0.0` previously on an older commit and want to re-point `1.0.0` to the latest commit:
```bash
# 1. Switch to main
git checkout main

# 2. Delete the old tag locally
git tag -d 1.0.0

# 3. Delete the old tag from GitHub
git push origin --delete 1.0.0

# 4. Re-create the tag on your current latest commit
git tag -a 1.0.0 -m "1.0.0"

# 5. Push the new tag to GitHub
git push origin 1.0.0

# 6. Switch back to develop
git checkout develop
```
*(Note: If JitPack already built the old 1.0.0, bumping to `1.0.1` is cleaner and avoids JitPack cache issues).*

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
| **Create release tag** | `git tag -a 1.0.1 -m "1.0.1"` |
| **Push release tag** | `git push origin 1.0.1` |
| **View last 5 commits** | `git log --oneline -n 5` |
