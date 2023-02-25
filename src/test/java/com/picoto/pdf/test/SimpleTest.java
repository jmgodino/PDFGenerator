package com.picoto.pdf.test;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.picoto.pdf.GestionPdf;
import com.picoto.pdf.LectorPdf;
import com.picoto.pdf.UtilStrings;

public class SimpleTest implements Runnable {

        private static final int MARGEN_IZQ = 28;
        private static final float MARGEN_DER = 30;
        private GestionPdf gestor = GestionPdf.getGestor();
        private Log log = LogFactory.getLog(SimpleTest.class);

        public ByteArrayOutputStream getPlantillaTest() throws IOException {
                String idi = "ES";

               
                gestor.iniciarDocumento(idi, "ASDFASDFASDFASDF");
              
                
                generarContenidoMasivo(idi);
                
                // Añade la página con codigo barras
                gestor.addPaginaDePlantilla(getLectorPlantilla(idi), 1);

                CampoPdf campoPdf = new CampoPdf();
                campoPdf.setX(82.0f);
                campoPdf.setY(342.0f);

                gestor.generarCodigoBarras128(campoPdf.getX(), campoPdf.getX(), "(90)an42",
                                "9051200200000000030902282001501107F12345678Z", 300, 30);
               
                gestor.generarCodigoBarras417(campoPdf.getX(), campoPdf.getX()-50, "90015DIGI202211171714493350001110", 200, 30);

                gestor.generarMarcaAgua(36, Color.GRAY, "EJEMPLO DE MARCA DE AGUA SOLAPADA", true);
                gestor.generarMarcaAgua(8, Color.GRAY, "SIN VALIDAR", true, true, 0, 0, 20);
                gestor.generarMarcaAguaVertical(8, Color.GRAY, "SIN VALIDAR", true, 20, 10);

                // Devuelve el fichero generado
                return gestor.getContenidoPdf();
        }

        protected void putMarcasAgua() throws IOException {
                gestor.generarMarcaAgua(80, Color.BLUE, "Entorno de pruebas", true);
                gestor.generarMarcaAgua(80, Color.BLUE, "no válido", true, 20, -80);
        }

        private void generarContenidoMasivo(String idioma) throws IOException {

                float x = (float) MARGEN_IZQ + 10 * 2;
                for (int i = 0; i < 3; i++) {
                        gestor.addPaginaDePlantilla(getLectorPlantilla(idioma), 1);
                        float posVertical = gestor.getDocumentHeight() - 80;
                        float incVertical = -12;
                        iniciarPaginaDinamica();
                        for (int j = 0; j < 58; j++) {
                                posVertical = posVertical + incVertical;
                                Partida p = new Partida();
                                p.setValor("" + 10000000 * new Random().nextDouble());
                                p.setTexto("Texto xx" + (10 * i + j));
                                p.setIdPartida(10 * i + j);
                                p.setRotulo(String.format("%04d", 10 * i + j));
                                imprimirPartida(p, x, posVertical);
                                if (j == 24) {
                                        posVertical = posVertical + incVertical - 5;
                                        gestor.addRecuadroColor(MARGEN_IZQ, posVertical,
                                                        gestor.getDocumentWidth() - MARGEN_IZQ - MARGEN_DER + 3, 10, Color.LIGHT_GRAY, 1, true);
                                        gestor.addTextoNegrita("Titulo", x, posVertical + 2, GestionPdf.ALIGN_LEFT, 8);
                                }
                        }
                        gestor.generarMarcaAgua(80, Color.BLUE, "Entorno de pruebas", true);
                        gestor.generarMarcaAgua(80, Color.BLUE, "sin validez", true, 20, -80);
                        gestor.addTextoSubrayado("Esto es una prueba de texto subrayado", 40, 30, GestionPdf.ALIGN_LEFT, 12);
                        gestor.addTextoFormato("*Negrita*: _subrayado_ normal", 360, 30, GestionPdf.ALIGN_LEFT, 12);

                        gestor.generarMarcaAgua(36, Color.GRAY, "ES UN TEST, NO VÁLIDO", true);

                        gestor.imprimirPiePagina(true, (i + 1),
                                        "Fin de página",
                                        28.0f);
                }

        }

        private LectorPdf getLectorPlantilla(String idioma) {
                try {
                        return new LectorPdf(IOUtils.resourceToByteArray("com/picoto/pdf/test/test.pdf",
                                        this.getClass().getClassLoader()), "plantillaTest");
                } catch (IOException e) {
                        error(e);
                        throw new RuntimeException("Error al cargar las caratulas: " + e.getMessage());
                }
        }

        private void error(IOException e) {
			System.out.println(e);
			
		}

		private void setCompresion(boolean b) {
                // TODO Auto-generated method stub

        }

        public void iniciarPaginaDinamica() throws IOException {

        }

        public void imprimirPartida(Partida partida, float x, float y) throws IOException {
                int fontSize = 8;

                gestor.addTexto(partida.getTexto(), x, y, GestionPdf.ALIGN_LEFT, fontSize);

                float ancho = gestor.getLongitudTexto(partida.getTexto(), fontSize);

                // Imprime una linea desde el texto al valor
                gestor.addLinea(0.5f, x + ancho + 10, y - 2, gestor.getDocumentWidth() - MARGEN_DER + 3, y - 2);

                // Imprime el valor
                gestor.addTexto(UtilStrings.getDecimal(partida.getValor(), true), gestor.getDocumentWidth() - MARGEN_DER - 16, y, GestionPdf.ALIGN_RIGHT,
                                fontSize + 1);

                String strPartida = ""+partida.getPartida();
                if (strPartida != null) {
                        // Imprime la partida
                        gestor.addTexto(strPartida, gestor.getDocumentWidth() - MARGEN_DER + 1.5f, y, GestionPdf.ALIGN_RIGHT,
                                        fontSize - 2);

                        // Imprime un recuadro para la partida
                        gestor.addRecuadro(1, gestor.getDocumentWidth() - MARGEN_IZQ - MARGEN_DER + 15, y - 2, 16, 10);

                }
        }

        @Override
        public void run() {
                log.info("Generando fichero de prueba");
                try {
                        String fichero = "test-" + System.currentTimeMillis() + ".pdf";
                        FileOutputStream fos = new FileOutputStream(fichero);

                        fos.write(getPlantillaTest().toByteArray());
                        fos.flush();
                        fos.close();

                } catch (Exception e) {
                        e.printStackTrace();
                }

        }

        public static void main(String args[]) throws IOException {
                SimpleTest p = new SimpleTest();

                p.setCompresion(false);
                p.run();

        }

 
}