package com.picoto.pdf;

import java.io.IOException;

public class Coordenadas {
        private float x;
        private float y;

        public Coordenadas(float x, float y) {
                super();
                this.x = x;
                this.y = y;
        }

        public float getX() {
                return x;
        }

        public float getY() {
                return y;
        }

        public void calcular(int alineacion, String cadena, int fontSize, boolean bold, CalcularLongitudTexto calc) throws IOException {
                if (alineacion == GestionPdf.ALIGN_LEFT) {
                        return;
                } else if (alineacion == GestionPdf.ALIGN_CENTER) {
                        float longitud = calc.getLongitudTexto(cadena, fontSize, bold);
                        x = x - (longitud / 2);
                } else {
                        float longitud = calc.getLongitudTexto(cadena, fontSize, bold);
                        x = x - longitud;
                }
        }
}