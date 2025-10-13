package ai.vision.vishnu.controller;

import ai.vision.vishnu.entity.FeatureFlag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

public interface FeatureFlagREST {
    ResponseEntity<FeatureFlag> createFeatureFlag(@RequestBody FeatureFlag featureFlag);
}
