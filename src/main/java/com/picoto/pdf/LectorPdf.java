package com.picoto.pdf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;

public class LectorPdf {

        private byte[] contenido;
        private String name;

        public LectorPdf(byte[] contenido, String name) throws IOException {
                this.contenido = contenido;
                this.name = name;
        }

        public InputStream getReader() {
                return new ByteArrayInputStream(contenido);
        }


        public PDDocument getCachedDocument() throws IOException {
                return PDDocument.load(contenido);
        }
       
        public String getName() {
                return name;
        }

}