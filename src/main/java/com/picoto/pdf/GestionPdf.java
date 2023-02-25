package com.picoto.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface GestionPdf extends CalcularLongitudTexto {

        // alineaciones de texto
        public static final int ALIGN_LEFT = 0;
        public static final int ALIGN_CENTER = 1;
        public static final int ALIGN_RIGHT = 2;

        // preparada el documento, metadatos y fuentes
        public void iniciarDocumento(String idioma) throws IOException;
       
        // preparada el documento, metadatos y fuentes
        public void iniciarDocumento(String idioma, String csv) throws IOException;

        // recupera el contenido del documento generado
        public ByteArrayOutputStream getContenidoPdf() throws IOException;

        // carga página con la plantilla indicada
        public void addPaginaDePlantilla(LectorPdf lector, int numPagina) throws IOException;

        // carga pagina en blanco
        public void addPaginaBlanco() throws IOException;

        // añade pie de página al documento. Centrado, con numéro de página opcional a la derecha
        public void imprimirPiePagina(boolean addPageNumber, int pagina, String msg, float margenDerecho)
                        throws IOException;

        // genera marca de agua en diagonal
        public void generarMarcaAgua(int fontSize, Color color, String texto, boolean textoHueco) throws IOException;

        // genera marca de agua en diagonal con desplazamiento
        public void generarMarcaAgua(int fontSize, Color color, String texto, boolean textoHueco, float despX, float despY)
                        throws IOException;

        // genera marca de agua, indicando angulo de giro
        public void generarMarcaAgua(int fontSize, Color color, String texto, boolean textoHueco, boolean centrado,
                        float angulo, float despX, float despY) throws IOException;
       
        // texto vertical en la posicion (x,y) del documento sin rotar
        public void generarMarcaAguaVertical(int fontSize, Color color, String texto, boolean textoHueco,
                        float x, float y) throws IOException;

        // código de barras GS1-128
        public void generarCodigoBarras128(float x, float y, String patron, String cadena, int ancho, int alto) throws IOException;
       
        // código de barras PDF417
        public void generarCodigoBarras417(float x, float y,  String cadena, int ancho, int alto) throws IOException;
       
        // código de barras QR
        public void generarCodigoQR(float x, float y, String cadena, int ancho, int alto) throws IOException;

        // texto en negrita
        public void addTextoNegrita(String cadena, float x, float y, int alineacion, int fontSize) throws IOException;
       
        // texto norma. permite alinear
        public void addTexto(String cadena, float x, float y, int alineacion, int fontSize) throws IOException;
       
        // texto genérico
        public void addTexto(String cadena, float x, float y, int alineacion, int fontSize, boolean bold) throws IOException;
       
        // pinta línea
        public void addLinea(float grosor, float x1, float y1, float x2, float y2) throws IOException;
       
        // pinta recuadro (hueco con borde negro)
        public void addRecuadro(float grosor, float x1, float y1, int ancho, int alto) throws IOException;
       
        // pinta recuadro de color, con posibilidad de rellenarlo
        public void addRecuadroColor(float x, float y, float ancho, float alto, Color color, float grosor, boolean fill)
                        throws IOException;
       
        // pinta recuadro de color, con posibilidad de rellenarlo
        public void addRecuadroColorBorde(float x, float y, float ancho, float alto, Color color, Color borde, float grosor, boolean fill)
                        throws IOException;
       
        // pinta un párrafo, pudiendo justificar texto a un ancho en píxeles definido
        public int addParrafo(float x, float y, String texto, boolean justificar, int fontSize,  float maxAnchoPixels)
                        throws IOException;
       
        // pinta un párrafo, pudiendo justificar texto a un ancho en píxeles definido
        public int addParrafo(float x, float y, String texto, boolean justificar, int fontSize,  float maxAnchoPixels, boolean negrita)
                        throws IOException;
       
        // zoom hacia fuera sin cambiar tamaño del PDF
        public void zoomOut(InputStream is, OutputStream os, float zoomLevel) throws IOException;

        // zoom hacia fuera sin cambiar tamaño del PDF y rotacion
        public void zoomOut(InputStream is, OutputStream os, float zoomLevel, boolean rotate) throws IOException;

        // ancho del documento
        public float getDocumentWidth();
       
        // alto del documento
        public float getDocumentHeight();        

        // permite cargar una imagen en el documento, especificando dimensiones
        public void addImagen(byte[] contenido, float x, float y, float ancho, float alto, String textoAlt) throws IOException;
       
        // texto con formato
        public void addTextoFormato(String cadena, float x, float y, int alineacion, int fontSize) throws IOException;
       
        // texto subrayado
        public void addTextoSubrayado(String cadena, float x, float y, int alineacion, int fontSize) throws IOException;
       
        // control metadatos
        public void setIgnorarMetadatos();

        // gestor sobre el que trabajar
        public static GestionPdf getGestor() {
                GestionPdf gestor =  new GestorPdfImpl();
                return gestor;
        }

}