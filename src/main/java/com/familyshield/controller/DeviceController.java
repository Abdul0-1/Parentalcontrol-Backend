package com.familyshield.controller;

import com.familyshield.dto.HeartbeatRequest;
import com.familyshield.dto.LocationRequest;
import com.familyshield.dto.PairRequest;
import com.familyshield.model.*;
import com.familyshield.repository.*;
import com.familyshield.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DeviceController {

    private final DeviceRepository devices;
    private final ChildRepository children;
    private final UserRepository users;
    private final LocationRepository locations;
    private final JwtService jwt;

    public DeviceController(
            DeviceRepository devices,
            ChildRepository children,
            UserRepository users,
            LocationRepository locations,
            JwtService jwt) {
        this.devices = devices;
        this.children = children;
        this.users = users;
        this.locations = locations;
        this.jwt = jwt;
    }

    @PostMapping("/device/pair")
    public ResponseEntity<?> pair(@Valid @RequestBody PairRequest req) {

        Child matched = children.findAll().stream()
                .filter(c -> req.pairingCode().equalsIgnoreCase(c.getPairingCode()))
                .filter(c -> c.getPairingExpiresAt() != null)
                .filter(c -> c.getPairingExpiresAt().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElse(null);

        if (matched == null) {
            return ResponseEntity.badRequest().body("Invalid or expired pairing code");
        }

        Device device = devices.findByDeviceUuid(req.deviceUuid()).orElseGet(Device::new);

        device.setChild(matched);
        device.setDeviceUuid(req.deviceUuid());
        device.setDeviceName(req.deviceName());
        device.setPlatform(req.platform());
        device.setStatus(DeviceStatus.PAIRED);
        device.setLastSeen(LocalDateTime.now());
        device.setPairedAt(LocalDateTime.now());

        Device saved = devices.save(device);

        // One-time pairing code
        matched.setPairingCode(null);
        matched.setPairingExpiresAt(null);
        children.save(matched);

        String token = jwt.createDeviceToken(saved.getDeviceUuid(), saved.getId());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "deviceId", saved.getId(),
                "childId", matched.getId(),
                "childName", matched.getName(),
                "deviceName", saved.getDeviceName()
        ));
    }

    @PostMapping("/device/heartbeat")
    public ResponseEntity<?> heartbeat(
            @RequestBody HeartbeatRequest req,
            Principal principal) {

        Device device = getDevice(principal);

        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeen(LocalDateTime.now());

        if (req.batteryLevel() != null) {
            device.setBatteryLevel(Math.max(0, Math.min(100, req.batteryLevel())));
        }

        if (req.latitude() != null && req.longitude() != null) {
            saveLocation(device, req.latitude(), req.longitude());
        }

        devices.save(device);

        return ResponseEntity.ok(Map.of(
                "status", "ONLINE",
                "lastSeen", device.getLastSeen().toString()
        ));
    }

    @PostMapping("/device/location")
    public ResponseEntity<?> location(
            @Valid @RequestBody LocationRequest req,
            Principal principal) {

        Device device = getDevice(principal);
        saveLocation(device, req.latitude(), req.longitude());
        devices.save(device);

        return ResponseEntity.ok(Map.of(
                "message", "Location received",
                "latitude", req.latitude(),
                "longitude", req.longitude()
        ));
    }

    @GetMapping("/children/{childId}/devices")
    public ResponseEntity<?> childDevices(
            @PathVariable Long childId,
            Principal principal) {

        User parent = users.findByEmail(principal.getName()).orElseThrow();

        if (children.findByIdAndParentId(childId, parent.getId()).isEmpty()) {
            return ResponseEntity.status(403).body("Child does not belong to this parent");
        }

        List<Map<String, Object>> result = devices.findByChildId(childId).stream()
                .map(d -> Map.<String, Object>of(
                        "id", d.getId(),
                        "deviceUuid", d.getDeviceUuid(),
                        "deviceName", d.getDeviceName(),
                        "platform", d.getPlatform(),
                        "status", d.getStatus().name(),
                        "batteryLevel", d.getBatteryLevel() == null ? -1 : d.getBatteryLevel(),
                        "latitude", d.getLatitude() == null ? 0 : d.getLatitude(),
                        "longitude", d.getLongitude() == null ? 0 : d.getLongitude(),
                        "lastSeen", d.getLastSeen() == null ? "" : d.getLastSeen().toString()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<?> unpair(
            @PathVariable Long deviceId,
            Principal principal) {

        User parent = users.findByEmail(principal.getName()).orElseThrow();

        Device device = devices.findByIdAndChildParentId(deviceId, parent.getId())
                .orElse(null);

        if (device == null) {
            return ResponseEntity.notFound().build();
        }

        // Delete location history first to avoid FK constraint violation
        locations.deleteByDeviceId(deviceId);
        devices.delete(device);
        return ResponseEntity.ok(Map.of("message", "Device unpaired"));
    }

    private Device getDevice(Principal principal) {
        return devices.findByDeviceUuid(principal.getName()).orElseThrow();
    }

    private void saveLocation(Device device, Double latitude, Double longitude) {
        device.setLatitude(latitude);
        device.setLongitude(longitude);
        device.setLastSeen(LocalDateTime.now());
        device.setStatus(DeviceStatus.ONLINE);

        Location location = new Location();
        location.setDevice(device);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setRecordedAt(LocalDateTime.now());
        locations.save(location);
    }
}
