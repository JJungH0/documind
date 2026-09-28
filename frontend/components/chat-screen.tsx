"use client";

import { useEffect, useRef, useState } from "react";
import AssistantMessage from "@/components/assistant-message";
import {
    ApiError,
    ask,
    getDocuments,
    getJobs,
    type AskResponse,
    type DocumentResponse,
    type JobResponse,
} from "@/lib/api";

const MAX_QUESTION_LENGTH = 1000;

type Message =
    | { id: string; role: "user"; text: string }
    | { id: string; role: "assistant"; response: AskResponse }
    | { id: string; role: "error"; text: string };

export default function ChatScreen() {
    const [documents, setDocuments] = useState<DocumentResponse[]>([]);
    const [jobs, setJobs] = useState<JobResponse[]>([]);
    const [documentId, setDocumentId] = useState<number | null>(null);
    const [jobId, setJobId] = useState<number | null>(null);
    const [messages, setMessages] = useState<Message[]>([]);
    const [draft, setDraft] = useState("");
    const [loading, setLoading] = useState(false);
    const [loadError, setLoadError] = useState<string | null>(null);
    const bottomRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        let ignore = false;
        getDocuments()
            .then((docs) => {
                if (!ignore) setDocuments(docs);
            })
            .catch((e) => {
                if (!ignore) setLoadError(toMessage(e));
            });
        return () => {
            ignore = true;
        };
    }, []);

    useEffect(() => {
        const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        bottomRef.current?.scrollIntoView({
            behavior: reduceMotion ? "auto" : "smooth",
            block: "end",
        });
    }, [messages, loading]);

    function resetConversation() {
        setMessages([]);
        setDraft("");
    }

    function handleDocumentChange(value: string) {
        const id = value ? Number(value) : null;
        setDocumentId(id);
        setJobs([]);
        setJobId(null);
        setLoadError(null);
        resetConversation();
        if (id === null) return;

        getJobs(id)
            .then((list) => {
                setJobs(list);
                setJobId(list.find((job) => job.status === "COMPLETED")?.id ?? null);
            })
            .catch((e) => setLoadError(toMessage(e)));
    }

    function handleJobChange(value: string) {
        setJobId(value ? Number(value) : null);
        resetConversation();
    }

    async function send() {
        const question = draft.trim();
        if (jobId === null || !question || loading) return;

        setDraft("");
        setMessages((prev) => [...prev, { id: crypto.randomUUID(), role: "user", text: question }]);
        setLoading(true);
        try {
            const response = await ask(jobId, question);
            setMessages((prev) => [...prev, { id: crypto.randomUUID(), role: "assistant", response }]);
        } catch (e) {
            setMessages((prev) => [...prev, { id: crypto.randomUUID(), role: "error", text: toMessage(e) }]);
        } finally {
            setLoading(false);
        }
    }

    const selectedDocument = documents.find((doc) => doc.id === documentId);
    const ready = jobId !== null;
    const canSend = ready && draft.trim().length > 0 && !loading;

    return (
        <div className="flex h-dvh flex-col md:flex-row">
            <aside className="flex shrink-0 flex-col gap-4 border-b border-line bg-surface p-4 md:w-72 md:border-r md:border-b-0">
                <p className="text-lg font-semibold">DocuMind</p>

                <div className="grid gap-3 sm:grid-cols-2 md:grid-cols-1">
                    <label className="flex flex-col gap-1 text-sm">
                        <span className="text-muted">문서</span>
                        <select
                            className="rounded-md border border-line bg-surface px-3 py-2 focus-visible:outline-2 focus-visible:outline-petrol"
                            value={documentId ?? ""}
                            onChange={(e) => handleDocumentChange(e.target.value)}
                        >
                            <option value="">문서를 고르세요</option>
                            {documents.map((doc) => (
                                <option key={doc.id} value={doc.id}>
                                    {doc.filename}
                                </option>
                            ))}
                        </select>
                    </label>

                    <label className="flex flex-col gap-1 text-sm">
                        <span className="text-muted">처리 설정</span>
                        <select
                            className="rounded-md border border-line bg-surface px-3 py-2 focus-visible:outline-2 focus-visible:outline-petrol disabled:text-muted"
                            value={jobId ?? ""}
                            disabled={jobs.length === 0}
                            onChange={(e) => handleJobChange(e.target.value)}
                        >
                            <option value="">설정을 고르세요</option>
                            {jobs.map((job) => (
                                <option key={job.id} value={job.id} disabled={job.status !== "COMPLETED"}>
                                    {jobLabel(job)}
                                </option>
                            ))}
                        </select>
                    </label>
                </div>

                <button
                    type="button"
                    className="self-start text-sm text-petrol underline-offset-4 hover:underline disabled:text-muted disabled:no-underline"
                    disabled={messages.length === 0}
                    onClick={resetConversation}
                >
                    새 대화
                </button>

                {loadError && (
                    <p role="alert" className="text-sm text-danger">
                        {loadError}
                    </p>
                )}
            </aside>

            <main className="flex min-h-0 flex-1 flex-col">
                <div className="min-h-0 flex-1 overflow-y-auto">
                    <div className="mx-auto flex max-w-3xl flex-col gap-6 px-4 py-6">
                        {messages.length === 0 && (
                            <EmptyState filename={selectedDocument?.filename} ready={ready} />
                        )}

                        {messages.map((message) => {
                            if (message.role === "user") {
                                return (
                                    <p
                                        key={message.id}
                                        className="ml-auto max-w-[80%] whitespace-pre-wrap rounded-2xl rounded-br-sm bg-petrol px-4 py-2 text-white"
                                    >
                                        {message.text}
                                    </p>
                                );
                            }
                            if (message.role === "assistant") {
                                return <AssistantMessage key={message.id} response={message.response} />;
                            }
                            return (
                                <p key={message.id} role="alert" className="text-sm text-danger">
                                    {message.text}
                                </p>
                            );
                        })}

                        {loading && (
                            <p className="text-sm text-muted motion-safe:animate-pulse">답변을 만드는 중</p>
                        )}

                        <div ref={bottomRef} />
                    </div>
                </div>

                <div className="border-t border-line bg-surface px-4 py-3">
                    <div className="mx-auto flex max-w-3xl items-end gap-2">
            <textarea
                rows={2}
                maxLength={MAX_QUESTION_LENGTH}
                disabled={!ready}
                value={draft}
                placeholder={ready ? "질문을 입력하세요. Enter로 보내고 Shift+Enter로 줄을 바꿉니다." : "먼저 문서를 고르세요."}
                className="max-h-40 min-h-11 flex-1 resize-none rounded-md border border-line px-3 py-2 focus-visible:outline-2 focus-visible:outline-petrol disabled:bg-canvas"
                onChange={(e) => setDraft(e.target.value)}
                onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
                        e.preventDefault();
                        void send();
                    }
                }}
            />
                        <button
                            type="button"
                            className="rounded-md bg-petrol px-4 py-2 font-semibold text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-petrol disabled:opacity-40"
                            disabled={!canSend}
                            onClick={() => void send()}
                        >
                            보내기
                        </button>
                    </div>
                </div>
            </main>
        </div>
    );
}

function EmptyState({ filename, ready }: { filename?: string; ready: boolean }) {
    if (!ready) {
        return <p className="mt-16 text-center text-muted">문서를 고르면 질문할 수 있습니다.</p>;
    }
    return (
        <div className="mx-auto mt-16 max-w-md text-center">
            <p className="text-lg font-semibold">{filename}에 대해 물어보세요</p>
            <p className="mt-2 text-sm leading-6 text-muted">
                질문마다 문서를 새로 찾아 답합니다. 앞의 질문을 이어받지 않으니, 매번 완전한 문장으로 물어보세요.
            </p>
        </div>
    );
}

function jobLabel(job: JobResponse): string {
    const base = `#${job.id}  청크 ${job.chunkSize}자, 겹침 ${job.chunkOverlap}자`;
    return job.status === "COMPLETED" ? base : `${base} (${job.status})`;
}

function toMessage(e: unknown): string {
    if (e instanceof ApiError) {
        if (e.code.startsWith("HTTP_")) {
            return "서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.";
        }
        return `[${e.code}] ${e.message}`;
    }
    return "서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.";
}