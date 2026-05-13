#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="9.4.1"
DIST_NAME="gradle-${GRADLE_VERSION}-bin"
UNPACKED_NAME="gradle-${GRADLE_VERSION}"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_DIR="${GRADLE_USER_HOME}/wrapper/dists/${DIST_NAME}"
GRADLE_BIN="${DIST_DIR}/${UNPACKED_NAME}/bin/gradle"
ZIP_FILE="${DIST_DIR}/${DIST_NAME}.zip"

if [[ ! -x "${GRADLE_BIN}" ]]; then
  mkdir -p "${DIST_DIR}"
  if [[ ! -f "${ZIP_FILE}" ]]; then
    curl -fL "https://services.gradle.org/distributions/${DIST_NAME}.zip" -o "${ZIP_FILE}"
  fi
  unzip -q -o "${ZIP_FILE}" -d "${DIST_DIR}"
fi

exec "${GRADLE_BIN}" "$@"
