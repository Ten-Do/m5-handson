import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

/**
 * Three seed tests. Coverage is intentionally light:
 *  - computeIncomeTax: only the middle slab branch is hit
 *  - computeVAT: only the 18% rate is hit
 *  - applyExemption: only the "under cap" branch is hit
 *  - roundToPaise: not exercised
 *  - isEligibleForReturn: not exercised at all
 */
public class TaxCalculatorTest {

    @Test
    public void incomeTax_middleSlab() {
        TaxCalculator c = new TaxCalculator();
        BigDecimal tax = c.computeIncomeTax(new BigDecimal("700000"));
        // 12500 (5% on 250k-500k) + 40000 (20% on 500k-700k) = 52500
        assertEquals(0, tax.compareTo(new BigDecimal("52500.00")));
    }

    @Test
    public void vat_eighteenPercent() {
        TaxCalculator c = new TaxCalculator();
        BigDecimal gst = c.computeVAT(new BigDecimal("1000"), 18);
        assertEquals(0, gst.compareTo(new BigDecimal("180.00")));
    }

    @Test
    public void exemption_underCap() {
        TaxCalculator c = new TaxCalculator();
        BigDecimal net = c.applyExemption(
                new BigDecimal("600000"), new BigDecimal("120000"));
        assertEquals(0, net.compareTo(new BigDecimal("480000")));
    }

    // ---- Session 5A Part B: AI-generated tests for isEligibleForReturn ----
    // Contract: a return must be filed when gross income is strictly above the
    // basic exemption limit for the taxpayer's age band (below 60: 2.5 L,
    // 60-79: 3 L, 80+: 5 L). Invalid input (null income, negative age) is
    // treated as "not eligible" rather than throwing.

    @Test
    public void isEligibleForReturn_nullIncome_returnsFalse() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act
        boolean eligible = c.isEligibleForReturn(null, 40);
        // Assert
        assertFalse(eligible);
    }

    @Test
    public void isEligibleForReturn_negativeAge_returnsFalse() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act
        boolean eligible = c.isEligibleForReturn(new BigDecimal("1000000"), -1);
        // Assert
        assertFalse(eligible);
    }

    @Test
    public void isEligibleForReturn_zeroIncome_returnsFalse() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act
        boolean eligible = c.isEligibleForReturn(BigDecimal.ZERO, 30);
        // Assert
        assertFalse(eligible);
    }

    @Test
    public void isEligibleForReturn_under60IncomeAtLimit_returnsFalse() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act
        boolean eligible = c.isEligibleForReturn(new BigDecimal("250000"), 30);
        // Assert
        assertFalse(eligible);
    }

    @Test
    public void isEligibleForReturn_under60IncomeJustAboveLimit_returnsTrue() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act
        boolean eligible = c.isEligibleForReturn(new BigDecimal("250000.01"), 30);
        // Assert
        assertTrue(eligible);
    }

    @Test
    public void isEligibleForReturn_seniorIncomeAtLimit_returnsFalse() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act
        boolean eligible = c.isEligibleForReturn(new BigDecimal("300000"), 65);
        // Assert
        assertFalse(eligible);
    }

    @Test
    public void isEligibleForReturn_seniorIncomeJustAboveLimit_returnsTrue() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act
        boolean eligible = c.isEligibleForReturn(new BigDecimal("300000.01"), 65);
        // Assert
        assertTrue(eligible);
    }

    @Test
    public void isEligibleForReturn_superSeniorIncomeAtLimit_returnsFalse() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act
        boolean eligible = c.isEligibleForReturn(new BigDecimal("500000"), 85);
        // Assert
        assertFalse(eligible);
    }

    @Test
    public void isEligibleForReturn_superSeniorIncomeJustAboveLimit_returnsTrue() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act
        boolean eligible = c.isEligibleForReturn(new BigDecimal("500000.01"), 85);
        // Assert
        assertTrue(eligible);
    }

    @Test
    public void isEligibleForReturn_age60IncomeBetweenLimits_returnsFalse() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act: 2.8 L is above the under-60 limit but within the senior limit
        boolean eligible = c.isEligibleForReturn(new BigDecimal("280000"), 60);
        // Assert
        assertFalse(eligible);
    }

    @Test
    public void isEligibleForReturn_age80IncomeBetweenLimits_returnsFalse() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act: 4 L is above the senior limit but within the super-senior limit
        boolean eligible = c.isEligibleForReturn(new BigDecimal("400000"), 80);
        // Assert
        assertFalse(eligible);
    }

    @Test
    public void isEligibleForReturn_salariedAdultTwelveLakh_returnsTrue() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act
        boolean eligible = c.isEligibleForReturn(new BigDecimal("1200000"), 35);
        // Assert
        assertTrue(eligible);
    }

    // ---- Session 5A Part D: targeted test for surviving mutant ----
    // Mutant at line 70: `ageYears < 0` -> `ageYears <= 0`. Age 0 is a valid
    // age, so a newborn with income above the under-60 limit must be eligible.

    @Test
    public void isEligibleForReturn_ageZeroIncomeAboveLimit_returnsTrue() {
        // Arrange
        TaxCalculator c = new TaxCalculator();
        // Act
        boolean eligible = c.isEligibleForReturn(new BigDecimal("400000"), 0);
        // Assert
        assertTrue(eligible);
    }
}
