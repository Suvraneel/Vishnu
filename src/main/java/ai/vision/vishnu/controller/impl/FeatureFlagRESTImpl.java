package ai.vision.vishnu.controller.impl;

import ai.vision.vishnu.controller.FeatureFlagREST;
import ai.vision.vishnu.entity.FeatureFlag;
import ai.vision.vishnu.service.FeatureFlagCreateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flags")
public class FeatureFlagRESTImpl implements FeatureFlagREST {

    @Autowired
    private FeatureFlagCreateService featureFlagCreateService;

    @PostMapping
    @Override
    public ResponseEntity<FeatureFlag> createFeatureFlag(FeatureFlag featureFlag) {
        return ResponseEntity.ok(featureFlagCreateService.createFeatureFlag(featureFlag));
    }
}
