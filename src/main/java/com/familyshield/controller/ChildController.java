package com.familyshield.controller;

import com.familyshield.dto.ChildRequest;
import com.familyshield.model.Child;
import com.familyshield.model.User;
import com.familyshield.repository.ChildRepository;
import com.familyshield.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/children")
public class ChildController {

    private final ChildRepository children;
    private final UserRepository users;
    private final Random random = new Random();

    public ChildController(ChildRepository children, UserRepository users) {
        this.children = children;
        this.users = users;
    }

    @GetMapping
    public List<Map<String, Object>> getChildren(Principal principal) {
        User parent = users.findByEmail(principal.getName()).orElseThrow();

        return children.findByParentId(parent.getId()).stream()
            .<Map<String, Object>>map(c -> Map.of(
                        "id", c.getId(),
                        "name", c.getName(),
                        "age", c.getAge(),
                        "pairingActive", c.getPairingCode() != null &&
                                c.getPairingExpiresAt() != null &&
                                c.getPairingExpiresAt().isAfter(LocalDateTime.now())
                ))
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> createChild(
            @Valid @RequestBody ChildRequest req,
            Principal principal) {

        User parent = users.findByEmail(principal.getName()).orElseThrow();

        Child child = new Child();
        child.setName(req.name());
        child.setAge(req.age());
        child.setParent(parent);
        children.save(child);

        return ResponseEntity.ok(Map.of(
                "id", child.getId(),
                "name", child.getName(),
                "age", child.getAge()
        ));
    }

    @PostMapping("/{childId}/pairing-code")
    public ResponseEntity<?> generatePairingCode(
            @PathVariable Long childId,
            Principal principal) {

        User parent = users.findByEmail(principal.getName()).orElseThrow();

        Child child = children.findByIdAndParentId(childId, parent.getId())
                .orElseThrow();

        String code = generateCode();
        child.setPairingCode(code);
        child.setPairingExpiresAt(LocalDateTime.now().plusMinutes(10));
        children.save(child);

        return ResponseEntity.ok(Map.of(
                "childId", child.getId(),
                "code", code,
                "expiresAt", child.getPairingExpiresAt().toString(),
                "message", "Use this code on the Child Device Agent"
        ));
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.substring(0, 4) + "-" + sb.substring(4);
    }
}
