package it.polito.pos;

import java.util.LinkedList;
import java.util.List;

public class IssuerClass implements Issuer{
    private String name;
    private List<String> carIIn=new LinkedList<>();
    TransactionResult transResult;
    int code;
    

    public boolean controlNum(String cardNumber){
        return carIIn.stream().anyMatch(cardNumber::startsWith);
    }
    
    

    public IssuerClass(String name, List<String> carIIn) {
        this.name = name;
        this.carIIn = carIIn;
    }
    

    public void setName(String name) {
        this.name = name;
    }
    


    public String getName() {
        return name;
    }


    public List<String> getCarIIn() {
        return carIIn;
    }
    



    public void setCode(int code) {
        this.code = code;
    }



    @Override
    public TransactionResult validatePurchase(String cardNumber, String pin, double amount) {
        TransactionResult tr=new TransactionResult(code, pin);
        return tr;
        
    }



    @Override
    public TransactionResult cancelPurchase(String tid, double amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'cancelPurchase'");
    }



    

}
