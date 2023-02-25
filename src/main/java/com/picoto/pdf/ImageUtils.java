package com.picoto.pdf;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

public class ImageUtils {

        public static BufferedImage generarCodigoBarras128(String texto, String patron, int ancho, int alto) throws IOException {
                BitMatrix matrix;
                try {
                        matrix = new MultiFormatWriter().encode(texto, BarcodeFormat.CODE_128, ancho, alto, getPropiedades());
                } catch (WriterException e) {
                        throw new IOException("Error al generar el código de barras", e);
                }

                return MatrixToImageWriter.toBufferedImage(matrix);

        }
       
        private static Map<EncodeHintType, Integer> getPropiedades() {
                Map<EncodeHintType, Integer> props = new HashMap<>();
                props.put(EncodeHintType.MARGIN, 0);
                return props;
        }

        public static BufferedImage generarCodigoBarras417(String texto, String patron, int ancho, int alto) throws IOException {
                BitMatrix matrix;
                try {
                        matrix = new MultiFormatWriter().encode(texto, BarcodeFormat.PDF_417, ancho, alto, getPropiedades());
                } catch (WriterException e) {
                        throw new IOException("Error al generar el código de barras", e);
                }

                return MatrixToImageWriter.toBufferedImage(matrix);

        }
       
        public static BufferedImage generarCodigoBarrarQR(String texto, String patron, int ancho, int alto) throws IOException {
                BitMatrix matrix;
                try {
                        matrix = new MultiFormatWriter().encode(texto, BarcodeFormat.QR_CODE, ancho, alto, getPropiedades());
                } catch (WriterException e) {
                        throw new IOException("Error al generar el código QR", e);
                }

                return MatrixToImageWriter.toBufferedImage(matrix);

        }

}