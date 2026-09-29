"use client";

import { useState } from "react";
import type { AskResponse } from "@/lib/api";

const CITATION_SPLIT = /(\[\d+\])/;
const CITATION_EXACT = /^\[(\d+)\]$/;

export default function AssistantMessage({ response }: { response: AskResponse }) {
    const [active, setActive] = useState<number | null>(null);
    const parts = response.answer.split(CITATION_SPLIT);
    const skipped = response.status === "NO_RELEVANT_CONTEXT";

    return (
        <div className="flex flex-col gap-2 md:flex-row md:items-start md:gap-0">
            <article
                className={`min-w-0 flex-1 rounded-md border border-line bg-surface px-4 py-3 leading-7 ${
                    skipped ? "text-muted" : ""
                }`}
            >
                <p className="whitespace-pre-wrap">
                    {parts.map((part, i) => {
                        const match = part.match(CITATION_EXACT);
                        if (!match) return <span key={i}>{part}</span>;

                        const number = Number(match[1]);
                        return (
                            <button
                                key={i}
                                type="button"
                                aria-label={`출처 ${number} 보기`}
                                className={`mx-0.5 rounded px-1 align-super text-xs font-semibold focus-visible:outline-2 focus-visible:outline-petrol ${
                                    active === number ? "bg-marker text-ink" : "text-petrol"
                                }`}
                                onClick={() => setActive(number)}
                            >
                                {number}
                            </button>
                        );
                    })}
                </p>

                <details className="mt-3 text-xs text-muted">
                    <summary className="cursor-pointer select-none">처리 정보</summary>
                    <dl className="mt-2 grid grid-cols-2 gap-x-6 gap-y-1 tabular-nums sm:grid-cols-4">
                        <Metric label="보낸 발췌" value={`${response.sources.length}개, ${response.contextChars.toLocaleString()}자`} />
                        <Metric label="입력 토큰" value={response.promptTokens.toLocaleString()} />
                        <Metric label="출력 토큰" value={response.completionTokens.toLocaleString()} />
                        <Metric label="임베딩 토큰" value={response.embeddingTokens.toLocaleString()} />
                        <Metric label="전체" value={`${response.totalMs.toLocaleString()}ms`} />
                        <Metric label="임베딩" value={`${response.embeddingMs.toFixed(0)}ms`} />
                        <Metric label="검색" value={`${response.searchMs.toFixed(1)}ms`} />
                        <Metric label="생성" value={`${response.generationMs.toFixed(0)}ms`} />
                    </dl>
                    {skipped && (
                        <p className="mt-2">
                            문서와 비슷한 내용이 기준보다 적어 답변 생성 모델을 호출하지 않았습니다.
                        </p>
                    )}
                </details>
            </article>

            {response.sources.length > 0 && (
                <ol aria-label="출처" className="flex flex-wrap gap-1 md:mt-3 md:flex-col">
                    {response.sources.map((source) => (
                        <li key={source.number}>
                            <button
                                type="button"
                                title={`유사도 ${source.similarity.toFixed(3)}, 청크 ${source.chunkIndex}번`}
                                className={`flex items-baseline gap-1.5 rounded-md border border-line px-2 py-1 text-xs focus-visible:outline-2 focus-visible:outline-petrol md:rounded-l-none md:border-l-0 ${
                                    active === source.number ? "bg-marker" : "bg-surface"
                                }`}
                                onClick={() => setActive(source.number)}
                            >
                                <span className="font-semibold">{source.number}</span>
                                <span>{source.pageNumber ?? "?"}쪽</span>
                            </button>
                        </li>
                    ))}
                </ol>
            )}
        </div>
    );
}

function Metric({ label, value }: { label: string; value: string }) {
    return (
        <div className="flex justify-between gap-2">
            <dt>{label}</dt>
            <dd>{value}</dd>
        </div>
    );
}