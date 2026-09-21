package com.documind.domain.document.chunker;

import com.documind.domain.document.parser.PdfParser;
import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextChunker {

    private static final int MIN_CHUNK_SIZE = 100;
    private static final int MAX_CHUNK_SIZE = 4000;

    private static final Pattern PAGE_MARKER = Pattern.compile(
            Pattern.quote(PdfParser.PAGE_DELIMITER_PREFIX)
                    + "(\\d+)"
                    + Pattern.quote(PdfParser.PAGE_DELIMITER_SUFFIX)
    );

    private static final String[][] SEPARATOR_LEVELS = {
            {"\n\n"},
            {". ", "? ", "! ", ".\n", "?\n", "!\n"},
            {"\n"},
            {" "}
    };

    public List<ChunkDraft> chunk(String rawText, int chunkSize, int overlap) {
        validate(chunkSize, overlap);

        PageIndexedText indexed = removePageMarkers(rawText);
        String text = indexed.text();

        List<ChunkDraft> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;

        while (start < text.length()) {
            int end = findChunkEnd(text, start, chunkSize);
            String content = text.substring(start, end).strip();

            if (!content.isEmpty()) {
                chunks.add(new ChunkDraft(index, content, indexed.pageAt(start)));
                index++;
            }

            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    private int findChunkEnd(String text, int start, int chunkSize) {
        int hardEnd = Math.min(start + chunkSize, text.length());

        if (hardEnd == text.length()) {
            return hardEnd;
        }
        int minEnd = start + chunkSize / 2;

        for (String[] level : SEPARATOR_LEVELS) {
            int bestEnd = -1;
            for (String separator : level) {
                int pos = text.lastIndexOf(separator, hardEnd - separator.length());
                if (pos >= minEnd) {
                    bestEnd = Math.max(bestEnd, pos + separator.length());
                }
            }
            if (bestEnd != -1) {
                return bestEnd;
            }
        }
        return hardEnd;
    }


    private void validate(int chunkSize, int overlap) {
        if (chunkSize < MIN_CHUNK_SIZE || chunkSize > MAX_CHUNK_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_CHUNK_OPTION,
                    "chunkSize는 " + MIN_CHUNK_SIZE + "~" + MAX_CHUNK_SIZE
                            + " 사이어야 함. (입력: " + chunkSize + ")");
        }
        if (overlap < 0 || overlap >= chunkSize / 2) {
            throw new BusinessException(ErrorCode.INVALID_CHUNK_OPTION,
                    "overlap은 0 이상, chunkSize의 절반 미만이어야 함. (입력: " + overlap + ")");
        }
    }

    private PageIndexedText removePageMarkers(String rawText) {
        Matcher matcher = PAGE_MARKER.matcher(rawText);
        StringBuilder clean = new StringBuilder(rawText.length());
        List<Integer> pageStarts = new ArrayList<>();
        List<Integer> pageNumbers = new ArrayList<>();

        int last = 0;
        while (matcher.find()) {
            clean.append(rawText, last, matcher.start());
            pageStarts.add(clean.length());
            pageNumbers.add(Integer.parseInt(matcher.group(1)));
            last = matcher.end();
        }
        clean.append(rawText, last, rawText.length());

        return new PageIndexedText(clean.toString(), pageStarts, pageNumbers);
    }

    private record PageIndexedText(String text, List<Integer> pageStarts, List<Integer> pageNumbers) {
        Integer pageAt(int offset) {
            if (pageStarts.isEmpty()) {
                return null;
            }

            int idx = Collections.binarySearch(pageStarts, offset);
            if (idx < 0) {
                idx = -idx - 2;
            }
            if (idx < 0) {
                return pageNumbers.getFirst();
            }

            return pageNumbers.get(idx);
        }
    }
}
