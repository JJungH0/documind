"""OpenAI API를 흉내 내는 로컬 서버. 재시도 동작을 비용 없이 측정하기 위한 도구.

실행 예:
  python3 scripts/mock/mock_openai.py --status 401 --fail-count -1       # 항상 401
  python3 scripts/mock/mock_openai.py --status 429 --fail-count 3        # 처음 3번만 429, 이후 정상
  python3 scripts/mock/mock_openai.py --status 429 --fail-count -1 --retry-after 1
"""
import argparse
import hashlib
import json
import math
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DIMENSIONS = 1536

ERROR_BODIES = {
    401: {"message": "Incorrect API key provided.", "type": "invalid_request_error", "code": "invalid_api_key"},
    429: {"message": "Rate limit reached for requests.", "type": "requests", "code": "rate_limit_exceeded"},
    500: {"message": "The server had an error while processing your request.", "type": "server_error", "code": None},
}


class State:
    def __init__(self, status, fail_count, retry_after):
        self.status = status
        self.fail_count = fail_count
        self.retry_after = retry_after
        self.count = 0
        self.started = None
        self.lock = threading.Lock()

    def next(self):
        with self.lock:
            self.count += 1
            now = time.monotonic()
            if self.started is None:
                self.started = now
            should_fail = self.fail_count < 0 or self.count <= self.fail_count
            return self.count, now - self.started, should_fail


def fake_vector(text):
    seed = hashlib.sha256(text.encode("utf-8")).digest()
    values = [((seed[i % len(seed)] + i) % 251) / 251 - 0.5 for i in range(DIMENSIONS)]
    norm = math.sqrt(sum(v * v for v in values)) or 1.0
    return [v / norm for v in values]


def make_handler(state):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, fmt, *args):
            pass

        def do_POST(self):
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length) if length else b"{}"
            n, elapsed, should_fail = state.next()

            if should_fail:
                self.send_error_json(n, elapsed)
                return

            if self.path.endswith("/embeddings"):
                self.send_embeddings(n, elapsed, body)
            else:
                print(f"[#{n:>3} +{elapsed:7.2f}s] POST {self.path} -> 404 (흉내 내지 않는 경로)", flush=True)
                self.send_json(404, {"error": {"message": "not mocked", "type": "invalid_request_error"}})

        def send_error_json(self, n, elapsed):
            status = state.status
            print(f"[#{n:>3} +{elapsed:7.2f}s] POST {self.path} -> {status}", flush=True)
            headers = {}
            if status == 429 and state.retry_after is not None:
                headers["Retry-After"] = str(state.retry_after)
            self.send_json(status, {"error": ERROR_BODIES.get(status, {"message": "error"})}, headers)

        def send_embeddings(self, n, elapsed, body):
            payload = json.loads(body or b"{}")
            inputs = payload.get("input", [])
            if isinstance(inputs, str):
                inputs = [inputs]
            tokens = sum(max(1, len(t) // 2) for t in inputs)
            data = [{"object": "embedding", "index": i, "embedding": fake_vector(t)} for i, t in enumerate(inputs)]
            print(f"[#{n:>3} +{elapsed:7.2f}s] POST {self.path} -> 200 (입력 {len(inputs)}개)", flush=True)
            self.send_json(200, {
                "object": "list",
                "data": data,
                "model": payload.get("model", "text-embedding-3-small"),
                "usage": {"prompt_tokens": tokens, "total_tokens": tokens},
            })

        def send_json(self, status, obj, headers=None):
            raw = json.dumps(obj).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(raw)))
            for k, v in (headers or {}).items():
                self.send_header(k, v)
            self.end_headers()
            self.wfile.write(raw)

    return Handler


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=9999)
    parser.add_argument("--status", type=int, default=429, help="실패 시 돌려줄 HTTP 상태")
    parser.add_argument("--fail-count", type=int, default=-1, help="처음 몇 번 실패할지 (-1이면 항상 실패)")
    parser.add_argument("--retry-after", type=int, default=None, help="429 응답에 넣을 Retry-After(초)")
    args = parser.parse_args()

    state = State(args.status, args.fail_count, args.retry_after)
    server = ThreadingHTTPServer(("127.0.0.1", args.port), make_handler(state))
    mode = "항상" if args.fail_count < 0 else f"처음 {args.fail_count}번"
    print(f"가짜 OpenAI 서버: http://localhost:{args.port}  ({mode} {args.status} 응답)", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print(f"\n종료: 받은 요청 {state.count}번", flush=True)


if __name__ == "__main__":
    main()
