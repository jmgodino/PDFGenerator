package com.picoto.pdf;


public class Texto {

        enum Tipo {
                NORMAL, BOLD, UNDERLINE
        };

       
        private String valor;
        private Tipo tipo;

        public Texto(String valor, Tipo tipo) {
                super();
                this.valor = valor;
                this.tipo = tipo;
        }

        public String getValor() {
                return valor;
        }

        public Tipo getTipo() {
                return tipo;
        }

}