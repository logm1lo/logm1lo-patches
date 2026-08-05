#!/usr/bin/env bash
set -euo pipefail
WORKDIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$WORKDIR"

VERSION="$(grep '^version=' gradle.properties | cut -d= -f2)"
REPO="logm1lo/logm1lo-patches"
VERSION_FILE="$WORKDIR/gradle.properties"
BUILD_DIR="$WORKDIR/patches/build/libs"

echo "==> Current version: $VERSION"

# Parse version and bump patch
MAJOR="${VERSION%%.*}"
REST="${VERSION#*.}"
MINOR="${REST%%.*}"
PATCH="${REST##*.}"
NEW_PATCH="$((PATCH + 1))"
NEW_VERSION="${MAJOR}.${MINOR}.${NEW_PATCH}"

echo "==> New version: $NEW_VERSION"

# Update gradle.properties
sed -i "s/^version=.*/version=${NEW_VERSION}/" "$VERSION_FILE"

# Build
echo "==> Building patches..."
./gradlew :patches:buildAndroid

MPP_FILE=$(ls -t "$BUILD_DIR"/patches-*.mpp 2>/dev/null | head -1)
if [ -z "$MPP_FILE" ]; then
    echo "ERROR: No .mpp file found in $BUILD_DIR"
    exit 1
fi
echo "==> Built: $MPP_FILE"

# Generate patches-list.json
echo "==> Generating patches-list.json..."
./gradlew :patches:generatePatchesList 2>&1 | tail -3

# Generate patches-bundle.json
echo "==> Generating patches-bundle.json..."
TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%S)"
TAG="v${NEW_VERSION}"
cat > patches-bundle.json << JSONEOF
{
  "created_at": "$TIMESTAMP",
  "description": "",
  "download_url": "https://github.com/$REPO/releases/download/$TAG/patches-${NEW_VERSION}.mpp",
  "signature_download_url": "",
  "version": "$NEW_VERSION"
}
JSONEOF

# Generate checksums
echo "==> Generating checksums..."
shasum -a 256 "$MPP_FILE" patches-list.json patches-bundle.json > SHA256SUMS.txt

# Stage files
echo "==> Committing version bump..."
git add "$VERSION_FILE" patches-list.json patches-bundle.json SHA256SUMS.txt
git commit -m "release: v${NEW_VERSION}"

# Tag and push
echo "==> Tagging ${TAG}..."
git tag -a "$TAG" -m "v${NEW_VERSION}"

echo "==> Pushing..."
git push origin dev
git push origin "$TAG"

# Create GitHub release
echo "==> Creating GitHub release..."
gh release create "$TAG" \
    --title "v${NEW_VERSION}" \
    --notes "## v${NEW_VERSION}\n\nPatches compiled and released." \
    "$MPP_FILE" \
    patches-list.json \
    patches-bundle.json \
    SHA256SUMS.txt

echo ""
echo "==> Release v${NEW_VERSION} complete"
echo "    Download: https://github.com/$REPO/releases/download/$TAG/patches-${NEW_VERSION}.mpp"
echo "    Source:   https://github.com/$REPO/releases/tag/$TAG"
echo "    Manager:  https://morphe.software/add-source?github=$REPO"
