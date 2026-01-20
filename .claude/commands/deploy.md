# Deploy Build to Telegram

This command bumps the version, commits all changes, pushes to GitHub, then builds and deploys to Telegram.

## Steps to execute:

### 1. Check for uncommitted changes
Run `git status` and `git diff --stat` to see what files have changed.

### 2. Bump the version number
Before committing, increment the version in `app/build.gradle.kts`:
- Find `versionCode = XXXXX` and increment by 1
- Find `versionName = "XXXXX"` and update to match the new versionCode

Example: If current version is `10008`, change both to `10009`.

### 3. Make proper commits (if there are changes)
Group related changes into separate commits:
- **Feature/fix commits**: Each feature or bug fix should be its own commit
- **Config changes**: Separate commit for config/settings changes
- **Build script changes**: Separate commit for CI/build tooling
- **Documentation**: Separate commit for docs/readme changes
- **Version bump**: Include the version bump with the related feature commit, or as a separate commit if deploying existing changes

For each logical group:
1. Stage only the related files with `git add <specific-files>`
2. Write a clear, descriptive commit message explaining what and why
3. Include `Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>` at the end

Do NOT bundle unrelated changes into a single commit.

### 4. Push to GitHub
Run `git push origin main` to push all commits.

### 5. Generate build summary
Create a file `build/summary.txt` with a brief summary for testers:

```
📦 What's New:
- [List main changes in 2-3 bullet points]

🧪 What to Test:
- [List specific things to test based on changes]
- [Include any edge cases or specific flows to verify]
```

Keep it concise and actionable. Focus on user-facing changes and what testers should verify.

### 6. Run the build and deploy script
```bash
./build-and-send.sh
```

This script will:
- Build the debug APK
- Generate HTML changelog with commits since last build tag
- Send APK, changelog, and summary to Telegram
- Create a git tag for the build

### 7. Report results
After completion, report:
- Version number
- Number of commits in changelog
- Confirmation that APK was sent to Telegram
