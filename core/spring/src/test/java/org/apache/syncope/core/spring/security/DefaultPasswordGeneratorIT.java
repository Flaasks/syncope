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

import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DefaultPasswordGenerator - Integration Tests (Failsafe)")
class DefaultPasswordGeneratorIT {

    private static int countUpper(String s) { int c=0; for (char ch: s.toCharArray()) if (Character.isUpperCase(ch)) c++; return c; }
    private static int countLower(String s) { int c=0; for (char ch: s.toCharArray()) if (Character.isLowerCase(ch)) c++; return c; }
    private static int countDigits(String s) { int c=0; for (char ch: s.toCharArray()) if (Character.isDigit(ch)) c++; return c; }

    @Test
    @DisplayName("IT 1: Pipeline completo di generazione con merge di più politiche")
    void intTest_multiPolicyMergeAndGeneration() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();

        // Simula 3 politiche da fonti diverse (resource, realm1, realm2)
        List<DefaultPasswordRuleConf> policies = new ArrayList<>();

        // Politica 1: Resource policy - minimo base
        DefaultPasswordRuleConf resourcePolicy = new DefaultPasswordRuleConf();
        resourcePolicy.setMinLength(8);
        resourcePolicy.setMaxLength(32);
        resourcePolicy.setUppercase(1);
        resourcePolicy.setLowercase(1);
        resourcePolicy.setDigit(1);

        // Politica 2: Realm1 - richieste più forti
        DefaultPasswordRuleConf realm1Policy = new DefaultPasswordRuleConf();
        realm1Policy.setMinLength(12);
        realm1Policy.setMaxLength(64);
        realm1Policy.setUppercase(2);
        realm1Policy.setLowercase(2);
        realm1Policy.setDigit(2);

        // Politica 3: Realm2 - esigenze specifiche
        DefaultPasswordRuleConf realm2Policy = new DefaultPasswordRuleConf();
        realm2Policy.setMinLength(10);
        realm2Policy.setMaxLength(48);
        realm2Policy.setUppercase(1);
        realm2Policy.setLowercase(3);
        realm2Policy.setDigit(1);

        policies.add(resourcePolicy);
        policies.add(realm1Policy);
        policies.add(realm2Policy);

        // Merge e generazione
        DefaultPasswordRuleConf merged = gen.merge(policies);
        String pwd = gen.generate(merged);

        // Verifiche sulle politiche mergeate
        assertEquals(12, merged.getMinLength(), "minLength deve essere max(8, 12, 10) = 12");
        assertEquals(32, merged.getMaxLength(), "maxLength deve essere min(32, 64, 48) = 32");
        assertEquals(2, merged.getUppercase(), "uppercase deve essere max(1, 2, 1) = 2");
        assertEquals(3, merged.getLowercase(), "lowercase deve essere max(1, 2, 3) = 3");
        assertEquals(2, merged.getDigit(), "digit deve essere max(1, 2, 1) = 2");

        // Verifiche sulla password generata
        assertNotNull(pwd);
        assertTrue(pwd.length() >= 12, "Password >= minLength(12)");
        assertTrue(pwd.length() <= 32, "Password <= maxLength(32)");
        assertTrue(countUpper(pwd) >= 2, "Password ha >= 2 uppercase");
        assertTrue(countLower(pwd) >= 3, "Password ha >= 3 lowercase");
        assertTrue(countDigits(pwd) >= 2, "Password ha >= 2 digit");
    }

    @Test
    @DisplayName("IT 2: Generazione batch di 100 password con stesse politiche → tutte valide")
    void intTest_batchPasswordGeneration() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();

        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(10);
        conf.setMaxLength(20);
        conf.setUppercase(2);
        conf.setLowercase(2);
        conf.setDigit(2);

        List<String> passwords = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            passwords.add(gen.generate(conf));
        }

        // Tutte le password devono essere univoche (con altissima probabilità)
        assertEquals(100, passwords.stream().distinct().count(),
            "Su 100 password, almeno 99 devono essere diverse (casualità)");

        // Tutte devono rispettare i requisiti
        for (String pwd : passwords) {
            assertNotNull(pwd);
            assertTrue(pwd.length() >= 10 && pwd.length() <= 20,
                "Password length deve essere tra 10 e 20, got: " + pwd.length());
            assertTrue(countUpper(pwd) >= 2, "Password deve avere >= 2 uppercase");
            assertTrue(countLower(pwd) >= 2, "Password deve avere >= 2 lowercase");
            assertTrue(countDigits(pwd) >= 2, "Password deve avere >= 2 digit");
        }
    }

    @Test
    @DisplayName("IT 3: Validazione dei pattern di password generate (regex compliance)")
    void intTest_passwordRegexValidation() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();

        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(12);
        conf.setMaxLength(32);
        conf.setUppercase(1);
        conf.setLowercase(1);
        conf.setDigit(1);

        // Genera 20 password e valida pattern
        Pattern hasUpper = Pattern.compile("[A-Z]");
        Pattern hasLower = Pattern.compile("[a-z]");
        Pattern hasDigit = Pattern.compile("[0-9]");
        Pattern allValid = Pattern.compile("^[A-Za-z0-9]+$");  // Solo alfanumerico

        for (int i = 0; i < 20; i++) {
            String pwd = gen.generate(conf);

            assertTrue(hasUpper.matcher(pwd).find(),
                "Password deve contenere almeno una lettera maiuscola: " + pwd);
            assertTrue(hasLower.matcher(pwd).find(),
                "Password deve contenere almeno una lettera minuscola: " + pwd);
            assertTrue(hasDigit.matcher(pwd).find(),
                "Password deve contenere almeno una cifra: " + pwd);
            assertTrue(allValid.matcher(pwd).find(),
                "Password deve contenere solo caratteri alfanumerici: " + pwd);

            // Lunghezza nel range
            assertTrue(pwd.length() >= 12 && pwd.length() <= 32,
                "Password length fuori range: " + pwd.length());

            // Non deve contenere spazi o caratteri speciali inattesi
            assertFalse(pwd.contains(" "), "Password non deve contenere spazi: " + pwd);
            assertFalse(pwd.contains("\n"), "Password non deve contenere newline: " + pwd);
            assertFalse(pwd.contains("\t"), "Password non deve contenere tab: " + pwd);
        }
    }
}
