"""질문 쌍의 임베딩 유사도를 측정한다. 시맨틱 캐시 기준값을 정하기 위한 도구."""
import json
import math
import os
import urllib.request

MODEL = "text-embedding-3-small"

PAIRS = [
    ("같은 뜻", "입사 1년이 지나면 연차가 며칠이야?", "1년 근무하면 연차 몇 일 줘?"),
    ("같은 뜻", "밤늦게까지 일하면 저녁값 지원돼?", "야근하면 식대 나와?"),
    ("같은 뜻", "회사 휴대폰 잃어버리면 어떻게 해야 돼?", "업무용 폰 분실하면 뭐 해야 해?"),
    ("같은 뜻", "배우자가 출산하면 휴가 며칠이야?", "와이프 출산하면 휴가 며칠 줘?"),
    ("같은 뜻", "장애 등급 SEV1은 몇 분 안에 대응해야 해?", "SEV1 장애 대응 시간이 몇 분이야?"),
    ("다른 뜻", "입사 1년이 지나면 연차가 며칠이야?", "입사 3년이 지나면 연차가 며칠이야?"),
    ("다른 뜻", "장애 등급 SEV1은 몇 분 안에 대응해야 해?", "장애 등급 SEV2는 몇 분 안에 대응해야 해?"),
    ("다른 뜻", "배우자가 출산하면 휴가 며칠이야?", "본인이 결혼하면 휴가 며칠이야?"),
    ("다른 뜻", "밤늦게까지 일하면 저녁값 지원돼?", "밤늦게까지 일하면 택시비 지원돼?"),
    ("다른 뜻", "병가 중에 돈 받는 날은 며칠이야?", "병가는 최대 며칠까지 쓸 수 있어?"),
    ("무관", "입사 1년이 지나면 연차가 며칠이야?", "사내 식당은 몇 시에 열어?"),
]


def embed(texts):
    body = json.dumps({"model": MODEL, "input": texts}).encode("utf-8")
    req = urllib.request.Request(
        "https://api.openai.com/v1/embeddings",
        data=body,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {os.environ['OPENAI_API_KEY']}",
        },
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.load(resp)
    vectors = [None] * len(texts)
    for item in data["data"]:
        vectors[item["index"]] = item["embedding"]
    return vectors


def cosine(a, b):
    dot = sum(x * y for x, y in zip(a, b))
    return dot / (math.sqrt(sum(x * x for x in a)) * math.sqrt(sum(y * y for y in b)))


def main():
    texts = sorted({q for _, a, b in PAIRS for q in (a, b)})
    vectors = dict(zip(texts, embed(texts)))

    results = []
    for label, a, b in PAIRS:
        sim = cosine(vectors[a], vectors[b])
        results.append((label, sim, a, b))
        print(f"{label:<5} {sim:.4f}  {a}  |  {b}")

    same = [s for label, s, _, _ in results if label == "같은 뜻"]
    other = [s for label, s, _, _ in results if label != "같은 뜻"]
    print()
    print(f"같은 뜻 최저:        {min(same):.4f}")
    print(f"다른 뜻·무관 최고:   {max(other):.4f}")
    if min(same) > max(other):
        print("→ 두 범위가 겹치지 않음: 그 사이에 기준값을 둘 수 있음")
    else:
        print("→ 두 범위가 겹침: 유사도 하나로는 안전한 기준값이 없음")


if __name__ == "__main__":
    main()