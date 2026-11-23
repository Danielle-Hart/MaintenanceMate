package com.maintenancemate.service;

import com.maintenancemate.model.MaintenanceRequest;
import com.maintenancemate.model.RequestStatus;
import com.maintenancemate.repository.MaintenanceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceRequestServiceTest {

    @Mock
    private MaintenanceRequestRepository repository;

    @InjectMocks
    private MaintenanceRequestService service;

    private MaintenanceRequest residentsRequest;

    @BeforeEach
    void setUp() {
        residentsRequest = new MaintenanceRequest();
        residentsRequest.setId(1L);
        residentsRequest.setTitle("Broken AC");
        residentsRequest.setDescription("Not cooling");
        residentsRequest.setApartmentNumber("204");
        residentsRequest.setSubmittedBy("resident");
        residentsRequest.setStatus(RequestStatus.OPEN);
    }

    @Test
    void residentSeesOnlyOwnRequests() {
        when(repository.findBySubmittedByOrderByCreatedAtDesc("resident"))
                .thenReturn(List.of(residentsRequest));

        assertEquals(List.of(residentsRequest),
                service.findVisibleRequests("resident", false));
        verify(repository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void managerSeesAllRequests() {
        when(repository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(residentsRequest));

        assertEquals(1, service.findVisibleRequests("manager", true).size());
    }

    @Test
    void missingRequestThrowsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class,
                () -> service.findAuthorized(99L, "manager", true));
    }

    @Test
    void residentCannotAccessSomeoneElsesRequest() {
        when(repository.findById(1L)).thenReturn(Optional.of(residentsRequest));

        assertThrows(AccessDeniedException.class,
                () -> service.findAuthorized(1L, "otherResident", false));
    }

    @Test
    void residentCannotDeleteSomeoneElsesRequest() {
        when(repository.findById(1L)).thenReturn(Optional.of(residentsRequest));

        assertThrows(AccessDeniedException.class,
                () -> service.delete(1L, "otherResident", false));
        verify(repository, never()).delete(any());
    }

    @Test
    void createForcesOwnerAndOpenStatus() {
        MaintenanceRequest submitted = new MaintenanceRequest();
        submitted.setId(42L);                          // client-supplied id must be ignored
        submitted.setSubmittedBy("someoneElse");       // spoofed owner must be ignored
        submitted.setStatus(RequestStatus.COMPLETED);  // spoofed status must be ignored
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRequest saved = service.create(submitted, "resident");

        assertNull(saved.getId());
        assertEquals("resident", saved.getSubmittedBy());
        assertEquals(RequestStatus.OPEN, saved.getStatus());
    }

    @Test
    void managerCanChangeStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(residentsRequest));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRequest saved =
                service.update(1L, formWithStatus(RequestStatus.IN_PROGRESS), "manager", true);

        assertEquals(RequestStatus.IN_PROGRESS, saved.getStatus());
    }

    @Test
    void residentCanEditDetailsButNotStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(residentsRequest));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRequest saved =
                service.update(1L, formWithStatus(RequestStatus.COMPLETED), "resident", false);

        assertEquals("Leaking sink", saved.getTitle());
        assertEquals(RequestStatus.OPEN, saved.getStatus());
    }

    private MaintenanceRequest formWithStatus(RequestStatus status) {
        MaintenanceRequest form = new MaintenanceRequest();
        form.setTitle("Leaking sink");
        form.setDescription("Drips all night");
        form.setApartmentNumber("204");
        form.setStatus(status);
        return form;
    }
}
