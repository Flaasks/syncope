package org.apache.syncope.core.logic;

import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.provisioning.api.data.SecurityQuestionDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SecurityQuestionLogic - Control Flow coverage")
class SecurityQuestionLogicCFTest {

    @Mock private SecurityQuestionDAO securityQuestionDAO;
    @Mock private UserDAO userDAO;
    @Mock private SecurityQuestionDataBinder binder;

    private SecurityQuestionLogic logic;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        logic = new SecurityQuestionLogic(securityQuestionDAO, userDAO, binder);
    }


    @Test
    @DisplayName("list NonEmpty: DAO.findAll → binder chiamato per ciascuna entity (ordine preservato)")
    void list_nonEmpty_invocationOrder() {
        SecurityQuestion e1 = mock(SecurityQuestion.class);
        SecurityQuestion e2 = mock(SecurityQuestion.class);

        List<SecurityQuestion> backing = new ArrayList<>();
        backing.add(e1);
        backing.add(e2);
        doReturn(backing).when(securityQuestionDAO).findAll();

        SecurityQuestionTO to1 = new SecurityQuestionTO(); to1.setKey("q1");
        SecurityQuestionTO to2 = new SecurityQuestionTO(); to2.setKey("q2");
        when(binder.getSecurityQuestionTO(e1)).thenReturn(to1);
        when(binder.getSecurityQuestionTO(e2)).thenReturn(to2);

        List<SecurityQuestionTO> out = logic.list();

        assertEquals(2, out.size());
        assertEquals("q1", out.get(0).getKey());
        assertEquals("q2", out.get(1).getKey());

        InOrder inOrder = inOrder(securityQuestionDAO, binder);
        inOrder.verify(securityQuestionDAO, times(1)).findAll();
        inOrder.verify(binder, times(1)).getSecurityQuestionTO(e1);
        inOrder.verify(binder, times(1)).getSecurityQuestionTO(e2);
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    @DisplayName("list Empty: nessuna entity → nessun mapping via binder")
    void list_empty_noMapping() {
        doReturn(Collections.emptyList()).when(securityQuestionDAO).findAll();

        List<SecurityQuestionTO> out = logic.list();

        assertNotNull(out);
        assertTrue(out.isEmpty());

        verify(securityQuestionDAO, times(1)).findAll();
        verifyNoInteractions(binder);
    }


    @Test
    @DisplayName("read Exists: DAO.findById → binder.getTO (ordine verificato)")
    void read_exists_flow() {
        String key = "K";
        SecurityQuestion entity = mock(SecurityQuestion.class);
        SecurityQuestionTO to = new SecurityQuestionTO(); to.setKey(key);

        doReturn(Optional.of(entity)).when(securityQuestionDAO).findById(key);
        when(binder.getSecurityQuestionTO(entity)).thenReturn(to);

        SecurityQuestionTO out = logic.read(key);

        assertNotNull(out);
        assertEquals(key, out.getKey());

        InOrder inOrder = inOrder(securityQuestionDAO, binder);
        inOrder.verify(securityQuestionDAO).findById(key);
        inOrder.verify(binder).getSecurityQuestionTO(entity);
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    @DisplayName("read NotFound: DAO non trova → NotFoundException e binder non invocato")
    void read_notFound_flow() {
        String missing = "MISSING";
        doReturn(Optional.empty()).when(securityQuestionDAO).findById(missing);

        assertThrows(NotFoundException.class, () -> logic.read(missing));

        verify(securityQuestionDAO).findById(missing);
        verifyNoInteractions(binder);
    }
}
