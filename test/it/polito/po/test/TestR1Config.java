package it.polito.po.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import it.polito.pos.Issuer;
import it.polito.pos.PosApp;
import it.polito.pos.PosException;
import it.polito.pos.TransactionResult;

public class TestR1Config {
    private PosApp pos;
    private Issuer server;
    private static final String CC1 = "4321567890008768";
    private static final String CC2 = "5310123456789019";
    private static final String CCX = "7310123456789015";


    @Before
    public void setUp() {
        pos = new PosApp();

        server = new Issuer() {
            // this is a mock implementation used for testing
            @Override
            public TransactionResult validatePurchase(String cardNumber, String pin, double amount) {
                return null; 
            }
            @Override
            public TransactionResult cancelPurchase(String tid, double amount) {
                return null;
            }
        };
        pos.registerIssuer("Visa",server,"4");
        pos.registerIssuer("Mastercard",server,"51","52","53","54","55");
    }

    @Test
    public void testCurrentDate() {
        pos.setCurrentDate(LocalDate.of(2024,5,1));

        LocalDate current = pos.currentDate();
        assertNotNull("Missing current date", current);
        assertEquals("Wrong month in current date", 5,current.getMonthValue());
        assertEquals("Wrong year in current date",2024,current.getYear());
        assertEquals("Wrong day in current date",1,current.getDayOfMonth());
    }

    @Test
    public void testCurrentDateDefault() {
        LocalDate today = LocalDate.now();

        LocalDate current = pos.currentDate();
        assertNotNull("Missing current date", current);
        assertEquals("Default date should be today", today.getMonthValue(),current.getMonthValue());
        assertEquals("Default date should be today",today.getYear(),current.getYear());
        assertEquals("Default date should be today",today.getDayOfMonth(),current.getDayOfMonth());
    }

    @Test
    public void testInfo() throws PosException{
        pos.setMerchantInfo("Il PoliKiosko", "C.Duca degli Abruzzi", "Torino");

        String info = pos.getMerchantInfo();
        assertNotNull(info);
        assertTrue(info.startsWith("Il"));
        String[] lines = info.split("\n");
        assertEquals("There should be three lines", 3, lines.length);
        assertEquals("C.Duca degli Abruzzi",lines[1]);
        assertEquals("Torino",lines[2]);
    }

    @Test
    public void testInfoError() {
        assertThrows("Null lines should raise exception",
                     PosException.class,
                     ()->pos.setMerchantInfo(null, "line2", "line3"));

        assertThrows("Null lines should raise exception",
                     PosException.class,
                     ()->pos.setMerchantInfo("line1", null, "line3"));

        assertThrows("Null lines should raise exception",
                     PosException.class,
                     ()->pos.setMerchantInfo("line1", "line2", null));

        String longLine = "0123456798ABCDEFGHIJKXXXXX";
        assertThrows("Lines longer than 20 chars should raise exception",
                     PosException.class,
                     ()->pos.setMerchantInfo(longLine, "line2", "line3"));

        assertThrows("Lines longer than 20 chars should raise exception",
                     PosException.class,
                     ()->pos.setMerchantInfo("line1", longLine, "line3"));

        assertThrows("Lines longer than 20 chars should raise exception",
                     PosException.class,
                     ()->pos.setMerchantInfo("line1", "line2", longLine));
    }

    @Test
    public void testIssuer() throws PosException{

        String issuer1 = pos.getIssuer(CC1);
        assertNotNull(issuer1);
        assertEquals("Visa", issuer1);

        String issuer2 = pos.getIssuer(CC2);
        assertEquals("Mastercard", issuer2);
    }

    @Test
    public void testNoIssuer() {
        assertThrows("Unrecognizes issuer should raise exception",
                      PosException.class,
                      ()->pos.getIssuer(CCX));
    }

}
