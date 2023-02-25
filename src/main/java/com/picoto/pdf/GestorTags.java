package com.picoto.pdf;

import java.io.IOException;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;

public class GestorTags {
        private PDDocument doc = null;
        private int currentMCID = 0;
        private PDStructureElement docElement;
        private PDStructureElement pageElement;
        private COSArray numDictionaries = new COSArray();
        private boolean ignorar;
        private String idioma;

        public GestorTags(PDDocument doc, String titulo, String idioma, boolean ignorar) throws IOException {
                this.doc = doc;
                this.ignorar = ignorar;
                this.idioma = idioma;
                setupDocumentCatalog(idioma, titulo);
        }

        public COSDictionary getNextMarkedContentDictionary() {
                COSDictionary currentMarkedContentDictionary = new COSDictionary();
                currentMarkedContentDictionary.setInt(COSName.MCID, currentMCID++);
                return currentMarkedContentDictionary;
        }

        private void setupDocumentCatalog(String idioma, String titulo) {
                PDDocumentCatalog documentCatalog = doc.getDocumentCatalog();
                documentCatalog.setLanguage(idioma);

                documentCatalog.setViewerPreferences(new PDViewerPreferences(new COSDictionary()));
                documentCatalog.getViewerPreferences().setDisplayDocTitle(true);
                documentCatalog.getCOSObject().setString(COSName.LANG, idioma);
                PDStructureTreeRoot root = new PDStructureTreeRoot();
                documentCatalog.setStructureTreeRoot(root);

                docElement = new PDStructureElement(StandardStructureTypes.DOCUMENT, root);
               
                // Creamos el MCID = 0
                getNextMarkedContentDictionary();

                docElement.setTitle(titulo);
                docElement.setLanguage(idioma);
                docElement.setAlternateDescription("Documento generado por XXX");
                root.appendKid(docElement);

                PDMarkInfo markInfo = new PDMarkInfo();
                markInfo.setMarked(true);
                documentCatalog.setMarkInfo(markInfo);
        }

        public void iniciarTagTexto(PDPageContentStream canvas, PDPage page, String texto) throws IOException {
                if (ignorar) {
                        return;
                }

                COSDictionary currentMarkedContentDictionary = getNextMarkedContentDictionary();

                PDStructureElement nuevoNodo = new PDStructureElement(StandardStructureTypes.P, pageElement);
                nuevoNodo.setPage(page);
                nuevoNodo.setActualText(texto);
                nuevoNodo.appendKid(new PDMarkedContent(COSName.P, currentMarkedContentDictionary));

                pageElement.appendKid(nuevoNodo);

                COSDictionary numDict = new COSDictionary();
                numDict.setInt(COSName.K, currentMCID - 1);
                numDict.setString(COSName.LANG, idioma);
                numDict.setItem(COSName.PG, page.getCOSObject());
                numDict.setItem(COSName.P, nuevoNodo.getCOSObject());

                numDict.setName(COSName.S, COSName.P.getName());
                numDictionaries.add(numDict);

                canvas.beginMarkedContent(COSName.P, PDPropertyList.create(currentMarkedContentDictionary));

        }

        public void iniciarTagImagen(PDPageContentStream canvas, PDPage page, String texto, PDXObject imagen)
                        throws IOException {
                if (ignorar) {
                        return;
                }
               
                COSDictionary currentMarkedContentDictionary = getNextMarkedContentDictionary();

                PDStructureElement nuevoNodo = new PDStructureElement(StandardStructureTypes.Figure, pageElement);
                nuevoNodo.setPage(page);
                nuevoNodo.setAlternateDescription(texto);
                currentMarkedContentDictionary.setString(COSName.ALT, texto);
                PDMarkedContent markedImg = new PDMarkedContent(COSName.IMAGE, currentMarkedContentDictionary);
                markedImg.addXObject(imagen);
                nuevoNodo.appendKid(markedImg);

                pageElement.appendKid(nuevoNodo);

                COSDictionary numDict = new COSDictionary();
                numDict.setInt(COSName.K, currentMCID - 1);
                numDict.setString(COSName.LANG, idioma);
                numDict.setItem(COSName.PG, page.getCOSObject());
                numDict.setItem(COSName.IMAGE, nuevoNodo.getCOSObject());

                numDict.setName(COSName.S, COSName.IMAGE.getName());
                numDictionaries.add(numDict);

                canvas.beginMarkedContent(COSName.IMAGE, PDPropertyList.create(currentMarkedContentDictionary));
        }

        public void terminarTagTexto(PDPageContentStream canvas, PDPage page) throws IOException {
                if (ignorar) {
                        return;
                }
               
                canvas.endMarkedContent();
        }

        public void terminarTagImagen(PDPageContentStream canvas, PDPage page) throws IOException {
                if (ignorar) {
                        return;
                }
               
                canvas.endMarkedContent();
        }

        public void incorporarTags() {
                COSArray nums = new COSArray();
               
                COSDictionary dict = new COSDictionary();
               
                // Fundamental, sin esto no funciona el lector de voz de Acrobat.
                nums.add(COSInteger.get(0)); // Creamos la referencia al MCID con valor 0 (nodo raiz)
                nums.add(numDictionaries);
                dict.setItem(COSName.NUMS, nums);
                PDNumberTreeNode numberTreeNode = new PDNumberTreeNode(dict, dict.getClass());
                // ¿Esto hace algo?
                doc.getDocumentCatalog().getStructureTreeRoot().setParentTreeNextKey(1);
                doc.getDocumentCatalog().getStructureTreeRoot().setParentTree(numberTreeNode);
        }

        public void nuevaPagina(PDPage page, int pagNum) {
                page.getCOSObject().setItem(COSName.STRUCT_PARENTS, COSInteger.get(0));

                pageElement = new PDStructureElement(StandardStructureTypes.SECT, docElement);
                pageElement.setTitle("Pagina " + pagNum);
                pageElement.setPage(page);
               
                docElement.appendKid(pageElement);

        }


}