package com.picoto.pdf;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.picoto.pdf.Texto.Tipo;

public class UtilStrings {

        public static List<String> parseaLineas(String texto, float maxAnchoPixels, int fontSize,
                        CalcularLongitudTexto calc, boolean bold) throws IOException {
                List<String> lines = new ArrayList<String>();
                int lastSpace = -1;
                while (texto.length() > 0) {
                        int spaceIndex = texto.indexOf(' ', lastSpace + 1);
                        if (spaceIndex < 0)
                                spaceIndex = texto.length();
                        String subString = texto.substring(0, spaceIndex);
                        float size = calc.getLongitudTexto(subString, fontSize, bold);
                        float anchoPixels = maxAnchoPixels;
                        if (size > anchoPixels) {
                                if (lastSpace < 0) {
                                        lastSpace = spaceIndex;
                                }
                                subString = texto.substring(0, lastSpace);
                                lines.add(subString);
                                texto = texto.substring(lastSpace).trim();
                                lastSpace = -1;
                        } else if (spaceIndex == texto.length()) {
                                lines.add(texto);
                                texto = "";
                        } else {
                                lastSpace = spaceIndex;
                        }
                }
                return lines;
        }

        public static boolean esVacio(String str) {
                return str == null || "".equals(str.trim());
        }

        public static String filtrar(String str) {

                if (str == null) {
                        return null;
                }
                return str.replace("\u0092", "").replace("\u0094", "'").replace("\u00A0", "").replace("\u0009", "")
                                .replace("\r", "").replace("\n", "");
        }

        public static String filtrarDiacriticos(String cadena) {
                return Normalizer.normalize(cadena, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        }

        public static String getFechaFormato(Calendar fecha) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                sdf.setTimeZone(TimeZone.getDefault());
                return sdf.format(fecha.getTime());
        }

        /*
        public static String getDecimal(String cadena) {
                return getDecimal(cadena, 2, true);
        }
        */

       
        public static String getDecimal(String cadena, boolean inCentimos) {
                return getDecimal(cadena, 2, inCentimos);
        }

        public static String getDecimal(String cadena, int decimales, boolean inCentimos) {
                try {
                        if (UtilStrings.esVacio(cadena)) {
                                return "";
                        }

                        BigDecimal numero = new BigDecimal(cadena);

                        return getDecimal(numero, decimales, inCentimos);
                       
                } catch (NumberFormatException e) {
                        return null;
                }
        }

        public static String getDecimal(BigDecimal numero, int decimales, boolean inCentimos) {
                // Compruena que no sea cero
                if (numero.equals(new BigDecimal(0))) {
                        return "";
                } else {
                        // Formatea el número que llega en céntimos de euro sin separadores
                        DecimalFormat df = getDecimalFormat();
                        df.setGroupingUsed(true);
                        df.setGroupingSize(3);
                        df.setMinimumFractionDigits(decimales);
                        df.setMaximumFractionDigits(decimales);
                        if (inCentimos) {
                                numero = numero.divide(new BigDecimal(100));
                        }
                        return df.format(numero);
                }
        }
       
        public static DecimalFormat getDecimalFormat() {
                NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
                return (DecimalFormat) nf;
        }

        public static List<Texto> separarBloquesTextoFormato(String str) {
                List<Texto> res = new ArrayList<Texto>();
                boolean bold = false;
                boolean underline = false;
                StringBuilder sb = new StringBuilder();
                for (char c : str.toCharArray()) {
                        if (c == '*') {
                                if (bold) {
                                        bold = false;
                                        if (sb.length() > 0) {
                                                res.add(new Texto(sb.toString(), Tipo.BOLD));
                                        }
                                        sb = new StringBuilder();
                                } else {
                                        bold = true;
                                        if (sb.length() > 0) {
                                                res.add(new Texto(sb.toString(), Tipo.NORMAL));
                                        }
                                        sb = new StringBuilder();
                                }
                        } else if (c == '_') {
                                if (underline) {
                                        underline = false;
                                        if (sb.length() > 0) {
                                                res.add(new Texto(sb.toString(), Tipo.UNDERLINE));
                                        }
                                        sb = new StringBuilder();
                                } else {
                                        underline = true;
                                        if (sb.length() > 0) {
                                                res.add(new Texto(sb.toString(), Tipo.NORMAL));
                                        }
                                        sb = new StringBuilder();
                                }
                        } else {
                                sb.append(c);
                        }
                }
                if (sb.length() > 0) {
                        res.add(new Texto(sb.toString(), Tipo.NORMAL));
                }

                return res;
        }

}