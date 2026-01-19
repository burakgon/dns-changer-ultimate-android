# Deploy Build to Telegram

This command commits all changes, pushes to GitHub, then builds and deploys to Telegram.

## Steps to execute:

### 1. Check for uncommitted changes
Run `git status` and `git diff --stat` to see what files have changed.

### 2. Make proper commits (if there are changes)
Group related changes into separate commits:
- **Feature/fix commits**: Each feature or bug fix should be its own commit
- **Config changes**: Separate commit for config/settings changes
- **Build script changes**: Separate commit for CI/build tooling
- **Documentation**: Separate commit for docs/readme changes

For each logical group:
1. Stage only the related files with `git add <specific-files>`
2. Write a clear, descriptive commit message explaining what and why
3. Include `Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>` at the end

Do NOT bundle unrelated changes into a single commit.

### 3. Push to GitHub
Run `git push origin main` to push all commits.

### 4. Run the build and deploy script
```bash
./build-and-send.sh
```

This script will:
- Auto-increment the version number
- Build the debug APK
- Generate HTML changelog with commits since last build tag
- Send APK and changelog to Telegram
- Create a git tag for the build

### 5. Report results
After completion, report:
- Version number
- Number of commits in changelog
- Confirmation that APK was sent to Telegram
