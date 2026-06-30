package com.example.banking.web;

import com.example.banking.security.CurrentUser;
import com.example.banking.service.ContactService;
import com.example.banking.web.dto.AddContactRequest;
import com.example.banking.web.dto.ContactResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService service;
    private final CurrentUser currentUser;

    public ContactController(ContactService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<ContactResponse> list() {
        return service.list(currentUser.require().getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse add(@RequestBody AddContactRequest req) {
        return service.add(currentUser.require(), req.username());
    }

    @DeleteMapping("/{contactUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID contactUserId) {
        service.remove(currentUser.require(), contactUserId);
    }
}
