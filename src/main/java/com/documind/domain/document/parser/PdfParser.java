package com.documind.domain.document.parser;

import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Component
public class PdfParser {

    public static final String PAGE_DELIMITER_PREFIX = "\n[[PAGE:";
    public static final String PAGE_DELIMITER_SUFFIX = "]]\n";

    private static final char REPLACEMENT_CHAR = '\uFFFD';

    public ParseResult parse(Path path) {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {

            if (document.isEncrypted()) {
                throw new BusinessException(ErrorCode.ENCRYPTED_PDF);
            }

            int pageCount = document.getNumberOfPages();
            StringBuilder text = new StringBuilder();

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                text.append(PAGE_DELIMITER_PREFIX)
                        .append(page)
                        .append(PAGE_DELIMITER_SUFFIX)
                        .append(stripper.getText(document));
            }

            String normalized = replaceUnmappedChars(text.toString(), path);
            return new ParseResult(normalized, pageCount);

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PDF_PARSE_FAILED, e);
        }
    }

    private String replaceUnmappedChars(String raw, Path path) {
        long brokenCount = raw.chars()
                .filter(c -> c == REPLACEMENT_CHAR)
                .count();

        if (brokenCount > 0) {
            double ratio = (double) brokenCount / raw.length() * 100;
            log.warn("PDF에 유니코드로 변환되지 않은 글자가 있어 공백으로 치환합니다. file={}, count={}, ratio={}%",
                    path.getFileName(), brokenCount, String.format("%.2f", ratio));
        }
        return raw.replace(REPLACEMENT_CHAR, ' ');
    }

    public record ParseResult(String text, int pageCount) {
    }
}