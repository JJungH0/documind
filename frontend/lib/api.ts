export type ApiResponse<T> = {
    success: boolean;
    data?: T;
    error?: { code: string; message: string };
}

export type DocumentResponse = {
    id: number;
    filename: string;
    fileSize: number;
    pageCount: number | null;
    createdAt: string;
};

export type JobStatus =
    | "PENDING"
    | "PARSING"
    | "CHUNKING"
    | "EMBEDDING"
    | "COMPLETED"
    | "FAILED";

export type JobResponse = {
    id: number;
    documentId: number;
    chunkSize: number;
    chunkOverlap: number;
    chunkStrategy: "FIXED" | "ARTICLE";
    processingMode: "SYNC" | "ASYNC";
    status: JobStatus;
    totalChunks: number;
    embeddedChunks: number;
    progressPercent: number;
    durationMs: number | null;
    errorCode: string | null;
    errorMessage: string | null;
    createdAt: string;
};

export type Source = {
    number: number;
    chunkIndex: number;
    pageNumber: number | null;
    similarity: number;
};

export type AskResponse = {
    queryLogId: number;
    question: string;
    answer: string;
    status: "ANSWERED" | "NO_RELEVANT_CONTEXT";
    sources: Source[];
    contextChars: number;
    embeddingTokens: number;
    promptTokens: number;
    completionTokens: number;
    embeddingMs: number;
    searchMs: number;
    generationMs: number;
    totalMs: number;
};

export class ApiError extends Error {
    readonly code: string;
    readonly status: number;

    constructor(code: string, message: string, status: number) {
        super(message);
        this.name = "ApiError";
        this.code = code;
        this.status = status;
    }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const isForm = init?.body instanceof FormData;
    const response = await fetch(path, {
        ...init,
        headers: isForm ? init?.headers : {"Content-Type": "application/json", ...init?.headers},
    });

    let body: ApiResponse<T> | null = null;
    try{
        body = await response.json();
    }catch {

    }

    if (!response.ok || !body?.success || body.data === undefined) {
        throw new ApiError(
            body?.error?.code ?? `HTTP_${response.status}`,
            body?.error?.message ?? "요청을 처리하지 못했습니다.",
            response.status,
        );
    }
    return body.data;
}

export function getDocuments(): Promise<DocumentResponse[]> {
    return request<DocumentResponse[]>("/api/documents");
}

export function getJobs(documentId: number): Promise<JobResponse[]> {
    return request<JobResponse[]>(`/api/documents/${documentId}/jobs`);
}

export function ask(jobId: number, question: string): Promise<AskResponse> {
    return request<AskResponse>(`/api/jobs/${jobId}/ask`, {
        method: "POST",
        body: JSON.stringify({question}),
    });
}

export function uploadDocument(file: File): Promise<DocumentResponse> {
    const form = new FormData();
    form.append("file", file);
    return request<DocumentResponse>("/api/documents", {method: "POST", body: form});
}

export function createJob(documentId: number): Promise<JobResponse> {
    return request<JobResponse>(`/api/documents/${documentId}/jobs`, {
        method: "POST",
        body: JSON.stringify({chunkSize: 1000, chunkOverlap: 100}),
    });
}

export function getJob(jobId: number): Promise<JobResponse> {
    return request<JobResponse>(`/api/jobs/${jobId}`);
}

export function toErrorMessage(e: unknown): string {
    if (e instanceof ApiError && !e.code.startsWith("HTTP_")) {
        return `[${e.code} ${e.message}`;
    }
    return "서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.";
}