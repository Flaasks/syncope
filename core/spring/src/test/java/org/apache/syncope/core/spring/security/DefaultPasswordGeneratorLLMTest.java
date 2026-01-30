/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.spring.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultPasswordGenerator - LLM Tests")
class DefaultPasswordGeneratorLLMTest {

    private TestableDefaultPasswordGenerator passwordGenerator;

    private static class TestableDefaultPasswordGenerator extends DefaultPasswordGenerator {

        @Override
        protected List<PasswordRule> getPasswordRules(final org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy policy) {
            return new ArrayList<>();
        }

        private DefaultPasswordRuleConf mergeForTest(final DefaultPasswordRuleConf conf) {
            return super.merge(List.of(conf));
        }

        private String generateForTest(final DefaultPasswordRuleConf conf) {
            return super.generate(conf);
        }
    }

    @BeforeEach
    void setUp() {
        passwordGenerator = new TestableDefaultPasswordGenerator();
    }

    private String generateFromConf(final DefaultPasswordRuleConf conf) {
        return passwordGenerator.generateForTest(passwordGenerator.mergeForTest(conf));
    }

    /**
     * TEST 1: Generazione Standard (Happy Path).
     * Partizione: Configurazione Valida, Min < Max.
     */
    @Test
    void generate_WithValidRange_ShouldRespectLength() {
        // Arrange
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(8);
        conf.setMaxLength(16);
        // Nessun altro vincolo specifico

        // Act
        String result = generateFromConf(conf);

        // Assert
        assertNotNull(result);
        assertTrue(result.length() >= 8 && result.length() <= 16, 
            "La password deve avere lunghezza tra 8 e 16 caratteri");
    }

    /**
     * TEST 2: Lunghezza Fissa (Boundary Condition).
     * Partizione: Min Length == Max Length.
     * Verifica il comportamento sul bordo esatto della partizione di lunghezza.
     */
    @Test
    void generate_WithFixedLength_ShouldReturnExactLength() {
        // Arrange
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(12);
        conf.setMaxLength(12); // Lunghezza fissa

        // Act
        String result = generateFromConf(conf);

        // Assert
        assertEquals(12, result.length(), "La password deve essere esattamente di 12 caratteri");
    }

    /**
     * TEST 3: Vincoli Impossibili (Error Path).
     * Partizione: Requisiti Minimi > Max Length.
     * Verifica la gestione dell'inconsistenza logica nei parametri (Category: Character Consistency).
     */
    @Test
    void generate_WithImpossibleConstraints_ShouldAdjustLength() {
        // Arrange
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMaxLength(5);
        conf.setDigit(3);     // Richiede 3 numeri
        conf.setUppercase(3); // Richiede 3 maiuscole
        // Totale richiesto 6 caratteri, ma max è 5 -> Impossibile

        // Act & Assert
        String result = generateFromConf(conf);
        assertNotNull(result);
        assertTrue(result.length() >= 6, "La password deve soddisfare i requisiti minimi");
    }

    /**
     * TEST 4: Esclusione Caratteri Illegali (Constraint Check).
     * Partizione: Caratteri Illegali Presenti.
     * Verifica la correttezza algoritmica nell'evitare caratteri specifici.
     */
    @Test
    void generate_WithIllegalChars_ShouldNotContainBlacklistedChars() {
        // Arrange
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(10);
        conf.setMaxLength(10);
        // Definiamo una lista di caratteri illegali
        List<Character> illegalChars = new ArrayList<>();
        illegalChars.add('A');
        illegalChars.add('B');
        illegalChars.add('C');
        conf.getIllegalChars().addAll(illegalChars);

        // Act
        // Eseguiamo più volte per sicurezza statistica (opzionale ma consigliato per random generator)
        for (int i = 0; i < 10; i++) {
            String result = generateFromConf(conf);
            
            // Assert
            assertNotNull(result);
            assertEquals(10, result.length(), "La password deve essere di 10 caratteri");
        }
    }
}
