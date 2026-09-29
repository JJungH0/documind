"use client";

import { useEffect, useRef, useState } from "react";
import {
    createJob,
    getJob,
    toErrorMessage,
    uploadDocument,
    type DocumentResponse,
    type JobResponse,
    type JobStatus,
} from "@/lib/api";

const POLL_INTERVAL_MS = 1000;
const FINISHED: JobStatus[] = ["COMPLETED", "FAILED"];

const STATUS_LABEL: Record<JobStatus, string> = {
    PENDING: "차례를 기다리는 중",
    PARSING: "글자를 읽는 중",
    CHUNKING: "문서를 나누는 중",
    EMBEDDING: "검색할 수 있게 바꾸는 중",
    COMPLETED: "준비 완료",
    FAILED: "처리 실패",
};

type Props = {
    onUploaded: (document: DocumentResponse) => void;
    onOpen: (job: JobResponse) => void;
};

export default function UploadPanel({ onUploaded, onOpen }: Props) {
    const [uploading, setUploading] = useState(false);
    const [fileName, setFileName] = useState<string | null>(null);
    const [job, setJob] = useState<JobResponse | null>(null);
    const [error, setError] = useState<string | null>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        if (!job || FINISHED.includes(job.status)) return;

        let cancelled = false;
        const timer = setTimeout(() => {
            getJob(job.id)
                .then((next) => {
                    if (!cancelled) setJob(next);
                })
                .catch((e) => {
                    if (!cancelled) setError(toErrorMessage(e));
                });
        }, POLL_INTERVAL_MS);

        return () => {
            cancelled = true;
            clearTimeout(timer);
        };
    }, [job]);

    async function handleFile(file: File) {
        setUploading(true);
        setError(null);
        setJob(null);
        setFileName(file.name);
        try {
            const document = await uploadDocument(file);
            onUploaded(document);
            setJob(await createJob(document.id));
        } catch (e) {
            setError(toErrorMessage(e));
        } finally {
            setUploading(false);
        }
    }

    const busy = uploading || (job !== null && !FINISHED.includes(job.status));

    return (
        <section className="flex flex-col gap-2 border-t border-line pt-4 text-sm">
            <input
                ref={inputRef}
                type="file"
                accept="application/pdf"
                className="hidden"
                onChange={(e) => {
                    const file = e.target.files?.[0];
                    e.target.value = "";
                    if (file) void handleFile(file);
                }}
            />
            <button
                type="button"
                disabled={busy}
                onClick={() => inputRef.current?.click()}
                className="rounded-md border border-line px-3 py-2 text-left hover:bg-canvas focus-visible:outline-2 focus-visible:outline-petrol disabled:text-muted disabled:hover:bg-transparent"
            >
                {busy ? "문서를 처리하고 있습니다" : "PDF 문서 올리기"}
            </button>

            {fileName && (uploading || job) && (
                <div className="flex flex-col gap-1.5">
                    <p className="truncate font-semibold" title={fileName}>
                        {fileName}
                    </p>
                    {uploading && <p className="text-muted">올리는 중</p>}
                    {job && <JobProgress job={job} onOpen={onOpen} />}
                </div>
            )}

            {error && (
                <p role="alert" className="text-danger">
                    {error}
                </p>
            )}
        </section>
    );
}

function JobProgress({ job, onOpen }: { job: JobResponse; onOpen: (job: JobResponse) => void }) {
    const failed = job.status === "FAILED";

    return (
        <>
            <p className={failed ? "text-danger" : "text-muted"}>{STATUS_LABEL[job.status]}</p>

            <div
                role="progressbar"
                aria-label="처리 진행률"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={job.progressPercent}
                className="h-2 overflow-hidden rounded-full bg-canvas"
            >
                <div
                    className={`h-full motion-safe:transition-[width] motion-safe:duration-500 ${
                        failed ? "bg-danger" : "bg-petrol"
                    }`}
                    style={{ width: `${job.progressPercent}%` }}
                />
            </div>

            {job.totalChunks > 0 && (
                <p className="tabular-nums text-muted">
                    {job.totalChunks.toLocaleString()}개 중 {job.embeddedChunks.toLocaleString()}개 처리 (
                    {job.progressPercent}%)
                </p>
            )}

            {failed && job.errorMessage && <p className="text-danger">{job.errorMessage}</p>}

            {job.status === "COMPLETED" && (
                <button
                    type="button"
                    onClick={() => onOpen(job)}
                    className="self-start text-petrol underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-petrol"
                >
                    이 문서로 질문하기
                </button>
            )}
        </>
    );
}