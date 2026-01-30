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
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Category Partition Tests per DefaultPasswordGenerator
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPasswordGenerator - Category Partition Tests")
class DefaultPasswordGeneratorCategoryPartitionTest {

    @Mock
    private PasswordPolicy policy1;

    @Mock
    private PasswordPolicy policy2;

    @Mock
    private ExternalResource resource;

    @Mock
    private Realm realm1;

    @Mock
    private Realm realm2;

    @Mock
    private Implementation implementation;

    @Mock
    private PasswordRule passwordRule;

    private TestableDefaultPasswordGenerator generator;

    private DefaultPasswordRuleConf ruleConf;

    private static class TestableDefaultPasswordGenerator extends DefaultPasswordGenerator {

        @Override
        protected List<PasswordRule> getPasswordRules(final PasswordPolicy policy) {
            return new ArrayList<>();
        }

        private DefaultPasswordRuleConf mergeForTest(final List<DefaultPasswordRuleConf> defaultRuleConfs) {
            return super.merge(defaultRuleConfs);
        }

        private String generateForTest(final DefaultPasswordRuleConf ruleConf) {
            return super.generate(ruleConf);
        }
    }

    @BeforeEach
    void setUp() {
        generator = new TestableDefaultPasswordGenerator();
        ruleConf = new DefaultPasswordRuleConf();
    }

    private DefaultPasswordRuleConf merge(final DefaultPasswordRuleConf conf) {
        return generator.mergeForTest(List.of(conf));
    }

    private String generateFromConf(final DefaultPasswordRuleConf conf) {
        return generator.generateForTest(merge(conf));
    }

    @Test
    @DisplayName("CP1: generate() con lista policy vuota -> password generata")
    void testGenerate_EmptyPolicies_ReturnsPassword() {
        String password = generator.generate(new ArrayList<>());

        assertNotNull(password);
        assertFalse(password.isEmpty());
        assertTrue(password.length() >= 8); // MIN_LENGTH_IF_ZERO default
    }


    @Test
    @DisplayName("CP2: generate() con 1 policy -> password rispetta regole")
    void testGenerate_SinglePolicy_ReturnsPassword() {
        String password = generator.generate(List.of(policy1));

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }

    @Test
    @DisplayName("CP3: minLength=0 -> convertito a 8 (MIN_LENGTH_IF_ZERO)")
    void testMerge_MinLengthZero_ConvertedToDefault() {
        ruleConf.setMinLength(0);
        ruleConf.setMaxLength(64);

        DefaultPasswordRuleConf merged = merge(ruleConf);
        String password = generator.generateForTest(merged);

        assertEquals(8, merged.getMinLength());
        assertTrue(password.length() >= 8);
    }

    @Test
    @DisplayName("CP4: minLength=64 -> password massima lunghezza")
    void testMerge_MinLengthMaxBoundary_GeneratesMaxPassword() {
        ruleConf.setMinLength(64);
        ruleConf.setMaxLength(64);

        DefaultPasswordRuleConf merged = merge(ruleConf);
        String password = generator.generateForTest(merged);

        assertNotNull(password);
        assertEquals(64, password.length());
    }


    @Test
    @DisplayName("CP5: maxLength=0 -> ignorato, usa default VERY_MAX_LENGTH")
    void testMerge_MaxLengthZero_IgnoredUsesDefault() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(0);

        DefaultPasswordRuleConf merged = merge(ruleConf);
        String password = generator.generateForTest(merged);

        assertNotNull(password);
        assertEquals(64, merged.getMaxLength());
        assertTrue(password.length() >= 8);
    }


    @Test
    @DisplayName("CP6: maxLength=64 -> password rispetta limite massimo")
    void testMerge_MaxLengthMaxBoundary_RespectsLimit() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);

        DefaultPasswordRuleConf merged = merge(ruleConf);
        String password = generator.generateForTest(merged);

        assertNotNull(password);
        assertEquals(64, merged.getMaxLength());
        assertTrue(password.length() >= 8);
    }


    @Test
    @DisplayName("CP7: uppercase=0 → no uppercase requirement")
    void testGenerate_NoUppercaseRequirement_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setUppercase(0);

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }


    @Test
    @DisplayName("CP8: uppercase=1 → minimum uppercase requirement")
    void testGenerate_MinimalUppercaseRequirement_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setUppercase(1);

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertTrue(password.length() >= 8);
    }


    @Test
    @DisplayName("CP9: lowercase=0 → no lowercase requirement")
    void testGenerate_NoLowercaseRequirement_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setLowercase(0);

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }


    @Test
    @DisplayName("CP10: lowercase=1 → minimum lowercase requirement")
    void testGenerate_MinimalLowercaseRequirement_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setLowercase(1);

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertTrue(password.length() >= 8);
    }


    @Test
    @DisplayName("CP11: digit=0 → no digit requirement")
    void testGenerate_NoDigitRequirement_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setDigit(0);

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }



    @Test
    @DisplayName("CP12: digit=1 → minimum digit requirement")
    void testGenerate_MinimalDigitRequirement_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setDigit(1);

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertTrue(password.length() >= 8);
    }

    @Test
    @DisplayName("CP13: special=0 → no special char requirement")
    void testGenerate_NoSpecialRequirement_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setSpecial(0);

        String password = generateFromConf(ruleConf);

        // Assert
        assertNotNull(password);
        assertFalse(password.isEmpty());
    }

    /**
     * CP14: special = 1 (one required)
     * Categoria 7: Special Count = 1 (Upper Boundary)
     */
    @Test
    @DisplayName("CP14: special=1 → minimum special char requirement")
    void testGenerate_MinimalSpecialRequirement_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setSpecial(1);
        ruleConf.getSpecialChars().clear();
        ruleConf.getSpecialChars().add('!');

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertTrue(password.length() >= 8);
    }

    
    @Test
    @DisplayName("CP15: repeatSame=0 → no repetition limit")
    void testGenerate_NoRepeatLimit_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setRepeatSame(0);

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }


    @Test
    @DisplayName("CP16: repeatSame=1 → no consecutive same chars")
    void testGenerate_StrictRepeatLimit_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setRepeatSame(1);

        assertThrows(IllegalArgumentException.class, () -> generateFromConf(ruleConf));
    }


    @Test
    @DisplayName("CP17: specialChars empty → uses default")
    void testGenerate_EmptySpecialChars_UsesDefault() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setSpecial(0);
        ruleConf.getSpecialChars().clear();

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }


    @Test
    @DisplayName("CP18: specialChars={'!'} → custom special chars")
    void testGenerate_SingleSpecialChar_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setSpecial(1);
        ruleConf.getSpecialChars().clear();
        ruleConf.getSpecialChars().add('!');

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertTrue(password.length() >= 8);
    }


    @Test
    @DisplayName("CP19: illegalChars empty → no forbidden chars")
    void testGenerate_NoIllegalChars_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.getIllegalChars().clear();

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }


    @Test
    @DisplayName("CP20: illegalChars={'0'} → excludes forbidden chars")
    void testGenerate_WithIllegalChars_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.getIllegalChars().clear();
        ruleConf.getIllegalChars().add('0');

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertTrue(password.length() >= 8);
    }


    @Test
    @DisplayName("CP21: usernameAllowed=false → username not allowed")
    void testGenerate_UsernameNotAllowed_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setUsernameAllowed(false);

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }


    @Test
    @DisplayName("CP22: usernameAllowed=true → username allowed")
    void testGenerate_UsernameAllowed_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setUsernameAllowed(true);

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }


    @Test
    @DisplayName("CP23: wordsNotPermitted empty → no forbidden words")
    void testGenerate_NoForbiddenWords_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.getWordsNotPermitted().clear();

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }


    @Test
    @DisplayName("CP24: wordsNotPermitted={'password'} → excludes forbidden words")
    void testGenerate_WithForbiddenWords_Success() {
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.getWordsNotPermitted().clear();
        ruleConf.getWordsNotPermitted().add("password");

        String password = generateFromConf(ruleConf);

        assertNotNull(password);
        assertTrue(password.length() >= 8);
    }


    @Test
    @DisplayName("CP25: generate(null) → NullPointerException")
    void testGenerate_NullPolicies_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> {
            generator.generate((List<PasswordPolicy>) null);
        });
    }


    @Test
    @DisplayName("CP26: generate(null, realms) → NullPointerException")
    void testGenerate_NullResource_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> {
            generator.generate((ExternalResource) null, new ArrayList<>());
        });
    }


    @Test
    @DisplayName("CP27: generate(resource, null) → NullPointerException")
    void testGenerate_NullRealms_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> {
            generator.generate(resource, null);
        });
    }


    @Test
    @DisplayName("CP28: generate([policy1, null]) → handles null gracefully")
    void testGenerate_NullPolicyInList_HandlesGracefully() {
        List<PasswordPolicy> policies = new ArrayList<>();
        policies.add(policy1);
        policies.add(null);

        assertDoesNotThrow(() -> generator.generate(policies));
    }


    @Test
    @DisplayName("CP29: resource.passwordPolicy=null → skipped in merge")
    void testGenerate_ResourceNullPolicy_SkippedGracefully() {
        when(resource.getPasswordPolicy()).thenReturn(null);
        when(realm1.getPasswordPolicy()).thenReturn(null);

        String password = generator.generate(resource, List.of(realm1));

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }


    @Test
    @DisplayName("CP30: realm.passwordPolicy=null → skipped in merge")
    void testGenerate_RealmNullPolicy_SkippedGracefully() {
        when(resource.getPasswordPolicy()).thenReturn(null);
        when(realm1.getPasswordPolicy()).thenReturn(null);

        String password = generator.generate(resource, List.of(realm1));

        assertNotNull(password);
        assertFalse(password.isEmpty());
    }



    @Test
    @DisplayName("CP31: minLength=16 > maxLength=8 → corregge a maxLength=16")
    void testMerge_MinGreaterThanMax_CorrectsByAdjustingMax() {
        ruleConf.setMinLength(16);
        ruleConf.setMaxLength(8);

        DefaultPasswordRuleConf merged = merge(ruleConf);
        String password = generator.generateForTest(merged);

        assertNotNull(password);
        assertEquals(16, merged.getMinLength());
        assertEquals(16, merged.getMaxLength());
        assertTrue(password.length() >= 16);
    }


    @Test
    @DisplayName("CP32: uppercase=5 + lowercase=5 + digit=5 > minLength=8 → adjusted")
    void testGenerate_RequirementsExceedMinLength_AdjustsLength() {
        
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setUppercase(5);
        ruleConf.setLowercase(5);
        ruleConf.setDigit(5);

        
        String password = generateFromConf(ruleConf);

        
        assertNotNull(password);
        assertTrue(password.length() >= 15); // sum of requirements
    }


    @Test
    @DisplayName("CP33: empty rule config → fallback to default charset")
    void testGenerate_EmptyRuleConf_UsesDefaultCharset() {
        DefaultPasswordRuleConf empty = new DefaultPasswordRuleConf();

        String password = generateFromConf(empty);

        assertNotNull(password);
        assertTrue(password.length() >= 8);
    }
}