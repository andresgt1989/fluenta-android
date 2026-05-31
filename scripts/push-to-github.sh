#!/bin/bash
TOKEN="${GITHUB_TOKEN:-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}"
cd /opt/fluenta-android
git remote set-url origin "https://${TOKEN}@github.com/andresgt1989/fluenta-android.git"
git add -A
if ! git diff --cached --quiet; then
  git commit -m "auto: sync from VPS $(date '+%Y-%m-%d %H:%M')"
fi
git push origin master && echo "Pushed OK — Android Studio: VCS → Git → Pull (Ctrl+T)"
