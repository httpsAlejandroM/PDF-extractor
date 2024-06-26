import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFExtractor {
    public static String extractTextFromPDF(String filePath) {
        String text = "";
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            text = pdfStripper.getText(document);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }

    public static String extractSection(String text, String startMarker, String endMarker) {
        int startIndex = text.indexOf(startMarker);
        int endIndex = text.indexOf(endMarker);
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return text.substring(startIndex, endIndex);
        }
        return "";
    }

    public static String extractField(String text, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*(.*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            if (fieldName == "D.N.I.") {
                return matcher.group(1).replace(".", "").trim();
            }
            if (fieldName == "Teléfono particular") {
                matcher.group(1).replace(" ", "").trim();
                return matcher.group(1).replace(" ", "").trim();
            }
            return matcher.group(1).trim();
        }
        return "No encontrado";
    }
}
