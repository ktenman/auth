rm -rf .gradle || true
rm -rf build || true
rm -rf .idea || true
rm -rf *.iml || true
rm -rf node_modules || true
rm -rf .kotlin || true
rm gradlew || true
rm LICENSE || true
rm package-lock.json || true
rm combined_files.txt || true
python3 code.py
git reset --hard
npm install
