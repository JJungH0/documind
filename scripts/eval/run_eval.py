#!/usr/bin/env python3
"""DocuMind RAG 평가 스크립트.

질문 세트를 /api/jobs/{jobId}/ask 로 보내고, 판정과 측정값을 CSV로 저장한다.
표준 라이브러리만 사용한다.
"""
import argparse
import csv
import json
import statistics
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

# 서버 QueryService.NO_CONTEXT_ANSWER 와 같은 문장이어야 한다 (마침표 제외).
NO_CONTEXT_ANSWER = "문서에서 관련 내용을 찾을 수 없습니다"
WARMUP_QUESTION = "이 문서는 어떤 문서야?"

CSV_FIELDS = [
    "run", "id", "type", "question", "verdict", "answer",
    "top_similarity", "embedding_tokens", "prompt_tokens", "completion_tokens",
    "embedding_ms", "search_ms", "generation_ms", "total_ms", "error",
]


def ask(base_url, job_id, question):
    body = json.dumps({"question": question}).encode("utf-8")
    request = urllib.request.Request(
        f"{base_url}/api/jobs/{job_id}/ask",
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return json.loads(response.read().decode("utf-8"))["data"], None
    except urllib.error.HTTPError as e:
        try:
            code = json.loads(e.read().decode("utf-8"))["error"]["code"]
        except (ValueError, KeyError):
            code = f"HTTP {e.code}"
        return None, code
    except urllib.error.URLError as e:
        return None, f"CONNECTION: {e.reason}"


def judge(item, data):
    refused = NO_CONTEXT_ANSWER in data["answer"]

    if not item["answerable"]:
        return "PASS" if refused else "CHECK"
    if refused:
        return "FALSE_REFUSAL"

    answer = data["answer"].replace(" ", "")
    for group in item["expected"]:
        if not any(keyword.replace(" ", "") in answer for keyword in group):
            return "FAIL"
    return "PASS"


def to_row(run, item, data, error):
    row = {"run": run, "id": item["id"], "type": item["type"], "question": item["question"]}
    if error:
        row.update(verdict="ERROR", error=error)
        return row

    sources = data["sources"]
    row.update(
        verdict=judge(item, data),
        answer=data["answer"],
        top_similarity=round(sources[0]["similarity"], 4) if sources else "",
        embedding_tokens=data["embeddingTokens"],
        prompt_tokens=data["promptTokens"],
        completion_tokens=data["completionTokens"],
        embedding_ms=round(data["embeddingMs"], 1),
        search_ms=round(data["searchMs"], 2),
        generation_ms=round(data["generationMs"], 1),
        total_ms=data["totalMs"],
    )
    return row


def print_summary(rows):
    verdicts = [r["verdict"] for r in rows]
    print("\n=== 판정 ===")
    for v in ["PASS", "FAIL", "FALSE_REFUSAL", "CHECK", "ERROR"]:
        print(f"{v:<15}{verdicts.count(v)}")

    ok = [r for r in rows if r["verdict"] != "ERROR"]
    if not ok:
        return

    print("\n=== 토큰 합계 ===")
    for key in ["embedding_tokens", "prompt_tokens", "completion_tokens"]:
        print(f"{key:<19}{sum(r[key] for r in ok)}")

    print("\n=== 지연 중앙값 (ms) ===")
    for key in ["embedding_ms", "search_ms", "generation_ms", "total_ms"]:
        print(f"{key:<15}{statistics.median([r[key] for r in ok]):.1f}")


def main():
    parser = argparse.ArgumentParser(description="DocuMind RAG 평가")
    parser.add_argument("--job-id", type=int, required=True)
    parser.add_argument("--questions", default=str(Path(__file__).parent / "questions.json"))
    parser.add_argument("--repeat", type=int, default=1)
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--no-warmup", action="store_true")
    args = parser.parse_args()

    items = json.loads(Path(args.questions).read_text(encoding="utf-8"))

    if not args.no_warmup:
        print("예열 요청 1회 (기록하지 않음)")
        ask(args.base_url, args.job_id, WARMUP_QUESTION)

    rows = []
    for run in range(1, args.repeat + 1):
        for item in items:
            data, error = ask(args.base_url, args.job_id, item["question"])
            row = to_row(run, item, data, error)
            rows.append(row)
            print(f"[run {run}] #{item['id']:<3}{row['verdict']:<15}{item['question']}")

    out_dir = Path(__file__).parent / "results"
    out_dir.mkdir(exist_ok=True)
    out_path = out_dir / f"{datetime.now():%Y%m%d-%H%M%S}_job{args.job_id}.csv"
    with out_path.open("w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_FIELDS)
        writer.writeheader()
        writer.writerows(rows)

    print_summary(rows)
    print(f"\n결과 저장: {out_path}")


if __name__ == "__main__":
    main()