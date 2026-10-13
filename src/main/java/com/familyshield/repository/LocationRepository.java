package com.familyshield.repository;

import com.familyshield.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findTopByDeviceIdOrderByRecordedAtDesc(Long deviceId);
    void deleteByDeviceId(Long deviceId);
}
