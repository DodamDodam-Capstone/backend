#!/bin/sh
# 호스트 비밀 파일의 0700/0600 권한을 유지하며 같은 비루트 UID/GID로 로컬 앱을 실행한다.
set -eu
app_runtime_uid=$(id -u)
app_runtime_gid=$(id -g)
if [ "$app_runtime_uid" -le 0 ] || [ "$app_runtime_gid" -le 0 ]; then
    printf '%s\n' '로컬 full Compose는 루트가 아닌 사용자·그룹으로 실행해 주세요.' >&2
    exit 1
fi
exec env APP_RUNTIME_UID="$app_runtime_uid" APP_RUNTIME_GID="$app_runtime_gid" docker compose --profile full "$@"
