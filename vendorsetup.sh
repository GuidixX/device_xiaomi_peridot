# TMP fix for AIDL Fingerprint
cd frameworks/base
curl -s https://raw.githubusercontent.com/GuidixX/patches/refs/heads/main/touchfeature-calls.patch -s | git am
cd ../..
