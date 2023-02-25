package com.picoto.pdf.test;

import java.io.IOException;

import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.ValidationResult.ValidationError;
import org.apache.pdfbox.preflight.exception.SyntaxValidationException;
import org.apache.pdfbox.preflight.parser.PreflightParser;

public class ConformanceTest {
	public static void main(String[] args) throws IOException {
		ValidationResult result = null;

		PreflightParser parser = new PreflightParser("./Pdf-A-Test.pdf");
		try {
			parser.parse();
			PreflightDocument document = parser.getPreflightDocument();
			document.validate();
			result = document.getResult();
			document.close();

		} catch (SyntaxValidationException e) {
			result = e.getResult();
		}

		if (result.isValid()) {
			System.out.println("The file is a valid PDF/A-1b file");
		} else {
			System.out.println("The file is not valid, error(s) :");
			for (ValidationError error : result.getErrorsList()) {
				System.out.println(error.getErrorCode() + " : " + error.getDetails());
			}
		}
	}
}
