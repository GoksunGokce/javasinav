package it.polito.pos;

public class Card {
    private String cardNumber;
    private String cardHolder;
    private String expirationDate;
    private String issuer;
    private String cardPin;
    public Card(String cardNumber, String cardHolder, String expirationDate) {
        this.cardNumber = cardNumber;
        this.cardHolder = cardHolder;
        this.expirationDate = expirationDate;
        
    }
    public String getCardNumber() {
        return cardNumber;
    }
    public String getCardHolder() {
        return cardHolder;
    }
    public String getExpirationDate() {
        return expirationDate;
    }
    public String getIssuer() {
        return issuer;
    }
    public String getCardPin() {
        return cardPin;
    }
    public void setCardPin(String cardPin) {
        this.cardPin = cardPin;
    }
    
    


}
