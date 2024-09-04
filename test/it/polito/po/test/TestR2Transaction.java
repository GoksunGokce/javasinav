package it.polito.po.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import it.polito.pos.Issuer;
import it.polito.pos.PosApp;
import it.polito.pos.PosException;
import it.polito.pos.TransactionResult;

public class TestR2Transaction {
    private PosApp pos;
    private Issuer server;
    private TransactionResult result;
    private static final String CC1 = "4321567890008768";
    private static final String CCX = "7310123456789015";
    private static final String TID = "AA998877665544";
    private static final TransactionResult RESULT_OK = new TransactionResult(TransactionResult.OK, TID);
    private static final TransactionResult RESULT_PIN = new TransactionResult(TransactionResult.WRONG_PIN, "Wron PIN");
    private static final TransactionResult RESULT_DECLINED = new TransactionResult(TransactionResult.DECLINED, "Non enough money on account");
    private static final TransactionResult RESULT_TIMEOUT = new TransactionResult(TransactionResult.TIMEOUT, "Server did not respond");


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
    public void testInitialStatus() {
        assertEquals(PosApp.Status.IDLE, pos.getStatus());
    }

    @Test
    public void testBegin() throws PosException {
        assertEquals(PosApp.Status.IDLE, pos.getStatus());
        pos.beginPayment(15.00);
        assertEquals(PosApp.Status.STARTED, pos.getStatus());
    }

    @Test
    public void testDoubleBegin() throws PosException {
        pos.beginPayment(15.00);
        assertThrows("Begin payment should be allowed only in IDLE state",
                      PosException.class,
                      ()->pos.beginPayment(300.00));
    }

    @Test
    public void testReadStripe() throws PosException {
        pos.beginPayment(15.00);
        assertEquals(PosApp.Status.STARTED, pos.getStatus());
        pos.readStripe(CC1, "Trump Donald", "1124" );
    }

    @Test
    public void testReadStripeIdle() {
        assertEquals(PosApp.Status.IDLE, pos.getStatus());
        assertThrows("Cannot read stripe before begin payment",
                      PosException.class,
                      ()->pos.readStripe(CC1, "Trump Donald", "1124" ));
    }

    @Test
    public void testReadStripeWrongIssuer() throws PosException {
        pos.beginPayment(15.00);
        assertEquals(PosApp.Status.STARTED, pos.getStatus());
        assertThrows("Card from unknown issuer should raise exception",
                      PosException.class,
                      ()->pos.readStripe(CCX, "Trump Donald", "1124" ));
        assertEquals(PosApp.Status.DECLINED, pos.getStatus());
    }

    @Test
    public void testPerformPayment() throws PosException {
        pos.beginPayment(15.00);
        pos.readStripe(CC1, "Trump Donald", "1124" );
        assertEquals(PosApp.Status.READ, pos.getStatus());
        result = RESULT_OK;
        String tid = pos.performPayment("12345");

        assertEquals("Wrong transaction ID", TID, tid);
        assertEquals("Wrong status", PosApp.Status.SUCCESS, pos.getStatus());

    }

    @Test
    public void testWrongPIN() throws PosException {
        pos.beginPayment(15.00);
        pos.readStripe(CC1, "Trump Donald", "1124" );
        assertEquals(PosApp.Status.READ, pos.getStatus());
        result = RESULT_PIN;
        assertThrows("Wrong PIN result should trigger exception",
                      PosException.class,
                      ()->pos.performPayment("54321"));

        assertEquals(PosApp.Status.WRONG_PIN, pos.getStatus());
        
        result = RESULT_OK;
        String tid = pos.performPayment("12345");

        assertEquals(TID, tid);
        assertEquals(PosApp.Status.SUCCESS, pos.getStatus());
    }

    @Test
    public void testDeclined() throws PosException {
        pos.beginPayment(1500.00);
        pos.readStripe(CC1, "Trump Donald", "1124" );
        assertEquals(PosApp.Status.READ, pos.getStatus());
        result = RESULT_DECLINED;
        assertThrows("Declined transaction should trigger exception",
                      PosException.class,
                      ()->pos.performPayment("12345"));

        assertEquals(PosApp.Status.DECLINED, pos.getStatus());
        // swipe again
        pos.readStripe(CC1, "Trump Donald", "1124" );
        assertEquals(PosApp.Status.READ, pos.getStatus());
        result = RESULT_OK;
        pos.performPayment("12345");
        assertEquals(PosApp.Status.SUCCESS, pos.getStatus());
    }

    @Test
    public void testTimeout() throws PosException {
        pos.beginPayment(1500.00);
        pos.readStripe(CC1, "Trump Donald", "1124" );
        assertEquals(PosApp.Status.READ, pos.getStatus());
        result = RESULT_TIMEOUT;
        assertThrows("Timed out transaction should trigger exception",
                      PosException.class,
                      ()->pos.performPayment("12345"));

        assertEquals(PosApp.Status.DECLINED, pos.getStatus());
        // swipe again
        pos.readStripe(CC1, "Trump Donald", "1124" );
        assertEquals(PosApp.Status.READ, pos.getStatus());
        result = RESULT_OK;
        pos.performPayment("12345");
        assertEquals(PosApp.Status.SUCCESS, pos.getStatus());
    }

}
