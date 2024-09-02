package it.polito.pos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
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

public class TestPOS {

    private PosApp pos;
    private Issuer server;
    private TransactionResult result;
    private static final String CC1 = "4321567890008768";
    private static final String CC1_ERR = "4321567890008763"; // parity error
    private static final String CC2 = "5310123456789019";
    private static final String TID = "AA998877665544";
    private static final TransactionResult RESULT_OK = new TransactionResult(TransactionResult.OK, TID);
    private static final TransactionResult RESULT_DECLINED = new TransactionResult(TransactionResult.DECLINED, "Non enough money on account");
    private static final TransactionResult RESULT_PIN = new TransactionResult(TransactionResult.WRONG_PIN, "Wron PIN");


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
    }

    @Test
    public void testR1_Config() throws PosException{
        LocalDate current = pos.currentDate();
        assertNotNull(current);
        assertEquals(8,current.getMonthValue());

        String info = pos.getMerchantInfo();
        assertNotNull(info);
        assertTrue(info.startsWith("Il"));
        int posNl = info.indexOf("\n");
        assertTrue(posNl>0);
        assertEquals("C.Duca",info.substring(posNl+1, posNl+7));

        String issuer1 = pos.getIssuer(CC1);
        assertNotNull(issuer1);
        assertEquals("Visa", issuer1);
        String issuer2 = pos.getIssuer(CC2);
        assertEquals("Mastercard", issuer2);
    }

    @Test
    public void testR2_Transaction() throws PosException{
        
        assertEquals(PosApp.Status.IDLE, pos.getStatus());

        pos.beginPayment(15.00);

        assertEquals(PosApp.Status.STARTED, pos.getStatus());

        pos.readStripe(CC1, "Trump Donald", "1124" );

        assertEquals(PosApp.Status.READ, pos.getStatus());

        // attempts a payment with wrong PIN
        result = RESULT_PIN;
        assertThrows(PosException.class,
                     ()->pos.performPayment("00000"));
        assertEquals(PosApp.Status.WRONG_PIN, pos.getStatus());

        // attempts payment but payment is declined by server
        result = RESULT_DECLINED;
        assertThrows(PosException.class,
                    ()->pos.performPayment("12345"));
        assertEquals(PosApp.Status.DECLINED, pos.getStatus());

        // stripe a different card a pays
        pos.readStripe(CC2, "Harris Kamala", "0129" );
        assertEquals(PosApp.Status.READ, pos.getStatus());

        result = RESULT_OK;
        String tid = pos.performPayment("201064");

        assertEquals(TID, tid);
        assertEquals(PosApp.Status.SUCCESS, pos.getStatus());

    }

    @Test
    public void testR3_Extended() throws PosException{
        pos.beginPayment(5.00);
        pos.readStripe(CC1, "Trump Donald", "1124" );
        result = RESULT_OK;
        pos.performPayment("12345");

        assertEquals(PosApp.Status.SUCCESS, pos.getStatus());

        pos.reset();

        assertEquals(PosApp.Status.IDLE, pos.getStatus());

        pos.beginPayment(15.00);
        pos.readStripe(CC1, "Trump Donald", "1124" );
        result = RESULT_DECLINED;
        assertThrows(PosException.class,
                    ()->pos.performPayment("12345"));
        assertEquals(PosApp.Status.DECLINED, pos.getStatus());

        pos.abortPayment();

        assertEquals(PosApp.Status.IDLE, pos.getStatus());

        pos.beginPayment(5.00);
        pos.readStripe(CC1, "Trump Donald", "1124" );
        result = RESULT_OK;
        pos.performPayment("12345");

        assertEquals(PosApp.Status.SUCCESS, pos.getStatus());

        String tid = pos.cancelTransaction();

        assertEquals(TID, tid);
        assertEquals(PosApp.Status.IDLE, pos.getStatus());
    }

    @Test
    public void testR4_Stat() throws PosException{

        pos.beginPayment(5.00);
        pos.readStripe(CC1, "Trump Donald", "1124" );
        result = RESULT_OK;
        pos.performPayment("12345");
        pos.reset();

        pos.beginPayment(15.00);
        pos.readStripe(CC2, "Harris Kamala", "0129" );
        result = RESULT_OK;
        pos.performPayment("201064");
        pos.reset();

        pos.beginPayment(15.00);
        pos.readStripe(CC2, "Harris Kamala", "0129" );
        result = RESULT_OK;
        pos.performPayment("201064");
        pos.reset();

        String receipt = pos.receipt();
        assertTrue(receipt.contains(TID));
        assertTrue(receipt.contains(CC2.substring(12,16)));

        Map<String,List<String>> transactions = pos.transactionsByIssuer();
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
        assertTrue(transactions.containsKey("Visa"));
        assertEquals(2, transactions.get("Mastercard").size());

        Map<String,Double> totals = pos.totalByIssuer();
        assertNotNull(totals);
        assertEquals(2, totals.size());
        assertTrue(totals.containsKey("Visa"));
        assertEquals(5.0, totals.get("Visa"), 0.01);
        assertEquals(30.0, totals.get("Mastercard"), 0.01);
    }

    @Test
    public void TestR5_Checks() throws PosException{
        pos.beginPayment(15.00);

        // attempts payment with expired card
        assertThrows(PosException.class,
                    ()->pos.readStripe(CC1, "Trump Donald", "0121" ));
        
        assertEquals(PosApp.Status.DECLINED, pos.getStatus());

        // attempts payment with wrong card number (parity error)
        assertThrows(PosException.class,
                    ()->pos.readStripe(CC1_ERR, "Trump Donald", "1124" ));

        assertEquals(PosApp.Status.DECLINED, pos.getStatus());
    }

}
