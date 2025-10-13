package ai.vision.vishnu.service.impl;

import ai.vision.vishnu.entity.FeatureFlag;
import ai.vision.vishnu.repository.FeatureFlagRepository;
import ai.vision.vishnu.service.FeatureFlagCreateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeatureFlagCreateServiceImpl implements FeatureFlagCreateService {

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    @Override
    public FeatureFlag createFeatureFlag(FeatureFlag featureFlag) {
        FeatureFlag existingFeatureFlag = featureFlagRepository.findByFlagKey(featureFlag.getFlagKey());
        if (existingFeatureFlag != null)
            throw new IllegalArgumentException("Feature flag with key " + featureFlag.getFlagKey() + " already exists.");
        return featureFlagRepository.save(featureFlag);
    }
}
