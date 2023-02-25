package com.picoto.pdf.test;

public class Partida {

	private String valor;
	private String texto;
	private int id;
	private String rotulo;

	public void setValor(String s) {
		this.valor = s;
	}

	public void setTexto(String s) {
		this.texto = s;
	}

	public void setIdPartida(int i) {
		this.id = i;
	}

	public void setRotulo(String s) {
		this.rotulo = s;
	}

	public String getTexto() {
		return texto;
	}

	public String getValor() {
		return valor;
	}

	public int getPartida() {
		return id;
	}

}
