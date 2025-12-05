# GitHub Actions CI Setup Guide

This guide explains how to set up GitHub Actions CI for the Rule Engine project to display test status badges.

## What Was Added

1. **GitHub Actions Workflow**: `.github/workflows/ci.yml`
   - Runs on every push and pull request to main/master/develop branches
   - Sets up JDK 8 environment
   - Runs Maven tests
   - Generates code coverage reports
   - Uploads coverage to Codecov (optional)

2. **Badges in README**: Added CI and Codecov badges at the top of README.md

## Steps to Complete Setup

### 1. Update Badge URLs

In `README.md`, replace the placeholder `your-username` with your actual GitHub username and repository name:

```markdown
[![CI](https://github.com/YOUR_USERNAME/YOUR_REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/YOUR_REPO/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/YOUR_USERNAME/YOUR_REPO/branch/main/graph/badge.svg)](https://codecov.io/gh/YOUR_USERNAME/YOUR_REPO)
```

For example, if your repository is `https://github.com/johndoe/rule-engine`:
```markdown
[![CI](https://github.com/johndoe/rule-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/johndoe/rule-engine/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/johndoe/rule-engine/branch/main/graph/badge.svg)](https://codecov.io/gh/johndoe/rule-engine)
```

### 2. Push Changes to GitHub

```bash
git add .github/workflows/ci.yml README.md GITHUB_SETUP.md
git commit -m "Add GitHub Actions CI workflow and badges"
git push origin main
```

### 3. Enable GitHub Actions

1. Go to your GitHub repository
2. Click on the "Actions" tab
3. You should see the CI workflow running automatically
4. The badges in README.md will show the current build status

### 4. Optional: Set Up Codecov

If you want code coverage reporting:

1. Sign up at [codecov.io](https://codecov.io)
2. Connect your GitHub repository
3. The workflow will automatically upload coverage reports

## Workflow Details

The CI workflow:
- **Triggers**: Push/PR to main, master, or develop branches
- **Environment**: Ubuntu with JDK 8
- **Commands**:
  - `mvn test` - Run all tests
  - `mvn jacoco:report` - Generate coverage report
  - Upload coverage to Codecov

## Badge Meanings

- **Green CI badge**: All tests pass ✅
- **Red CI badge**: Some tests fail ❌
- **Yellow CI badge**: Tests are running 🔄
- **Gray CI badge**: No status available ❓

## Troubleshooting

1. **Workflow doesn't run**: Check that the file is in `.github/workflows/ci.yml` and the repository has Actions enabled
2. **Tests fail**: Check the Actions logs for error details
3. **Badge doesn't update**: GitHub badges can take a few minutes to update after workflow completion

## Customization

You can customize the workflow by editing `.github/workflows/ci.yml`:
- Change JDK version
- Add more test commands
- Modify branch triggers
- Add deployment steps
