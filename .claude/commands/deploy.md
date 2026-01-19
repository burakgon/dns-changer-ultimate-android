# Deploy Build to Telegram

This command commits all changes, pushes to GitHub, then builds and deploys to Telegram.

## Steps to execute:

### 1. Check for uncommitted changes
Run `git status` to see if there are any uncommitted changes.

### 2. If there are changes, commit them
- Stage all changes with `git add .`
- Create a commit with a descriptive message that summarizes what was changed
- Use good commit message conventions (imperative mood, explain the "what" and "why")
- Include `Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>` at the end

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
