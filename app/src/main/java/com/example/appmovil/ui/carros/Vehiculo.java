package com.example.appmovil.ui.carros;

import java.util.Date;

public class Vehiculo {
    public String placa;
    public String uid;
    public Date horaEntrada;
    public Date horaSalida;

    public Vehiculo(String placa, String uid) {
        this.placa = placa;
        this.uid = uid;
        this.horaEntrada = new Date();
        this.horaSalida = null;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Date getHoraEntrada() {
        return horaEntrada;
    }

    public void setHoraEntrada(Date horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public Date getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(Date horaSalida) {
        this.horaSalida = horaSalida;
    }
    public long calcularTiempoEnParqueadero() {
        return horaSalida.getTime() - horaEntrada.getTime();
    }

}
