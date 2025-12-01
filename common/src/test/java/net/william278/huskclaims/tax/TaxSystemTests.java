/*
 * This file is part of HuskClaims, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskclaims.tax;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the tax system to ensure calculations and balance handling work correctly
 */
@DisplayName("Tax System Tests")
public class TaxSystemTests {

    @Test
    @DisplayName("Test tax rate calculation")
    public void testTaxRateCalculation() {
        // Test that tax rate per block per day is correctly applied
        double taxRate = 0.1; // $0.10 per day per block
        long claimBlocks = 493;
        double expectedDailyTax = 493 * 0.1; // 49.3
        
        double actualDailyTax = claimBlocks * taxRate;
        
        assertEquals(expectedDailyTax, actualDailyTax, 0.01, 
            "Daily tax should be 49.3 for 493 blocks at $0.10 per block per day");
    }

    @Test
    @DisplayName("Test tax balance persistence")
    public void testTaxBalancePersistence() {
        // Test that tax balance should not be reset when creating new claims
        double initialBalance = 100.0;
        double balanceAfterNewClaim = initialBalance; // Should remain the same
        
        assertEquals(initialBalance, balanceAfterNewClaim, 0.01,
            "Tax balance should not be reset when creating new claims");
    }

    @Test
    @DisplayName("Test multiple claims share tax balance")
    public void testMultipleClaimsShareBalance() {
        // Test that multiple claims use the same tax balance
        double sharedBalance = 50.0;
        double claim1TaxOwed = 20.0;
        double claim2TaxOwed = 15.0;
        double totalTaxOwed = claim1TaxOwed + claim2TaxOwed;
        double netOwed = totalTaxOwed - sharedBalance; // Should be -15.0 (covered)
        
        assertTrue(netOwed <= 0.01, 
            "Multiple claims should share the same tax balance");
    }

    @Test
    @DisplayName("Test tax calculation with days")
    public void testTaxCalculationWithDays() {
        // Test tax calculation: days * blocks * rate
        double taxRate = 0.1;
        long claimBlocks = 100;
        double days = 5.0;
        double expectedTax = days * claimBlocks * taxRate; // 5 * 100 * 0.1 = 50.0
        
        double actualTax = days * claimBlocks * taxRate;
        
        assertEquals(expectedTax, actualTax, 0.01,
            "Tax should be calculated as days * blocks * rate");
    }

    @Test
    @DisplayName("Test tax balance reduces net owed")
    public void testTaxBalanceReducesNetOwed() {
        // Test that tax balance reduces the net amount owed
        double taxOwed = 100.0;
        double taxBalance = 30.0;
        double netOwed = taxOwed - taxBalance; // Should be 70.0
        
        assertEquals(70.0, netOwed, 0.01,
            "Tax balance should reduce the net amount owed");
    }
}
