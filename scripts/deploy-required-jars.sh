#!/usr/bin/env bash
set -euo pipefail

# Deploy only the jars required by NemakiWare to avoid WAR plugin/module issues
# on modern JDKs and to keep GitHub Packages contents focused.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

MODULES="\
chemistry-opencmis-commons/chemistry-opencmis-commons-api,\
chemistry-opencmis-commons/chemistry-opencmis-commons-impl,\
chemistry-opencmis-client/chemistry-opencmis-client-api,\
chemistry-opencmis-client/chemistry-opencmis-client-bindings,\
chemistry-opencmis-client/chemistry-opencmis-client-impl,\
chemistry-opencmis-server/chemistry-opencmis-server-support,\
chemistry-opencmis-server/chemistry-opencmis-server-bindings,\
chemistry-opencmis-test/chemistry-opencmis-test-tck"

echo "Deploying required modules to GitHub Packages..."
echo "Modules: ${MODULES}"

mvn -Pgithub-packages \
  -DskipTests \
  -DdeployAtEnd=true \
  -pl "${MODULES}" \
  -am \
  deploy "$@"
