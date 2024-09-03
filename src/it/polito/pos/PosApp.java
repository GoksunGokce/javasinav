package it.polito.pos;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PosApp{
    List<String> lines=new LinkedList<>();
    HashMap<String,IssuerClass> issuers=new HashMap<>();
    LocalDate currenDate=LocalDate.now();
    Status status=Status.IDLE;
    TreeMap<LocalDate,Payment> payments=new TreeMap<>();
    TreeMap<String,Card> cards=new TreeMap<>();
    Card card;
    /**
     * Define the current status of the POS
     */
    public enum Status {
        /** Initial state */
        IDLE, 
        /** State after amount set */
        STARTED, 
        /** State after card swiped */
        READ, 
        /** State after attempted paymend with wrong PIN */
        WRONG_PIN, 
        /** State after attempted paymend with error */
        DECLINED, 
        /** State after paymend successful */
        SUCCESS }

    // R1 - configuration

    /**
     * Register the merchant information as
     * three text lines having max 20 characters length.
     * The lines are then printed on the receipt.
     * 
     * @param line1 first line
     * @param line2 second line
     * @param line3 third line
     * @throws PosException if lines are null or longer that 20 chars
     */
    public void setMerchantInfo(String line1, String line2, String line3) throws PosException {
        if((line1.length()>20 )|| (line2.length()>20) || (line3.length()>20)){
            throw new PosException("lines lenght not valid");
        }
        lines.add(line1);
        lines.add(line2);
        lines.add(line3);

        // ..
    }

    /**
     * Retrieves the lines forming the merchant info
     * 
     * @return the merchant info
     */
    public String getMerchantInfo(){
        for(String s:lines){
            System.out.println(s);
        }
        return null;
    }

    /**
     * Register a server proxy for a given issuer providing
     * a server that is going to support transactions for that issuer
     * and a list of Issuer Identification Number (IIN)
     * 
     * @param name  the name of the card issuer (e.g. VISA)
     * @param server the server object
     * @param iins   the Issuer Identification Numbers
     */
    public void registerIssuer(String name, Issuer server, String... iins){
        List<String> idNumbers=new LinkedList<>();
        for(String s:iins){
            idNumbers.add(s);
        }
        IssuerClass issuer=new IssuerClass(name, idNumbers);
        issuers.put(name, issuer);
        

        // ..
    }

    /**
     * returns the name of the matching card issuer if any.
     * 
     * A card number matches an IIN id the card number starts
     * with the same digits as IIN
     * 
     * @param cardNumber the credit card number
     * @return the name of the card issuer
     * @throws PosException if no issuer IIN match the card number
     */
    public String getIssuer(String cardNumber) throws PosException {
        if(issuers.values().stream().filter(c->c.controlNum(cardNumber)).map(IssuerClass:: getName).toString() == null){
            throw new PosException("invalid");
        }
        return issuers.values().stream().filter(c->c.controlNum(cardNumber)).map(IssuerClass:: getName).toString();
    }

    /**
     * Retrived the current date for the POS system
     * By defaul is the acutal system date. i.e. LocalDate.now()
     * 
     * @return current date
     */
    public LocalDate currentDate(){
        return currenDate;
    }

    /**
     * Sets the new current date for the system.
     * 
     * @param today the new current date
     */
    public void setCurrentDate(LocalDate today){
        currenDate=today;

        // changes the current date
    }
    // R2 - Basic transaction

    /**
     * Retrieves the current status of the POS
     * 
     * @return current status
     */
    public Status getStatus(){
        return status;
    }

    /**
     * Start a payment transaction by defining the amount to be payed
     * The current status must be IDLE
     * Transitions the status into STARTED.
     * 
     * @param amount   payment amount
     * @throws PosException in case the current status is not IDLE
     */
    public void beginPayment(double amount) throws PosException {
        if(status != Status.IDLE){
            
            throw new PosException("not valid status");
           
        }
        else{
            status=Status.STARTED;
            Payment payment=new Payment(currenDate, amount);
            payments.put(currenDate, payment);
        }
        // store the payment amount to begin a transaction
    }


    /**
     * Accepts the data read when the card is swiped through the magnetic stripe reader
     * The current status must be STARTED or DECLINED
     * Transitions the status into READ otherwise it becomes DECLINED
     * 
     * @param cardNumber    card number
     * @param client        client name
     * @param expiration    expiration
     * @throws PosException if the current state is not STARTED or the card data is not correct or card is expired
     */
    public void readStripe(String cardNumber, String client, String expiration) throws PosException {
        if (status != Status.STARTED) {
            status=Status.DECLINED;
            throw new PosException("not valid status");

        }
        if(getIssuer(cardNumber) ==null){
            status=Status.DECLINED;
            throw new PosException("not valid credit card");
        }
        Card card1=new Card(cardNumber, client, expiration);
        cards.put(client, card1);
        card =card1;
        status=Status.READ;
        
    }

    /**
     * Performs the payment with the PIN that has been entered by the user
     * 
     * It contacts the server associated with the card issuer and in case of
     * success returns the transaction ID provided by the issuer server.
     * 
     * The current status must be READ or WRONG_PIN
     * Transitions the status into SUCCESS if successful, 
     * WRONG_PIN if there was a PIN error or DENIED in case of transaction
     * denied by the server.
     * 
     * After three consecutives PIN errors the status is DECLINED
     * 
     * @param pin   the PIN entered by the user
     * @return the transaction ID 
     * @throws PosException in case of error
     */
    public String performPayment(String pin) throws PosException {
        IssuerClass issuer=card.getIssuer();
        if(card.getCardPin().equals(pin)){
            issuer.validatePurchase(card.getCardNumber(), pin, 0);
            status=Status.SUCCESS; 
        }
        else{
            status=Status.DECLINED;
            throw new PosException("undefined pin");
        }
        return null;
    }


    // R3 - Extended transaction

    /**
     * Makes the POS ready for a new transaction after a successful payment
     * From status SUCCESS moves into IDLE
     */
    public void reset() throws PosException {
        // goes to IDLE state again
    }

    /**
     * Cancels (rollback) a successful transaction.
     * Calls the card issuer server providing the transaction ID and the amount
     * From status SUCCESS moves into IDLE if transaction was cancelled
     * 
     * @return ID of the cancelled transaction if server confirmed
     * @throws PosException if not in SUCCESS state or in case of error from the server
     */
    public String cancelTransaction() throws PosException {
        // cancel the latest transaction if successful
        return null;
    }

    /**
     * Terminates the transaction after and error
     * From status WRONG_PIN or DECLINED
     * moved back to IDLE
     */
    public void abortPayment() throws PosException {
        // in case of failed payment 
    }


    // R4 - Stats

    /**
     * Prints the receipt of the latest transaction that contains:
     * 
     * 
     * <ul>
     * <li> the merchant info
     * <li> the date of the payment
     * <li> the last 4 digits of the card number
     * <li> the amount of the payment
     * <li> the result of the payment (OK or ERROR) 
     * <li> the transaction ID if OK
     * </ul>
     * on distinct lines
     * 
     * @return the string of the receipt
     */
    public String receipt(){
        return null;
    }

    /**
     * Returns a map having the issuers as keys and the lists
     * of completed transaction IDs as values
     * 
     * @return the map issuer - transaction list
     */
    public Map<String,List<String>> transactionsByIssuer(){
        return null;
    }

    /**
     * Returns a map having the issuers as keys and the total
     * amount of successful transactions as values
     * 
     * @return the map issuer - transaction list
     */
    public Map<String,Double> totalByIssuer(){
        return null;
    }

}
