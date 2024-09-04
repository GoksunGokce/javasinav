package it.polito.po.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import it.polito.pos.Issuer;
import it.polito.pos.PosApp;
import it.polito.pos.PosException;
import it.polito.pos.TransactionResult;

public class TestR4Stats {
    private PosApp pos;
    private Issuer server;
    private TransactionResult result;
    private static final String CC1 = "4321567890008768";
    private static final String CC2 = "5310123456789019";
    private static final TransactionResult RESULT_OK1 = new TransactionResult(TransactionResult.OK, "TID001");
    private static final TransactionResult RESULT_OK2 = new TransactionResult(TransactionResult.OK, "TID002");
    private static final TransactionResult RESULT_OK3 = new TransactionResult(TransactionResult.OK, "TID003");
    private static final TransactionResult RESULT_DECLINED = new TransactionResult(TransactionResult.DECLINED, "Non enough money on account");


    @Before
    public void setUp() throws PosException{
        pos = new PosApp();
        pos.setCurrentDate(LocalDate.of(2024,8,3));
        pos.setMerchantInfo("Il PoliKiosko", "C.Duca degli Abruzzi", "Torino");
        server = new Issuer() {
            // this is a mock implementation used for testing
            @Override
            public TransactionResult validatePurchase(String cardNumber, String pin, double amount) {
                return result; 
            }
            @Override
            public TransactionResult cancelPurchase(String tid, double amount) {
                return result;
            }
        };
        pos.registerIssuer("Visa",server,"4");
        pos.registerIssuer("Mastercard",server,"51","52","53","54","55");

        pos.beginPayment(5.00);
        pos.readStripe(CC1, "Trump Donald", "1124" );
        result = RESULT_OK1;
        pos.performPayment("12345");
        pos.reset();

        pos.beginPayment(15.00);
        pos.readStripe(CC2, "Harris Kamala", "0129" );
        result = RESULT_OK2;
        pos.performPayment("201064");
        pos.reset();

        pos.beginPayment(15.00);
        pos.readStripe(CC2, "Harris Kamala", "0129" );
        result = RESULT_OK3;
        pos.performPayment("201064");
        pos.reset();
    }

    @Test
    public void testReceipt() throws PosException{
        pos.beginPayment(124.00);
        pos.readStripe(CC1, "Harris Kamala", "0129" );
        result = RESULT_OK1;
        pos.performPayment("201064");

        String receipt = pos.receipt();
        assertNotNull("Missing receipt", receipt);
        assertTrue("Merchant info is not in receipt", receipt.contains(pos.getMerchantInfo()));
        assertTrue("Transaction ID is not in receipt", receipt.contains(RESULT_OK1.getId()));
        assertTrue("Amount is not in receipt", receipt.contains("124"));
        assertTrue("Last four digits of card are not in receipt", receipt.contains(CC1.substring(12,16)));
        assertFalse("Card number is in receipt", receipt.contains(CC1));
    }

    @Test
    public void testTransactionsByIssuer(){
        Map<String,List<String>> transactions = pos.transactionsByIssuer();
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
        assertTrue(transactions.containsKey("Visa"));
        assertEquals(2, transactions.get("Mastercard").size());
        assertTrue(transactions.get("Mastercard").contains(RESULT_OK2.getId()));
        assertTrue(transactions.get("Mastercard").contains(RESULT_OK3.getId()));
    }

    @Test
    public void testTransactionsTotals(){
        Map<String,Double> totals = pos.totalByIssuer();
        assertNotNull(totals);
        assertEquals(2, totals.size());
        assertTrue(totals.containsKey("Visa"));
        assertEquals(5.0, totals.get("Visa"), 0.01);
        assertEquals(30.0, totals.get("Mastercard"), 0.01);
    }

    @Test
    public void testTransactionsTotalsOnlySuccess() throws PosException{

        pos.beginPayment(15.00);
        pos.readStripe(CC1, "Harris Kamala", "0129" );
        result = RESULT_DECLINED;
        try{
            pos.performPayment("201064");
        }catch(PosException e){ /* ok */ }

        Map<String,Double> totals = pos.totalByIssuer();
        assertNotNull(totals);
        assertTrue(totals.containsKey("Visa"));
        assertEquals(5.0, totals.get("Visa"), 0.01);
    }

}
