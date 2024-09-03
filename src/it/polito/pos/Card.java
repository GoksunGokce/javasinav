package it.polito.pos;

public class Card {
    private String cardNumber;
    private String cardHolder;
    private String expirationDate;
    private IssuerClass issuer;
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
    public IssuerClass getIssuer() {
        return issuer;
    }
    public String getCardPin() {
        return cardPin;
    }
    
    


}
