package it.polito.pos;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class IssuerClass implements Issuer{
    private String name;
    private List<String> carIIn=new LinkedList<>();
    TransactionResult transResult;
    

    public boolean controlNum(String cardNumber){
        List<String> list1=new ArrayList<>();
        return true;
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
    
    @Override
    public TransactionResult validatePurchase(String cardNumber, String pin, double amount) {
        TransactionResult tr=new TransactionResult(10,null);
        transResult=tr;
        return tr;

    }


    @Override
    public TransactionResult cancelPurchase(String tid, double amount) {
      return transResult;
    }



    public TransactionResult getTransResult() {
        return transResult;
    }

}
