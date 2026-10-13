package com.familyshield.repository;

import com.familyshield.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findByChildId(Long childId);
    Optional<Device> findByDeviceUuid(String deviceUuid);
    Optional<Device> findByIdAndChildParentId(Long id, Long parentId);
}
