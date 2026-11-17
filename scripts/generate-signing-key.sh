#!/usr/bin/env bash
set -euo pipefail

KEYSTORE_FILE="${1:-my-upload-key.jks}"

if [ -f "$KEYSTORE_FILE" ]; then
  echo "ERROR: $KEYSTORE_FILE already exists! Never overwrite your permanent signing key."
  exit 1
fi

echo "=== My Store Permanent Signing Key Generator ==="
echo "Keep the generated keystore file and passwords backed up safely offline."
echo "NEVER commit $KEYSTORE_FILE to GitHub!"
echo ""

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_FILE" \
  -alias upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000

echo ""
echo "=== Signing Certificate Fingerprint ==="
keytool -list -v -keystore "$KEYSTORE_FILE" -alias upload | grep -E "SHA1:|SHA256:"

echo ""
echo "=== Base64 for GitHub Actions Secret (ANDROID_KEYSTORE_BASE64) ==="
base64 -w 0 "$KEYSTORE_FILE" 2>/dev/null || base64 "$KEYSTORE_FILE"
echo ""
