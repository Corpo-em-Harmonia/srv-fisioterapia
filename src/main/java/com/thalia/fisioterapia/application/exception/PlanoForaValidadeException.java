package com.thalia.fisioterapia.application.exception;

public class PlanoForaValidadeException extends RuntimeException {

    private final int duracaoDias;
    private final int validadeGuiaDias;
    private final int frequenciaMinimaSugerida;

    public PlanoForaValidadeException(
            String message,
            int duracaoDias,
            int validadeGuiaDias,
            int frequenciaMinimaSugerida
    ) {
        super(message);
        this.duracaoDias = duracaoDias;
        this.validadeGuiaDias = validadeGuiaDias;
        this.frequenciaMinimaSugerida = frequenciaMinimaSugerida;
    }

    public int getDuracaoDias() {
        return duracaoDias;
    }

    public int getValidadeGuiaDias() {
        return validadeGuiaDias;
    }

    public int getFrequenciaMinimaSugerida() {
        return frequenciaMinimaSugerida;
    }
}
