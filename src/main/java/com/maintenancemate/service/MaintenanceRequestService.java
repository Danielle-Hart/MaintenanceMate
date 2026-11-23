package com.maintenancemate.service;

import com.maintenancemate.model.MaintenanceRequest;
import com.maintenancemate.model.RequestStatus;
import com.maintenancemate.repository.MaintenanceRequestRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business rules for maintenance requests: who can see, change, and
 * delete a request, and which fields each role is allowed to modify.
 * Keeping these rules here (instead of in the controller) means every
 * entry point enforces them the same way and they can be unit tested
 * without a web server or database.
 */
@Service
@Transactional(readOnly = true)
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository repository;

    public MaintenanceRequestService(MaintenanceRequestRepository repository) {
        this.repository = repository;
    }

    /** Managers see every request; residents see only their own. */
    public List<MaintenanceRequest> findVisibleRequests(String username, boolean manager) {
        if (manager) {
            return repository.findAllByOrderByCreatedAtDesc();
        }
        return repository.findBySubmittedByOrderByCreatedAtDesc(username);
    }

    /**
     * Loads a request the caller is allowed to act on.
     *
     * @throws RequestNotFoundException if the id does not exist (404)
     * @throws AccessDeniedException    if a resident asks for someone else's request (403)
     */
    public MaintenanceRequest findAuthorized(Long id, String username, boolean manager) {
        MaintenanceRequest request = repository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));

        if (!manager && !username.equals(request.getSubmittedBy())) {
            throw new AccessDeniedException("You can only access your own requests");
        }
        return request;
    }

    /** Saves a new request owned by {@code username}, always starting as OPEN. */
    @Transactional
    public MaintenanceRequest create(MaintenanceRequest request, String username) {
        request.setId(null);
        request.setSubmittedBy(username);
        request.setStatus(RequestStatus.OPEN);
        return repository.save(request);
    }

    /**
     * Copies the editable fields from {@code form} onto the stored request.
     * Only managers may change the status; residents' status values are ignored.
     */
    @Transactional
    public MaintenanceRequest update(Long id, MaintenanceRequest form,
                                     String username, boolean manager) {
        MaintenanceRequest existing = findAuthorized(id, username, manager);

        existing.setTitle(form.getTitle());
        existing.setDescription(form.getDescription());
        existing.setApartmentNumber(form.getApartmentNumber());

        if (manager && form.getStatus() != null) {
            existing.setStatus(form.getStatus());
        }
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id, String username, boolean manager) {
        repository.delete(findAuthorized(id, username, manager));
    }
}
