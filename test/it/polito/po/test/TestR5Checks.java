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

public class TestR5Checks {
    private PosApp pos;
    private Issuer server;
    private TransactionResult result;
    private static final String CC1 = "4321567890008768";
    private static final String CC1_ERR = "4321567890008763"; // parity error


    @Before
    public void setUp() {
        pos = new PosApp();
        pos.setCurrentDate(LocalDate.of(2024,8,3));
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
    public void TestExpired() throws PosException{
        pos.beginPayment(15.00);
        assertThrows("Expired card should trigger exception",
                    PosException.class,
                    ()->pos.readStripe(CC1, "Trump Donald", "0121" ));
        
        assertEquals(PosApp.Status.DECLINED, pos.getStatus());
    }

    @Test
    public void TestParity() throws PosException{
        pos.beginPayment(15.00);

        assertThrows("Parity error should trigger exception",
                     PosException.class,
                     ()->pos.readStripe(CC1_ERR, "Trump Donald", "1124" ));

        assertEquals(PosApp.Status.DECLINED, pos.getStatus());
    }
}
