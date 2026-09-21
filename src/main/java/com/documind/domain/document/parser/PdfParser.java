package com.documind.domain.document.parser;

import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class PdfParser {

    public static final String PAGE_DELIMITER_PREFIX = "\n[[PAGE:";
    public static final String PAGE_DELIMITER_SUFFIX = "]]\n";

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

            return new ParseResult(text.toString(), pageCount);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PDF_PARSE_FAILED, e);
        }
    }

    public record ParseResult(String text, int pageCount) { }

}
