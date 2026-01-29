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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LLM-Generated Tests for MailTemplateLogic
 * 
 * Questi test sono stati generati tramite AI e coprono i casi base
 * delle operazioni CRUD su MailTemplate.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MailTemplateLogic - LLM Generated Tests")
class MailTemplateLogicLLMTest {

    @Mock
    private MailTemplateDAO mailTemplateDAO;

    @Mock
    private NotificationDAO notificationDAO;

    @Mock
    private EntityFactory entityFactory;

    @Mock
    private MailTemplate mailTemplate;

    private MailTemplateLogic mailTemplateLogic;

    @BeforeEach
    void setUp() {
        mailTemplateLogic = new MailTemplateLogic(mailTemplateDAO, notificationDAO, entityFactory);
    }

    /**
     * TEST 1: Creazione di un nuovo template (Happy Path).
     * Partizione: Stato DB [Non Esistente] -> [Esistente].
     */
    @Test
    @DisplayName("LLM1: create() con nuova chiave → successo")
    void create_WithNewKey_ShouldSucceed() {
        // Arrange
        String newKey = "welcome_template";
        when(mailTemplateDAO.existsById(newKey)).thenReturn(false);
        when(entityFactory.newEntity(MailTemplate.class)).thenReturn(mailTemplate);
        when(mailTemplateDAO.save(mailTemplate)).thenReturn(mailTemplate);
        
        // Act
        MailTemplateTO result = mailTemplateLogic.create(newKey);

        // Assert
        assertNotNull(result);
        assertEquals(newKey, result.getKey());
        verify(mailTemplateDAO).existsById(newKey);
        verify(mailTemplate).setKey(newKey);
        verify(mailTemplateDAO).save(mailTemplate);
    }

    /**
     * TEST 2: Creazione di un template duplicato (Error Path).
     * Partizione: Stato DB [Esistente].
     * Questo testa il bordo in cui la chiave collide con una esistente.
     */
    @Test
    @DisplayName("LLM2: create() con chiave esistente → DuplicateException")
    void create_WithExistingKey_ShouldThrowDuplicateException() {
        // Arrange
        String existingKey = "existing_template";
        when(mailTemplateDAO.existsById(existingKey)).thenReturn(true);

        // Act & Assert
        DuplicateException exception = assertThrows(DuplicateException.class, () -> {
            mailTemplateLogic.create(existingKey);
        }, "Dovrebbe lanciare DuplicateException se la chiave esiste già");
        
        assertEquals(existingKey, exception.getMessage());
        verify(mailTemplateDAO).existsById(existingKey);
        verify(mailTemplateDAO, never()).save(any());
    }

    /**
     * TEST 3: Lettura di un template inesistente (Error Path).
     * Partizione: Stato DB [Non Esistente].
     * Verifica la gestione del caso 'Not Found'.
     */
    @Test
    @DisplayName("LLM3: read() con chiave mancante → NotFoundException")
    void read_WithMissingKey_ShouldThrowNotFoundException() {
        // Arrange
        String missingKey = "unknown_template";
        doReturn(Optional.empty()).when(mailTemplateDAO).findById(missingKey);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            mailTemplateLogic.read(missingKey);
        }, "Dovrebbe lanciare NotFoundException se la chiave non viene trovata");
        
        assertTrue(exception.getMessage().contains(missingKey));
        verify(mailTemplateDAO).findById(missingKey);
    }

    /**
     * TEST 4: Cancellazione di un template inesistente (Error Path).
     * Partizione: Stato DB [Non Esistente].
     * Verifica che non si possano cancellare entità che non esistono (consistenza).
     */
    @Test
    @DisplayName("LLM4: delete() con chiave mancante → NotFoundException")
    void delete_WithMissingKey_ShouldThrowNotFoundException() {
        // Arrange
        String missingKey = "already_deleted_template";
        doReturn(Optional.empty()).when(mailTemplateDAO).findById(missingKey);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            mailTemplateLogic.delete(missingKey);
        }, "Dovrebbe lanciare NotFoundException tentando di cancellare una chiave inesistente");
        
        assertTrue(exception.getMessage().contains(missingKey));
        verify(mailTemplateDAO).findById(missingKey);
        verify(mailTemplateDAO, never()).deleteById(anyString());
    }
}