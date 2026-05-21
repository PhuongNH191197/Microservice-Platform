"""Run this once after clone / proto changes to generate gRPC stubs."""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

subprocess.check_call([
    sys.executable, "-m", "grpc_tools.protoc",
    f"-I{ROOT / 'proto'}",
    f"--python_out={ROOT / 'generated'}",
    f"--grpc_python_out={ROOT / 'generated'}",
    str(ROOT / "proto" / "ai_media.proto"),
])
print("Stubs generated in generated/")
