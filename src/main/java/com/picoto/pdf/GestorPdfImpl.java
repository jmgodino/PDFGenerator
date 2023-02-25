package com.picoto.pdf;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.fontbox.ttf.OTFParser;
import org.apache.fontbox.ttf.OpenTypeFont;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.encoding.Encoding;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.util.Matrix;

public class GestorPdfImpl implements GestionPdf {
	private PDDocument documentoPdf;
	private PDFont fuenteLetra;
	private PDFont fuenteLetraBold;
	private PDPageContentStream _canvas;
	private PDPage _currentPage;
	private boolean ignorarMedatatos = false;
	private Map<String, PDDocument> linkedDocs = new HashMap<>();
	GestorTags gestorTags;

	public void iniciarDocumento(String idioma) throws IOException {
		iniciarDocumento(idioma, null);
	}

	public void iniciarDocumento(String idioma, String csv) throws IOException {
		try {
			String titulo = "Justificante XXX";
			String autor = "XXX";
			String claves = "XXX, Justificante";
			String software = "XXX";

			// Crea un documento vacio
			documentoPdf = new PDDocument();
			documentoPdf.setVersion(1.7f);

			// Cargamos la fuente
			cargarFuentePrincipal();

			if (UtilStrings.esVacio(idioma)) {
				idioma = "ES";
			} else {
				idioma = idioma.toUpperCase();
				if (idioma.length() > 2) {
					idioma = idioma.substring(0, 2);
				}
			}

			Calendar fecha = new GregorianCalendar();
			createXMPMetadata(new COSStream(), titulo, software, claves, software, fecha);
			createDocumentInfo(titulo, autor, claves, software, fecha);

			// Referencia a la fuente
			PDResources resources = new PDResources();
			resources.put(COSName.getPDFName(getBaseFont().getName()), getBaseFont());
			resources.put(COSName.getPDFName(getBaseFontBold().getName()), getBaseFontBold());
			generarDatosCodigoColor(documentoPdf);

			gestorTags = new GestorTags(documentoPdf, titulo, idioma, ignorarMedatatos);
		} catch (IOException e) {
			throw new IOException("Error abriendo el fichero: " + e.getMessage());
		}
	}

	private void createDocumentInfo(String titulo, String autor, String claves, String software, Calendar fecha) {
		PDDocumentInformation docInfo = documentoPdf.getDocumentInformation();
		docInfo.setTitle(titulo);
		docInfo.setSubject(titulo);
		docInfo.setCreator(autor);
		docInfo.setAuthor(autor);
		// Conflicto aunque coincidan con datos de XMP, lo quitamos
		// docInfo.setCreationDate(fecha);
		// docInfo.setModificationDate(fecha);
		//docInfo.setCreator(autor);
		//docInfo.setKeywords(claves);
		//docInfo.setProducer(software);
	}

	protected void cargarFuentePrincipal() throws IOException {
		cargarFuentePrincipalTtf();
	}

	protected void cargarFuentePrincipalTtf() throws IOException {
		// Carga la fuente. En ambos casos se embeben para evitar problema con PDF-A

		ClassLoader contextClassLoader = this.getClass().getClassLoader();
		Encoding encoding = Encoding.getInstance(COSName.WIN_ANSI_ENCODING);

		InputStream is = contextClassLoader.getResourceAsStream("Fuentes/nimbusSansRegular.ttf");
		fuenteLetra = PDTrueTypeFont.load(documentoPdf, is, encoding);

		InputStream is2 = contextClassLoader.getResourceAsStream("Fuentes/nimbusSansBold.ttf");
		fuenteLetraBold = PDTrueTypeFont.load(documentoPdf, is2, encoding);

		is.close();
		is2.close();

	}

	protected void cargarFuentePrincipalOtf() throws IOException {
		// Carga la fuente. En ambos casos se embeben para evitar problema con PDF-A

		ClassLoader contextClassLoader = this.getClass().getClassLoader();
		InputStream is = contextClassLoader.getResourceAsStream("Fuentes/nimbus.otf");

		OTFParser parser = new OTFParser();
		OpenTypeFont font = (OpenTypeFont) parser.parseEmbedded(is);
		fuenteLetra = PDType0Font.load(documentoPdf, font, false);

		InputStream is2 = contextClassLoader.getResourceAsStream("Fuentes/nimbusbold.otf");
		OpenTypeFont fontBold = (OpenTypeFont) parser.parseEmbedded(is2);
		fuenteLetraBold = PDType0Font.load(documentoPdf, fontBold, false);

		is.close();
		is2.close();

	}

	public ByteArrayOutputStream getContenidoPdf() throws IOException {
		if (documentoPdf.getNumberOfPages() == 0) {
			throw new RuntimeException("Se intento generar un documento sin ninguna página");
		}

		// Damos un tamaño de 500Kb por defecto
		gestorTags.incorporarTags();
		ByteArrayOutputStream salidaPdf = new ByteArrayOutputStream(500000);
		getCanvas().close();
		comprimir();
		documentoPdf.save(salidaPdf);
		documentoPdf.close();
		recoletarBasura();
		return salidaPdf;
	}

	private void recoletarBasura() {
		linkedDocs.keySet().stream().forEach(name -> {
			try {
				linkedDocs.get(name).close();
			} catch (IOException e) {
				throw new RuntimeException("Error liberando recursos a cerrar PDF");
			}
		});
		linkedDocs.clear();
	}

	private void comprimir() {
		// Ya no hace falta desde que es PDF-A. No se reduce tamaño

		/*
		 * Map<String, COSBase> fontFileCache = new HashMap<>(); for (int pageNumber =
		 * 0; pageNumber < documentoPdf.getNumberOfPages(); pageNumber++) { final PDPage
		 * page = documentoPdf.getPage(pageNumber); COSDictionary pageDictionary =
		 * (COSDictionary) page.getResources().getCOSObject()
		 * .getDictionaryObject(COSName.FONT); for (COSName currentFont :
		 * pageDictionary.keySet()) { COSDictionary fontDictionary = (COSDictionary)
		 * pageDictionary.getDictionaryObject(currentFont); for (COSName actualFont :
		 * fontDictionary.keySet()) { COSBase actualFontDictionaryObject =
		 * fontDictionary.getDictionaryObject(actualFont); if
		 * (actualFontDictionaryObject instanceof COSDictionary) { COSDictionary
		 * fontFile = (COSDictionary) actualFontDictionaryObject; if
		 * (fontFile.getItem(COSName.FONT_NAME) instanceof COSName) { COSName fontName =
		 * (COSName) fontFile.getItem(COSName.FONT_NAME);
		 * fontFileCache.computeIfAbsent(fontName.getName(), key ->
		 * fontFile.getItem(COSName.FONT_FILE2)); fontFile.setItem(COSName.FONT_FILE2,
		 * fontFileCache.get(fontName.getName())); } } } } }
		 */
	}

	public void addPaginaDePlantilla(LectorPdf lector, int numPagina) throws IOException {
		String name = lector.getName();
		PDDocument doc = null;
		if (linkedDocs.containsKey(name)) {
			doc = linkedDocs.get(name);
			// doc = lector.getCachedDocument();
		} else {
			doc = lector.getCachedDocument();
			linkedDocs.put(name, doc);
		}
		importarPaginaGenerico(name, doc, numPagina);
	}

	public void imprimirPiePagina(boolean addPageNumber, int pagina, String msg, float margenDerecho)
			throws IOException {

		// Obtiene el punto medio en función del ancho de la página actual
		float ancho = getDocumentWidth();

		// Escribe el pie de página si lo hay
		if (msg != null) {
			if (msg.length() > 140) {
				// Texto Centrado (1)
				addTexto(msg.substring(0, 80), ancho / 2, 20, ALIGN_CENTER, 7);
				addTexto(msg.substring(80), ancho / 2, 10, ALIGN_CENTER, 7);
			} else {
				addTexto(msg, ancho / 2, 10, ALIGN_CENTER, 10);
			}
		}

		// Escribe el número de página
		if (addPageNumber) {
			addTexto(String.valueOf(pagina), ancho - margenDerecho, 16, ALIGN_RIGHT, 10);
		}

	}

	// Diagonal por defecto, con texto hueco por defecto. Sin desplazamiento
	public void generarMarcaAgua(int fontSize, Color color, String texto) throws IOException {
		generarMarcaAgua(fontSize, color, texto, true);
	}

	// Diagonal por defecto. Sin desplazamiento. Puede elegir texto hueco o relleno
	public void generarMarcaAgua(int fontSize, Color color, String texto, boolean textoHueco) throws IOException {
		generarMarcaAgua(fontSize, color, texto, textoHueco, 0, 0);
	}

	// Diagonal por defecto. Sin desplazamiento. Puede elegir texto hueco o relleno.
	// Permite desplazar el texto
	public void generarMarcaAgua(int fontSize, Color color, String texto, boolean textoHueco, float despX, float despY)
			throws IOException {
		float alto = getDocumentHeight();
		float ancho = getDocumentWidth();
		generarMarcaAgua(fontSize, color, texto, textoHueco, true, (float) Math.atan2(alto, ancho), despX, despY);
	}

	// Método genérico. Permite definir angulo
	public void generarMarcaAgua(int fontSize, Color color, String texto, boolean textoHueco, boolean centrado,
			float angulo, float despX, float despY) throws IOException {

		PDPageContentStream canvas = getCanvas();

		// guardamos el estado porque vamos a rotar texto y eso luego se aplica al resto
		// de textos
		canvas.saveGraphicsState();
		if (angulo != 0) {
			canvas.transform(Matrix.getRotateInstance(angulo, 0, 0));
		}

		gestorTags.iniciarTagTexto(canvas, _currentPage, texto);

		canvas.beginText();
		canvas.setFont(getBaseFont(), fontSize);
		canvas.setRenderingMode((textoHueco) ? RenderingMode.STROKE : RenderingMode.FILL_STROKE);
		canvas.setStrokingColor(color);

		float totalCubrir = this.getDocumentWidth() / 2;
		if (angulo != 0) {
			if (angulo == Math.toRadians(90)) {
				totalCubrir = this.getDocumentHeight() / 2;
			} else {
				totalCubrir = (float) Math.sqrt(this.getDocumentWidth() / 2 * this.getDocumentWidth() / 2
						+ this.getDocumentHeight() / 2 * this.getDocumentHeight() / 2);
			}
		}

		ubicarTexto(texto, totalCubrir, 0, centrado ? ALIGN_CENTER : ALIGN_LEFT, fontSize, false, canvas, despX, despY);
		canvas.showText(UtilStrings.filtrar(texto));
		canvas.endText();

		gestorTags.terminarTagTexto(canvas, _currentPage);

		// restauramos estado grafico
		canvas.restoreGraphicsState();
	}

	// Texto vertical
	// Convierte las coordenadas originales (x,y) sin rotar, en la pagina rotada 90
	// grados (x,y) -> (y, -1*(x-Ancho))
	// Así el texto sube hacia arriba y no hay que echar cuentas de donde se quiere
	// el texto
	public void generarMarcaAguaVertical(int fontSize, Color color, String texto, boolean textoHueco, float x, float y)
			throws IOException {
		if (texto != null) {
			PDPageContentStream canvas = getCanvas();

			// guardamos el estado porque vamos a rotar texto y eso luego se aplica al resto
			// de textos
			canvas.saveGraphicsState();

			canvas.beginText();

			Matrix matrix = Matrix.getRotateInstance(Math.toRadians(90), 0, 0);
			matrix.translate(0, -1 * this.getDocumentHeight());

			canvas.setTextMatrix(matrix);

			canvas.setFont(getBaseFont(), fontSize);

			canvas.newLineAtOffset(y, getDocumentHeight() - x);

			canvas.showText(UtilStrings.filtrar(texto));
			canvas.endText();

			// restauramos estado grafico
			canvas.restoreGraphicsState();
		}
	}

	public void addPaginaBlanco() throws IOException {
		PDPage page = new PDPage(PDRectangle.A4);
		documentoPdf.addPage(page);
		if (_canvas != null) {
			_canvas.close();
		}

		gestorTags.nuevaPagina(page, documentoPdf.getNumberOfPages());

		_currentPage = page;
		_canvas = new PDPageContentStream(documentoPdf, page, PDPageContentStream.AppendMode.APPEND, true, true);
	}

	public void generarCodigoBarras128(float x, float y, String patron, String cadena, int ancho, int alto)
			throws IOException {

		if (cadena != null) {
			try { // Escribe el código de barras como una imagen
				cadena = cadena.replace("Ñ", "#");
				cadena = UtilStrings.filtrarDiacriticos(cadena);
				BufferedImage imagenBarras = ImageUtils.generarCodigoBarras128(cadena, patron, ancho, alto);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ImageIO.write(imagenBarras, "png", bos);
				PDImageXObject imagen = PDImageXObject.createFromByteArray(documentoPdf, bos.toByteArray(), null);
				gestorTags.iniciarTagImagen(_canvas, _currentPage, "Codigo barras 128: " + cadena, imagen);
				_canvas.drawImage(imagen, x, y, ancho, alto);
				gestorTags.terminarTagImagen(_canvas, _currentPage);
			} catch (IOException e) {
				throw new IOException("Error al insertar el código de barras " + e.getMessage());
			}
		}

	}

	public void generarCodigoBarras417(float x, float y, String cadena, int ancho, int alto) throws IOException {

		if (cadena != null) {
			try { // Escribe el código de barras como una imagen
				cadena = cadena.replace("Ñ", "#");
				cadena = UtilStrings.filtrarDiacriticos(cadena);
				BufferedImage imagenBarras = ImageUtils.generarCodigoBarras417(cadena, "", ancho, alto);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ImageIO.write(imagenBarras, "png", bos);
				PDImageXObject imagen = PDImageXObject.createFromByteArray(documentoPdf, bos.toByteArray(), null);
				gestorTags.iniciarTagImagen(_canvas, _currentPage, "Codigo barras 417: " + cadena, imagen);
				_canvas.drawImage(imagen, x, y, ancho, alto);
				gestorTags.terminarTagImagen(_canvas, _currentPage);
			} catch (IOException e) {
				throw new IOException("Error al insertar el código de barras " + e.getMessage());
			}
		}

	}

	public void generarCodigoQR(float x, float y, String cadena, int ancho, int alto) throws IOException {

		if (cadena != null) {
			try { // Escribe el código de barras como una imagen
				cadena = cadena.replace("Ñ", "#");
				cadena = UtilStrings.filtrarDiacriticos(cadena);
				BufferedImage imagenBarras = ImageUtils.generarCodigoBarrarQR(cadena, "", ancho, alto);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ImageIO.write(imagenBarras, "png", bos);
				PDImageXObject imagen = PDImageXObject.createFromByteArray(documentoPdf, bos.toByteArray(), null);
				gestorTags.iniciarTagImagen(_canvas, _currentPage, "Codigo QR: " + cadena, imagen);
				_canvas.drawImage(imagen, x, y, ancho, alto);
				gestorTags.terminarTagImagen(_canvas, _currentPage);

			} catch (IOException e) {
				throw new IOException("Error al insertar el código de barras " + e.getMessage());
			}
		}

	}

	private void importarPaginaGenerico(String name, PDDocument template, int pagina) throws IOException {

		PDPage paginaPlantilla = template.getPage(pagina - 1); // En PdfBox el índice empieza en 0 no en 1
		PDPage paginaImportar = documentoPdf.importPage(paginaPlantilla);
		gestorTags.nuevaPagina(paginaImportar, documentoPdf.getNumberOfPages());

		if (_canvas != null) {
			_canvas.close();
		}

		_canvas = new PDPageContentStream(documentoPdf, paginaImportar, PDPageContentStream.AppendMode.APPEND, true,
				true);
		_currentPage = paginaImportar;
	}

	public void addTextoFormato(String cadena, float x, float y, int alineacion, int fontSize) throws IOException {
		List<Texto> cadenas = UtilStrings.separarBloquesTextoFormato(cadena);
		float posx = x;
		for (Texto t : cadenas) {
			if (t.getTipo() == Texto.Tipo.NORMAL) {
				addTexto(t.getValor(), posx, y, alineacion, fontSize);
				posx += getTamTexto(t.getValor(), fontSize, false);
			} else if (t.getTipo() == Texto.Tipo.BOLD) {
				addTextoNegrita(t.getValor(), posx, y, alineacion, fontSize);
				posx += getTamTexto(t.getValor(), fontSize, true);
			} else if (t.getTipo() == Texto.Tipo.UNDERLINE) {
				addTextoSubrayado(t.getValor(), posx, y, alineacion, fontSize);
				posx += getTamTexto(t.getValor(), fontSize, false);
			}
		}
	}

	public void addTextoSubrayado(String cadena, float x, float y, int alineacion, int fontSize) throws IOException {
		addTexto(cadena, x, y, alineacion, fontSize);
		addLinea(0.5f, x, y - 1, x + getTamTexto(cadena, fontSize, false), y - 1);
	}

	public void addTextoNegrita(String cadena, float x, float y, int alineacion, int fontSize) throws IOException {
		addTexto(cadena, x, y, alineacion, fontSize, true);
	}

	public void addTexto(String cadena, float x, float y, int alineacion, int fontSize) throws IOException {
		addTexto(cadena, x, y, alineacion, fontSize, false);
	}

	public void addTexto(String cadena, float x, float y, int alineacion, int fontSize, boolean bold)
			throws IOException {
		gestorTags.iniciarTagTexto(_canvas, _currentPage, cadena);

		// Draws the given text centered within the current table cell.
		_canvas.beginText();
		_canvas.setRenderingMode(RenderingMode.FILL);
		_canvas.setStrokingColor(Color.BLACK);
		_canvas.setNonStrokingColor(Color.BLACK);
		if (bold) {
			_canvas.setFont(getBaseFontBold(), fontSize);
		} else {
			_canvas.setFont(getBaseFont(), fontSize);
		}
		ubicarTexto(cadena, x, y, alineacion, fontSize, bold, _canvas, 0, 0);
		_canvas.showText(UtilStrings.filtrar(cadena));
		_canvas.endText();

		gestorTags.terminarTagTexto(_canvas, _currentPage);
	}

	private Coordenadas ubicarTexto(String cadena, float x, float y, int alineacion, int fontSize, boolean bold,
			PDPageContentStream canvas, float despX, float despY) throws IOException {
		Coordenadas coord = new Coordenadas(x + despX, y + despY);
		coord.calcular(alineacion, cadena, fontSize, bold, this);
		canvas.newLineAtOffset(coord.getX(), coord.getY());
		return coord;
	}

	public void addLinea(float grosor, float x1, float y1, float x2, float y2) throws IOException {
		PDPageContentStream canvas = getCanvas();
		canvas.saveGraphicsState();
		canvas.setStrokingColor(Color.BLACK);
		canvas.setLineWidth(grosor);
		canvas.moveTo(x1, y1);
		canvas.lineTo(x2, y2);
		canvas.stroke();
		canvas.restoreGraphicsState();
	}

	public void addRecuadro(float grosor, float x1, float y1, int ancho, int alto) throws IOException {
		addRecuadroColor(x1, y1, ancho, alto, Color.BLACK, grosor, false);
	}

	public void addRecuadroColor(float x, float y, float ancho, float alto, Color color, float grosor, boolean fill)
			throws IOException {
		addRecuadroColorBorde(x, y, ancho, alto, color, color, grosor, fill);
	}

	public void addRecuadroColorBorde(float x, float y, float ancho, float alto, Color relleno, Color borde,
			float grosor, boolean fill) throws IOException {
		PDPageContentStream canvas = getCanvas();
		canvas.saveGraphicsState();
		canvas.setLineWidth(grosor);
		canvas.setStrokingColor(borde);
		canvas.setNonStrokingColor(relleno);

		canvas.addRect(x, y, ancho, alto);

		if (fill) {
			canvas.fillAndStroke();
		} else {
			canvas.stroke();
		}
		canvas.restoreGraphicsState();
	}

	public float getDocumentWidth() {
		try {
			// TODO: Considerar páginas rotadas?
			return (float) (_currentPage.getMediaBox().getWidth());
		} catch (Exception e) {
			return getA4PageWitdh();
		}
	}

	public float getDocumentHeight() {
		try {
			// TODO: Considerar páginas rotadas?
			return (float) (_currentPage.getMediaBox().getHeight());
		} catch (Exception e) {
			return getA4PageHeight();
		}
	}

	private float getTamTexto(String cadena, int fontSize, boolean bold) throws IOException {
		if (bold) {
			return fuenteLetraBold.getStringWidth(UtilStrings.filtrar(cadena)) * fontSize / 1000.0f;
		} else {
			return fuenteLetra.getStringWidth(UtilStrings.filtrar(cadena)) * fontSize / 1000.0f;
		}
	}

	public PDFont getBaseFont() {
		return fuenteLetra;
	}

	private PDFont getBaseFontBold() {
		return fuenteLetraBold;
	}

	private float getA4PageWitdh() {
		return PDRectangle.A4.getWidth();
	}

	private float getA4PageHeight() {
		return PDRectangle.A4.getHeight();
	}

	private PDPageContentStream getCanvas() throws IOException {
		return _canvas;
	}

	public int addParrafo(float x, float y, String texto, boolean justificar, int fontSize, float maxAnchoPixels)
			throws IOException {
		return addParrafo(x, y, texto, justificar, fontSize, maxAnchoPixels, false);
	}

	public int addParrafo(float x, float y, String texto, boolean justificar, int fontSize, float maxAnchoPixels,
			boolean negrita) throws IOException {
		if (UtilStrings.esVacio(texto)) {
			return 0;
		}
		List<String> lineas = UtilStrings.parseaLineas(texto, maxAnchoPixels, fontSize, this, negrita);
		PDPageContentStream canvas = getCanvas();

		gestorTags.iniciarTagTexto(canvas, _currentPage, texto);

		canvas.beginText();
		canvas.setFont(negrita ? getBaseFontBold() : getBaseFont(), fontSize);
		canvas.newLineAtOffset(x, y);
		for (String linea : lineas) {
			float charSpacing = 0;
			if (justificar) {
				if (linea.length() > 1) {
					float size = getLongitudTexto(linea, fontSize, negrita);
					float free = maxAnchoPixels - size;
					if (free > 0 && !lineas.get(lineas.size() - 1).equals(linea)) {
						charSpacing = free / (linea.length() - 1);
					}
				}
			}
			canvas.setCharacterSpacing(charSpacing);
			canvas.showText(UtilStrings.filtrar(linea));
			float margen = -1 * (fontSize + 3);
			canvas.newLineAtOffset(0, margen);
		}
		canvas.endText();

		gestorTags.terminarTagTexto(canvas, _currentPage);

		return lineas.size();
	}

	public void zoomOut(InputStream is, OutputStream os, float zoomLevel) throws IOException {
		zoomOut(is, os, zoomLevel, false);
	}

	public void zoomOut(InputStream is, OutputStream os, float zoomLevel, boolean rotate) throws IOException {
		PDDocument doc = PDDocument.load(is);

		double angulo = 90 * Math.PI / 180; // 90 grados a la izquierda

		for (int p = 0; p < doc.getNumberOfPages(); ++p) {
			PDPage page = doc.getPage(p);
			float ancho = page.getMediaBox().getWidth();
			float alto = page.getMediaBox().getHeight();

			PDPageContentStream contentStreamBefore = new PDPageContentStream(doc, page, AppendMode.PREPEND, true);

			Matrix matrix = new Matrix();
			matrix.scale(zoomLevel, zoomLevel);
			if (rotate) {
				matrix.rotate(angulo);
			}

			contentStreamBefore.transform(matrix);
			contentStreamBefore.close();

			PDRectangle mediaBox = page.getMediaBox();

			// Una vez escalado el contenido, lo recortamos para centrarlo
			float margenH = (ancho - ancho * zoomLevel) / 2;
			float margenV = (alto - alto * zoomLevel) / 2;

			PDRectangle newBox = new PDRectangle();

			if (rotate) {

				newBox.setLowerLeftX(mediaBox.getLowerLeftY() - (alto - margenV));
				newBox.setLowerLeftY(mediaBox.getLowerLeftX() - margenH);
				newBox.setUpperRightX(mediaBox.getUpperRightY() - (alto - margenV));
				newBox.setUpperRightY(mediaBox.getUpperRightX() - margenH);
			} else {
				newBox.setLowerLeftX(mediaBox.getLowerLeftX() - margenH);
				newBox.setLowerLeftY(mediaBox.getLowerLeftY() - margenV);
				newBox.setUpperRightX(mediaBox.getUpperRightX() - margenH);
				newBox.setUpperRightY(mediaBox.getUpperRightY() - margenV);
			}

			page.setMediaBox(newBox);
		}

		doc.save(os);
		doc.close();
	}

	private void createXMPMetadata(COSStream cosStream, String titulo, String autor, String tema, String tool, Calendar fecha)
			throws IOException {
		try {
			/*
			 * No funciona, al menos para validar con Adobe Acrobat PRO. Genera varios nodos
			 * RDF y no le gusta XMPMetadata xmpMetadata = XMPMetadata.createXMPMetadata();
			 *
			 * // XMP Basic properties XMPBasicSchema basicSchema =
			 * xmpMetadata.createAndAddXMPBasicSchema(); Calendar creationDate =
			 * Calendar.getInstance(); basicSchema.setCreateDate(creationDate);
			 * basicSchema.setModifyDate(creationDate);
			 * basicSchema.setMetadataDate(creationDate); basicSchema.setCreatorTool(autor);
			 * basicSchema.setAboutAsSimple("basic");
			 *
			 * // Dublin Core properties DublinCoreSchema dublinCoreSchema =
			 * xmpMetadata.createAndAddDublinCoreSchema();
			 * dublinCoreSchema.setTitle(titulo); dublinCoreSchema.addCreator(autor);
			 * dublinCoreSchema.setDescription(tema);
			 * dublinCoreSchema.setFormat("application/pdf");
			 * dublinCoreSchema.setAboutAsSimple("dublin");
			 *
			 * // PDF/A-1b properties PDFAIdentificationSchema pdfaSchema =
			 * xmpMetadata.createAndAddPFAIdentificationSchema(); pdfaSchema.setPart(1);
			 * pdfaSchema.setConformance("B"); pdfaSchema.setAboutAsSimple("pdfaid");
			 *
			 * AdobePDFSchema adobeSchema = xmpMetadata.createAndAddAdobePDFSchema();
			 * adobeSchema.setProducer(autor); adobeSchema.setKeywords(tema);
			 * adobeSchema.setAboutAsSimple("adobepdf");
			 *
			 * PDFAExtensionSchema pdfaExtension =
			 * xmpMetadata.createAndAddPDFAExtensionSchemaWithDefaultNS();
			 * pdfaExtension.setAboutAsSimple("pdfaExt");
			 *
			 * XMPMediaManagementSchema mmSchema =
			 * xmpMetadata.createAndAddXMPMediaManagementSchema();
			 * mmSchema.setAboutAsSimple("mm");
			 *
			 * XmpSerializer serializer = new XmpSerializer(); ByteArrayOutputStream
			 * xmpOutputStream = new ByteArrayOutputStream();
			 *
			 * serializer.serialize(xmpMetadata, xmpOutputStream, true); String str = new
			 * String(xmpOutputStream.toByteArray()); str =
			 * str.replace("xmlns:ns0=\"http://www.w3.org/XML/1998/namespace\" ns0:", "");
			 */

			InputStream is = this.getClass().getResourceAsStream("/Fuentes/pdfa-xmp.xml");
			PDMetadata metadata = new PDMetadata(documentoPdf);
			String metadatos = new String(IOUtils.toByteArray(is));
			metadata.importXMPMetadata(metadatos.replace("${fecha}", UtilStrings.getFechaFormato(fecha))
					.replace("${titulo}", titulo).replace("${autor}", autor).replace("${tool}", tool).getBytes());
			documentoPdf.getDocumentCatalog().setMetadata(metadata);
			is.close();

		} catch (Exception e) {
			throw new IOException("Error al generar metadatos DC");
		}
	}

	@Override
	public float getLongitudTexto(String texto, int fontSize) throws IOException {
		return getTamTexto(texto, fontSize, false);
	}

	@Override
	public float getLongitudTexto(String texto, int fontSize, boolean bold) throws IOException {
		return getTamTexto(texto, fontSize, bold);
	}

	private void generarDatosCodigoColor(PDDocument doc) throws IOException {
		InputStream colorProfile = this.getClass().getResourceAsStream("/Fuentes/sRGB.icc");
		PDOutputIntent oi = new PDOutputIntent(doc, colorProfile);
		oi.setInfo("sRGB IEC61966-2.1");
		oi.setOutputCondition("sRGB IEC61966-2.1");
		oi.setOutputConditionIdentifier("sRGB IEC61966-2.1");
		oi.setRegistryName("http://www.color.org");
		doc.getDocumentCatalog().addOutputIntent(oi);
		colorProfile.close();
	}

	@Override
	public void addImagen(byte[] contenido, float x, float y, float ancho, float alto, String textoAlt)
			throws IOException {
		PDImageXObject imagen = PDImageXObject.createFromByteArray(documentoPdf, contenido, null);
		gestorTags.iniciarTagImagen(_canvas, _currentPage, textoAlt, imagen);
		_canvas.drawImage(imagen, x, y, ancho, alto);
		gestorTags.terminarTagImagen(_canvas, _currentPage);
	}

	@Override
	public void setIgnorarMetadatos() {
		this.ignorarMedatatos = true;
	}

}