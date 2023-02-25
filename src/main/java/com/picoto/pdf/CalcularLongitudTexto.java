package com.picoto.pdf;


import java.io.IOException;

public interface CalcularLongitudTexto {
       
        float getLongitudTexto(String texto, int fontSize) throws IOException;
       
        float getLongitudTexto(String texto, int fontSize, boolean bold) throws IOException;

}