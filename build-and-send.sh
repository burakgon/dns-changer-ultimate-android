#!/bin/bash

# Telegram Config
BOT_TOKEN="8529670074:AAGYueFwTdM0Y_14w6BKhNWXasxIuclGZFY"
CHAT_ID="-4891111441"

# Set JAVA_HOME for macOS
export JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}🔨 DNS Changer - Build & Deploy${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Build the APK
echo -e "\n${YELLOW}📦 Building APK...${NC}"
./gradlew assembleDebug --quiet

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed!${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Build successful!${NC}"

# Get version info
VERSION_CODE=$(grep "versionCode = " app/build.gradle.kts | head -1 | sed 's/[^0-9]*//g')
VERSION_NAME=$(grep "versionName = " app/build.gradle.kts | head -1 | sed 's/.*"\(.*\)".*/\1/')

echo -e "${CYAN}   Version: ${VERSION_NAME} (${VERSION_CODE})${NC}"

# Find APK
APK_PATH="app/build/outputs/apk/debug/DNSChanger-${VERSION_NAME}-debug.apk"

if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}❌ APK not found at $APK_PATH${NC}"
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo -e "${CYAN}   APK Size: ${APK_SIZE}${NC}"

# Get app icon as base64
ICON_PATH="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png"
ICON_BASE64=$(base64 -i "$ICON_PATH" | tr -d '\n')

# Get last build tag (excluding current version)
LAST_TAG=$(git tag -l "build-*" --sort=-v:refname | grep -v "build-${VERSION_CODE}" | head -1)

echo -e "\n${YELLOW}📝 Generating changelog...${NC}"

# Generate HTML changelog
CHANGELOG_HTML="build/changelog-${VERSION_CODE}.html"
mkdir -p build

# Determine commit range
if [ -n "$LAST_TAG" ]; then
    COMMIT_RANGE="$LAST_TAG..HEAD"
    FROM_VERSION=$(echo "$LAST_TAG" | sed 's/build-//')
    echo -e "${CYAN}   Range: ${FROM_VERSION} → ${VERSION_CODE}${NC}"
else
    COMMIT_RANGE=""
    FROM_VERSION="initial"
    echo -e "${CYAN}   Range: All commits (no previous build tag)${NC}"
fi

# Count commits
if [ -z "$COMMIT_RANGE" ]; then
    COMMIT_COUNT=$(git rev-list --count HEAD 2>/dev/null || echo "0")
else
    COMMIT_COUNT=$(git rev-list --count $COMMIT_RANGE 2>/dev/null || echo "0")
fi

echo -e "${CYAN}   Commits: ${COMMIT_COUNT}${NC}"

# Create HTML
cat > "$CHANGELOG_HTML" << HTMLHEAD
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DNS Changer Changelog v${VERSION_NAME}</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-primary: #0d1117;
            --bg-secondary: #161b22;
            --bg-tertiary: #21262d;
            --border: #30363d;
            --text-primary: #e6edf3;
            --text-secondary: #8b949e;
            --text-muted: #6e7681;
            --accent: #f0b232;
            --accent-subtle: rgba(240, 178, 50, 0.15);
            --green: #3fb950;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            background: var(--bg-primary);
            color: var(--text-primary);
            line-height: 1.6;
            min-height: 100vh;
        }

        .header {
            background: linear-gradient(180deg, var(--bg-secondary) 0%, var(--bg-primary) 100%);
            border-bottom: 1px solid var(--border);
            padding: 48px 20px;
            text-align: center;
        }

        .logo {
            width: 80px;
            height: 80px;
            border-radius: 20px;
            margin-bottom: 20px;
            box-shadow: 0 8px 32px rgba(240, 178, 50, 0.3);
        }

        .app-name {
            font-size: 32px;
            font-weight: 700;
            color: var(--text-primary);
            margin-bottom: 12px;
            letter-spacing: -0.5px;
        }

        .tagline {
            color: var(--text-secondary);
            font-size: 15px;
            margin-bottom: 20px;
        }

        .version-badge {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            background: var(--accent-subtle);
            border: 1px solid var(--accent);
            color: var(--accent);
            padding: 10px 20px;
            border-radius: 50px;
            font-size: 15px;
            font-weight: 600;
        }

        .stats {
            display: flex;
            justify-content: center;
            gap: 40px;
            margin-top: 32px;
            padding-top: 24px;
            border-top: 1px solid var(--border);
        }

        .stat {
            text-align: center;
        }

        .stat-value {
            font-size: 28px;
            font-weight: 700;
            color: var(--accent);
        }

        .stat-label {
            font-size: 12px;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-top: 4px;
        }

        .container {
            max-width: 800px;
            margin: 0 auto;
            padding: 40px 20px;
        }

        .section-title {
            font-size: 13px;
            font-weight: 600;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 1.5px;
            margin-bottom: 20px;
        }

        .commit {
            background: var(--bg-secondary);
            border: 1px solid var(--border);
            border-radius: 12px;
            margin-bottom: 12px;
            overflow: hidden;
            transition: all 0.2s ease;
        }

        .commit:hover {
            border-color: var(--accent);
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
        }

        .commit-header {
            padding: 16px 20px;
            background: var(--bg-tertiary);
        }

        .commit-title {
            font-size: 15px;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 10px;
            line-height: 1.5;
        }

        .commit-meta {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 12px;
            font-size: 12px;
            color: var(--text-muted);
        }

        .commit-meta span {
            display: flex;
            align-items: center;
            gap: 5px;
        }

        .commit-body {
            padding: 16px 20px;
            color: var(--text-secondary);
            font-size: 14px;
            border-top: 1px solid var(--border);
        }

        .commit-body ul {
            margin: 0;
            padding-left: 20px;
            list-style-type: none;
        }

        .commit-body li {
            margin-bottom: 8px;
            position: relative;
            padding-left: 16px;
        }

        .commit-body li::before {
            content: "→";
            position: absolute;
            left: 0;
            color: var(--accent);
        }

        .commit-body p {
            margin-bottom: 10px;
        }

        .hash {
            font-family: 'JetBrains Mono', monospace;
            font-size: 11px;
            background: var(--bg-primary);
            padding: 4px 8px;
            border-radius: 6px;
            color: var(--text-secondary);
        }

        .empty-state {
            text-align: center;
            padding: 80px 20px;
            color: var(--text-muted);
        }

        .empty-state .icon {
            font-size: 64px;
            margin-bottom: 20px;
            opacity: 0.5;
        }

        .footer {
            text-align: center;
            padding: 32px 20px;
            color: var(--text-muted);
            font-size: 12px;
            border-top: 1px solid var(--border);
        }

        .footer a {
            color: var(--accent);
            text-decoration: none;
        }
    </style>
</head>
<body>
    <div class="header">
        <img src="data:image/png;base64,${ICON_BASE64}" alt="DNS Changer" class="logo">
        <div class="app-name">DNS Changer Ultimate</div>
        <div class="tagline">Secure & Private DNS</div>
        <div class="version-badge">
            <span>📦</span>
            <span>Version ${VERSION_NAME}</span>
        </div>
        <div class="stats">
            <div class="stat">
                <div class="stat-value">${COMMIT_COUNT}</div>
                <div class="stat-label">Commits</div>
            </div>
            <div class="stat">
                <div class="stat-value">${FROM_VERSION}</div>
                <div class="stat-label">Previous</div>
            </div>
            <div class="stat">
                <div class="stat-value">${VERSION_CODE}</div>
                <div class="stat-label">Current</div>
            </div>
        </div>
    </div>
    <div class="container">
        <div class="section-title">What's New</div>
HTMLHEAD

# Process commits
if [ "$COMMIT_COUNT" -gt 0 ]; then
    if [ -z "$COMMIT_RANGE" ]; then
        GIT_LOG_CMD="git log --pretty=format:'COMMIT_START%n%H%n%h%n%s%n%an%n%ar%n%b%nCOMMIT_END' --no-merges"
    else
        GIT_LOG_CMD="git log $COMMIT_RANGE --pretty=format:'COMMIT_START%n%H%n%h%n%s%n%an%n%ar%n%b%nCOMMIT_END' --no-merges"
    fi

    eval $GIT_LOG_CMD | while IFS= read -r line; do
        if [ "$line" = "COMMIT_START" ]; then
            read -r FULL_HASH
            read -r SHORT_HASH
            read -r SUBJECT
            read -r AUTHOR
            read -r DATE

            BODY=""
            while IFS= read -r bodyline; do
                [ "$bodyline" = "COMMIT_END" ] && break
                [ -n "$bodyline" ] && BODY="${BODY}${bodyline}\n"
            done

            SUBJECT_ESC=$(echo "$SUBJECT" | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g')

            echo "        <div class=\"commit\">" >> "$CHANGELOG_HTML"
            echo "            <div class=\"commit-header\">" >> "$CHANGELOG_HTML"
            echo "                <div class=\"commit-title\">${SUBJECT_ESC}</div>" >> "$CHANGELOG_HTML"
            echo "                <div class=\"commit-meta\">" >> "$CHANGELOG_HTML"
            echo "                    <span>👤 ${AUTHOR}</span>" >> "$CHANGELOG_HTML"
            echo "                    <span>🕐 ${DATE}</span>" >> "$CHANGELOG_HTML"
            echo "                    <span class=\"hash\">${SHORT_HASH}</span>" >> "$CHANGELOG_HTML"
            echo "                </div>" >> "$CHANGELOG_HTML"
            echo "            </div>" >> "$CHANGELOG_HTML"

            if [ -n "$BODY" ]; then
                # Filter out Co-Authored-By lines and format
                BODY_CLEAN=$(echo -e "$BODY" | grep -v "^Co-Authored-By:" | grep -v "^$")

                if [ -n "$BODY_CLEAN" ]; then
                    echo "            <div class=\"commit-body\">" >> "$CHANGELOG_HTML"
                    echo "                <ul>" >> "$CHANGELOG_HTML"

                    echo "$BODY_CLEAN" | while IFS= read -r bline; do
                        # Remove leading "- " or "• " if present
                        bline_clean=$(echo "$bline" | sed 's/^- //; s/^• //')
                        bline_esc=$(echo "$bline_clean" | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g')
                        [ -n "$bline_esc" ] && echo "                    <li>${bline_esc}</li>" >> "$CHANGELOG_HTML"
                    done

                    echo "                </ul>" >> "$CHANGELOG_HTML"
                    echo "            </div>" >> "$CHANGELOG_HTML"
                fi
            fi

            echo "        </div>" >> "$CHANGELOG_HTML"
        fi
    done
else
    cat >> "$CHANGELOG_HTML" << 'EMPTY'
        <div class="empty-state">
            <div class="icon">📭</div>
            <p>No changes since last build</p>
        </div>
EMPTY
fi

# Close HTML
cat >> "$CHANGELOG_HTML" << HTMLFOOT
    </div>
    <div class="footer">
        Generated on $(date '+%B %d, %Y at %H:%M') · DNS Changer Ultimate
    </div>
</body>
</html>
HTMLFOOT

echo -e "${GREEN}✅ Changelog created${NC}"

# Send to Telegram
echo -e "\n${YELLOW}📤 Sending to Telegram...${NC}"

# Send APK
RESPONSE=$(curl -s -X POST "https://api.telegram.org/bot${BOT_TOKEN}/sendDocument" \
    -F chat_id="$CHAT_ID" \
    -F document=@"$APK_PATH" \
    -F caption="🚀 *DNS Changer Ultimate* v${VERSION_NAME}
📦 Build ${VERSION_CODE} • ${APK_SIZE}" \
    -F parse_mode="Markdown")

if echo "$RESPONSE" | grep -q '"ok":true'; then
    echo -e "${GREEN}✅ APK sent!${NC}"
else
    echo -e "${RED}❌ Failed to send APK${NC}"
    echo "$RESPONSE"
    exit 1
fi

# Send changelog
RESPONSE=$(curl -s -X POST "https://api.telegram.org/bot${BOT_TOKEN}/sendDocument" \
    -F chat_id="$CHAT_ID" \
    -F document=@"$CHANGELOG_HTML" \
    -F caption="📋 Changelog: ${COMMIT_COUNT} commits (${FROM_VERSION} → ${VERSION_CODE})")

if echo "$RESPONSE" | grep -q '"ok":true'; then
    echo -e "${GREEN}✅ Changelog sent!${NC}"
else
    echo -e "${RED}❌ Failed to send changelog${NC}"
fi

# Tag this build
git tag -f "build-${VERSION_CODE}" 2>/dev/null
echo -e "${GREEN}🏷️  Tagged as build-${VERSION_CODE}${NC}"

echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ All done!${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
