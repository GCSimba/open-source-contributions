"""Print usage from /v1/chat/completions and /v1/chat/completions/batch
for the same conversation on a Qwen3 reasoning server (CPU evidence)."""
import json

import httpx

from tests.utils import RemoteOpenAIServer

MODEL = "Qwen/Qwen3-0.6B"
ARGS = ["--reasoning-parser", "qwen3", "--max-model-len", "2048", "--enforce-eager"]
MESSAGES = [{"role": "user", "content": "What is 1+1? Be concise."}]
BODY = {"model": MODEL, "max_tokens": 64, "temperature": 0, "include_reasoning": False}


def main() -> None:
    with RemoteOpenAIServer(MODEL, ARGS) as server:
        single = httpx.post(
            server.url_for("v1/chat/completions"),
            json={**BODY, "messages": MESSAGES},
            timeout=300,
        ).json()["usage"]
        batch = httpx.post(
            server.url_for("v1/chat/completions/batch"),
            json={**BODY, "messages": [MESSAGES, MESSAGES]},
            timeout=300,
        ).json()["usage"]
    print("USAGE " + json.dumps({"single": single, "batch_of_2": batch}))


if __name__ == "__main__":
    main()
