package com.aerospace.procurement_service.service;

import com.aerospace.procurement_service.dto.OrganizationProfileRequest;
import com.aerospace.procurement_service.entity.OrganizationProfile;

public interface OrganizationProfileService {
    OrganizationProfile getProfile();
    OrganizationProfile updateProfile(OrganizationProfileRequest request);
}
