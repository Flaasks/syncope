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
 * Unless required by applicable law or agreed to in writing
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Control Flow Tests per MailTemplateLogic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MailTemplateLogic - Control Flow Tests")
class MailTemplateLogicCFTest {

    @Mock
    private MailTemplateDAO mailTemplateDAO;

    @Mock
    private NotificationDAO notificationDAO;

    @Mock
    private EntityFactory entityFactory;

    @Mock
    private MailTemplate mailTemplate;

    @Mock
    private Notification notification1;

    @Mock
    private Notification notification2;

    private MailTemplateLogic mailTemplateLogic;

    @BeforeEach
    void setUp() {
        mailTemplateLogic = new MailTemplateLogic(mailTemplateDAO, notificationDAO, entityFactory);
    }

    @Test
    @DisplayName("CF1: delete() con 1 notifica associata -> SyncopeClientException (boundary minimo)")
    void testDelete_WithOneNotification_ThrowsInUseException() {
        String templateKey = "template_in_use";
        String notificationKey = "notification1";
        
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById(templateKey);
        when(notification1.getKey()).thenReturn(notificationKey);
        when(notificationDAO.findByTemplate(mailTemplate)).thenReturn(List.of(notification1));

        SyncopeClientException exception = assertThrows(SyncopeClientException.class,
                () -> mailTemplateLogic.delete(templateKey));

        assertEquals(ClientExceptionType.InUse, exception.getType());
        assertEquals(1, exception.getElements().size());
        assertTrue(exception.getElements().contains(notificationKey));
        
        verify(mailTemplateDAO).findById(templateKey);
        verify(notificationDAO).findByTemplate(mailTemplate);
        verify(mailTemplateDAO, never()).deleteById(anyString());
    }


    @Test
    @DisplayName("CF2: delete() con N notifiche associate -> SyncopeClientException (boundary multiplo)")
    void testDelete_WithMultipleNotifications_ThrowsInUseException() {
        String templateKey = "popular_template";
        String notificationKey1 = "notification1";
        String notificationKey2 = "notification2";
        String notificationKey3 = "notification3";
        
        Notification notification3 = mock(Notification.class);
        
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById(templateKey);
        when(notification1.getKey()).thenReturn(notificationKey1);
        when(notification2.getKey()).thenReturn(notificationKey2);
        when(notification3.getKey()).thenReturn(notificationKey3);
        when(notificationDAO.findByTemplate(mailTemplate))
                .thenReturn(Arrays.asList(notification1, notification2, notification3));

        SyncopeClientException exception = assertThrows(SyncopeClientException.class,
                () -> mailTemplateLogic.delete(templateKey));

        assertEquals(ClientExceptionType.InUse, exception.getType());
        assertEquals(3, exception.getElements().size());
        assertTrue(exception.getElements().contains(notificationKey1));
        assertTrue(exception.getElements().contains(notificationKey2));
        assertTrue(exception.getElements().contains(notificationKey3));
        
        verify(mailTemplateDAO).findById(templateKey);
        verify(notificationDAO).findByTemplate(mailTemplate);
        verify(mailTemplateDAO, never()).deleteById(anyString());
    }
}
