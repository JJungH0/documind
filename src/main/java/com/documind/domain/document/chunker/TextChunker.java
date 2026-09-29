package com.documind.domain.document.chunker;

import com.documind.domain.document.parser.PdfParser;
import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextChunker {

    public static final int MIN_CHUNK_SIZE = 100;
    public static final int MAX_CHUNK_SIZE = 4000;

    private static final Pattern PAGE_MARKER = Pattern.compile(
            Pattern.quote(PdfParser.PAGE_DELIMITER_PREFIX)
                    + "(\\d+)"
                    + Pattern.quote(PdfParser.PAGE_DELIMITER_SUFFIX));

    private static final Pattern ARTICLE_HEADING = Pattern.compile(
            "^[ \\t]*제\\s?\\d+조(?:의\\s?\\d+)?\\s*\\(", Pattern.MULTILINE);

    private static final Pattern CHAPTER_HEADING = Pattern.compile(
            "^[ \\t]*(?:제\\s?\\d+장\\s+\\S.*|부칙)[ \\t]*$", Pattern.MULTILINE);

    private static final String[][] SEPARATOR_LEVELS = {
            {"\n\n"},
            {". ", "? ", "! ", ".\n", "?\n", "!\n"},
            {"\n"},
            {" "}
    };

    public List<ChunkDraft> chunk(String rawText, int chunkSize, int overlap) {
        validate(chunkSize, overlap);
        PageIndexedText indexed = removePageMarkers(rawText);

        List<ChunkDraft> chunks = new ArrayList<>();
        addPieces(chunks, indexed, 0, indexed.text().length(), null, chunkSize, overlap);
        return chunks;
    }

    public List<ChunkDraft> chunkByArticle(String rawText, int maxSize, int overlap) {
        validate(maxSize, overlap);
        PageIndexedText indexed = removePageMarkers(rawText);
        String text = indexed.text();
        List<Heading> headings = findHeadings(text);

        List<ChunkDraft> chunks = new ArrayList<>();
        if (headings.stream().noneMatch(Heading::article)) {
            addPieces(chunks, indexed, 0, text.length(), null, maxSize, overlap);
            return chunks;
        }

        addPieces(chunks, indexed, 0, headings.getFirst().start(), null, maxSize, overlap);

        String chapter = null;
        for (int i = 0; i < headings.size(); i++) {
            Heading heading = headings.get(i);
            int end = (i + 1 < headings.size()) ? headings.get(i + 1).start() : text.length();

            if (heading.article()) {
                addPieces(chunks, indexed, heading.start(), end, chapter, maxSize, overlap);
                continue;
            }

            int lineEnd = text.indexOf('\n', heading.start());
            if (lineEnd < 0 || lineEnd > end) {
                lineEnd = end;
            }
            chapter = text.substring(heading.start(), lineEnd).strip();
            if (hasLetter(text, lineEnd, end)) {
                addPieces(chunks, indexed, lineEnd, end, chapter, maxSize, overlap);
            }
        }
        return chunks;
    }

    private void addPieces(List<ChunkDraft> chunks, PageIndexedText indexed,
                           int from, int to, String prefix, int size, int overlap) {
        String text = indexed.text();
        int bodySize = (prefix == null)
                ? size
                : Math.max(MIN_CHUNK_SIZE, size - prefix.length() - 1);
        int bodyOverlap = Math.min(overlap, bodySize / 2 - 1);

        int start = from;
        while (start < to) {
            int end = findChunkEnd(text, start, to, bodySize);
            String body = text.substring(start, end).strip();

            if (!body.isEmpty()) {
                String content = (prefix == null) ? body : prefix + "\n" + body;
                chunks.add(new ChunkDraft(chunks.size(), content, indexed.pageAt(start)));
            }

            if (end >= to) {
                break;
            }
            start = Math.max(end - bodyOverlap, start + 1);
        }
    }

    private int findChunkEnd(String text, int start, int limit, int chunkSize) {
        int hardEnd = Math.min(start + chunkSize, limit);
        if (hardEnd == limit) {
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

    private List<Heading> findHeadings(String text) {
        List<Heading> headings = new ArrayList<>();

        Matcher article = ARTICLE_HEADING.matcher(text);
        while (article.find()) {
            headings.add(new Heading(article.start(), true));
        }

        Matcher chapter = CHAPTER_HEADING.matcher(text);
        while (chapter.find()) {
            headings.add(new Heading(chapter.start(), false));
        }

        headings.sort(Comparator.comparingInt(Heading::start));
        return headings;
    }

    private static boolean hasLetter(String text, int from, int to) {
        return text.substring(from, to).codePoints().anyMatch(Character::isLetter);
    }

    private PageIndexedText removePageMarkers(String rawText) {
        Matcher matcher = PAGE_MARKER.matcher(rawText);
        StringBuilder clean = new StringBuilder(rawText.length());
        List<Integer> pageStarts = new ArrayList<>();
        List<Integer> pageNumbers = new ArrayList<>();

        int last = 0;
        while (matcher.find()) {
            clean.append(rawText, last, matcher.start());
            if (clean.length() > 0 && clean.charAt(clean.length() - 1) != '\n') {
                clean.append('\n');
            }
            pageStarts.add(clean.length());
            pageNumbers.add(Integer.parseInt(matcher.group(1)));
            last = matcher.end();
        }
        clean.append(rawText, last, rawText.length());

        return new PageIndexedText(clean.toString(), pageStarts, pageNumbers);
    }

    public void validate(int chunkSize, int overlap) {
        if (chunkSize < MIN_CHUNK_SIZE || chunkSize > MAX_CHUNK_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_CHUNK_OPTION,
                    "chunkSize는 " + MIN_CHUNK_SIZE + "~" + MAX_CHUNK_SIZE
                            + " 사이여야 합니다. (입력: " + chunkSize + ")");
        }
        if (overlap < 0 || overlap >= chunkSize / 2) {
            throw new BusinessException(ErrorCode.INVALID_CHUNK_OPTION,
                    "overlap은 0 이상, chunkSize의 절반 미만이어야 합니다. (입력: " + overlap + ")");
        }
    }

    private record Heading(int start, boolean article) {
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
                return pageNumbers.get(0);
            }
            return pageNumbers.get(idx);
        }
    }
}