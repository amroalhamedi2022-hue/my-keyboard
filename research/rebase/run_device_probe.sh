#!/usr/bin/env bash
set -euo pipefail

fixture=$(find fixture -name '*.apk' -print -quit)
test -f "$fixture"
args=(
    --candidate candidate-results/zero-mature-core-debug-evaluation.apk
    --fixture "$fixture"
    --out candidate-results/runtime
)
if [[ ${ZERO_ADAPTIVE_SETTINGS_SMOKE:-false} == true ]]; then
    args+=(--adaptive-settings)
fi

replay_dir=/tmp/zero-dictation
if [[ -f "$replay_dir/english.wav" ]]; then
    proto_root="$ANDROID_HOME/emulator/lib"
    replay_python="$replay_dir/venv/bin/python"
    "$replay_python" -m grpc_tools.protoc -I "$proto_root" \
        --python_out="$replay_dir/generated" "$proto_root/emulator_controller.proto"
    export PYTHONPATH="$replay_dir/generated"
    exec "$replay_python" research/rebase/device_probe.py "${args[@]}" --english-wav "$replay_dir/english.wav"
fi

exec python3 research/rebase/device_probe.py "${args[@]}"
