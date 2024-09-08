package it.polito.pos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class PosApp{
    List<String> lines=new LinkedList<>();
    HashMap<String,IssuerClass> issuers=new HashMap<>();
    HashMap<String,Issuer> issuerMap=new HashMap<>();
    private Map<String,String> numbertocardtoprovider=new HashMap<>();
    LocalDate currenDate=LocalDate.now();
    Status status=Status.IDLE;
    TreeMap<LocalDate,Payment> payments=new TreeMap<>();
    TreeMap<String,Card> cards=new TreeMap<>();
    private Map<String, List<String>> issuersTxMap = new HashMap<>();
    private Map<String, Double> issuerTotalMap = new HashMap<>();
    Card card;
    Double amount;
    int wrong_pins=0;
    String currentTr;
    String currentIssuer;
    String currentCardNumber;
    

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
        if (line1 == null || line2 == null || line3 == null) {
            throw new PosException("lines cannot be null");
        }
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
        return String.join("\n", lines);
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
            this.numbertocardtoprovider.put(s, name);
        }
        issuerTotalMap.put(name, 0.0);
        issuersTxMap.put(name, new ArrayList<>());

        IssuerClass issuer=new IssuerClass(name, idNumbers);
        issuers.put(name, issuer);
        issuerMap.put(name, server);
        


        

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
        return issuers.values().stream().filter(c->c.controlNum(cardNumber)).map(IssuerClass:: getName).findFirst().orElseThrow(()-> new PosException("invalid"));
        
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
            this.amount=amount;

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
        if (this.status == Status.DECLINED || this.status == Status.STARTED) {
            try {
                String issuerName = this.getIssuer(cardNumber);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMyy");
                YearMonth cardExpiryDate = YearMonth.parse(expiration, formatter);
                LocalDate cardExpiryLocalDate = cardExpiryDate.atEndOfMonth();

                if (this.currentDate().isBefore(cardExpiryLocalDate)) {
                    int sum = 0;
                    boolean second = false;

                    for (int i = cardNumber.length() - 1; i >= 0; i--) {
                        // Get each character and convert it to a digit
                        char digitChar = cardNumber.charAt(i);
                        int digit = Character.getNumericValue(digitChar);  // Convert char to int

                        if (second) {
                            if (digit * 2 >= 10) {
                                sum += ((digit * 2) / 10) + ((digit * 2) % 10);
                            } else {
                                sum += digit * 2;
                            }
                            second = false;
                        } else {
                            sum += digit;
                            second = true;
                        }
                    }

                    if (sum % 10 == 0) {
                        this.status = Status.READ;
                        this.currentIssuer = issuerName;
                        this.currentCardNumber = cardNumber;
                    } else {
                        this.status = Status.DECLINED;
                        throw new PosException("Card not valid, invalid parity digit");
                    }
                    
                } else {
                    this.status = Status.DECLINED;
                    throw new PosException("readStripe: Card has expired");
                }
            } catch (PosException e) {
                this.status = Status.DECLINED;
                throw e;
            }
            
        } else {
            throw new PosException("readStripe: POS status not STARTED or DECLINED");
        
    }
       /* if (status != Status.STARTED) {
            status=Status.DECLINED;
            throw new PosException("not valid status");

        }
        try{
            getIssuer(cardNumber);
        }catch(PosException e){
            status=Status.DECLINED;
            throw new PosException("not valid credit card");
        }
        Card card1=new Card(cardNumber, client, expiration);
        cards.put(cardNumber, card1);
        card =card1;

        status=Status.READ;
        */
        
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
        
        if (status == Status.READ || (status == Status.WRONG_PIN)) {

            Issuer issuer = this.issuerMap.get(this.currentIssuer);
            TransactionResult result = issuer.validatePurchase(this.currentCardNumber, pin, this.amount);
            if (result.getResultCode() == TransactionResult.OK) {
                this.status = Status.SUCCESS;
                double total = this.issuerTotalMap.get(this.currentIssuer);
                this.issuerTotalMap.put(this.currentIssuer, total + this.amount);
                this.issuersTxMap.get(this.currentIssuer).add(result.getId());
                this.currentTr = result.getId();
                return result.getId();
            } else if (result.getResultCode() == TransactionResult.WRONG_PIN) {
                if (this.wrong_pins < 2) {
                    this.wrong_pins += 1;
                    this.status = Status.WRONG_PIN;
                    throw new PosException("performPayment: Wrong pin entered");
                } else {
                    this.wrong_pins = 0;
                    this.status = Status.DECLINED;
                    throw new PosException("performPayment: 3 consecutive wrong pins");
                }
            } else {
                this.status = Status.DECLINED;
                throw new PosException("performPayment: transacton refused by server");
            }
        } else {
            throw new PosException("performPayment: POS state not READ or WRONG_PIN");
        }
        
        
    }


    // R3 - Extended transaction

    /**
     * Makes the POS ready for a new transaction after a successful payment
     * From status SUCCESS moves into IDLE
     */
    public void reset() throws PosException {
        if (status== Status.SUCCESS) {
            this.status = Status.IDLE;
            this.amount = 0.0;
            this.wrong_pins = 0;
        } else {
            throw new PosException("reset: POS status not SUCCESS");
        }
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
        if (status == Status.SUCCESS) {
            Issuer issuer = this.issuerMap.get(this.currentIssuer);
            TransactionResult result = issuer.cancelPurchase(this.currentTr, amount);
            if (result.getResultCode() == TransactionResult.OK) {
                this.status = Status.IDLE;
                Double total = this.issuerTotalMap.get(this.currentIssuer);
                this.issuerTotalMap.put(this.currentIssuer, total - this.amount);
                this.issuersTxMap.get(this.currentIssuer).remove(this.currentTr);
                this.amount = 0.0;
                this.wrong_pins = 0;
                return result.getId();
            } else {
                throw new PosException("cancelTransaction: Server denied cancelling the transaction with id: " + this.currentTr);
            }
        } else {
            throw new PosException("cancelTransaction: POS status not SUCCESS");
        }
    
    }

    /**
     * Terminates the transaction after and error
     * From status WRONG_PIN or DECLINED
     * moved back to IDLE
     */
    public void abortPayment() throws PosException {
        if (status == Status.WRONG_PIN || status == Status.DECLINED) {
            this.status = Status.IDLE;
            this.amount = 0.0;
            this.wrong_pins = 0;
        } else {
            throw new PosException("abortPayment: POS not in status WRONG_PIN or DECLINED");
        }
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
        String res = "";
        res += this.getMerchantInfo() + "\n";
        res += this.currenDate + "\n";
        res += this.currentCardNumber.substring(this.currentCardNumber.length() - 4) + "\n";
        res += this.amount+ "\n";
        if (this.status == Status.SUCCESS || this.status == Status.IDLE) {
            res += "OK\n";
            res += this.currentTr + "\n";
        } else {
            res += "ERROR" + "\n";
        }
        return res;
        
    }

    /**
     * Returns a map having the issuers as keys and the lists
     * of completed transaction IDs as values
     * 
     * @return the map issuer - transaction list
     */
    public Map<String,List<String>> transactionsByIssuer(){
        return this.issuersTxMap;
    }

    /**
     * Returns a map having the issuers as keys and the total
     * amount of successful transactions as values
     * 
     * @return the map issuer - transaction list
     */
    public Map<String,Double> totalByIssuer(){
        return this.issuerTotalMap;
    }

}
