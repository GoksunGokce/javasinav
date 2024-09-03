package it.polito.pos;

import java.time.LocalDate;

import it.polito.pos.PosApp.Status;

public class Payment {
    private LocalDate time;
    private double amount;
    private Card card;
    public Payment(LocalDate time, double amount) {
        this.time = time;
        this.amount = amount;
    }
    public LocalDate getTime() {
        return time;
    }
    public double getAmount() {
        return amount;
    }
    

}
