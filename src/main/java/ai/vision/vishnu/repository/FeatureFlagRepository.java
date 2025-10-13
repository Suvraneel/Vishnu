package ai.vision.vishnu.repository;

import ai.vision.vishnu.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {
    FeatureFlag findByFlagKey(String flagKey);
}
