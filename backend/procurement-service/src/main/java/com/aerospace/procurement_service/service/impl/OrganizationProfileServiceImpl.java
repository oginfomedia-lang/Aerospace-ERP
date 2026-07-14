package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.dto.OrganizationProfileRequest;
import com.aerospace.procurement_service.entity.OrganizationProfile;
import com.aerospace.procurement_service.repository.OrganizationProfileRepository;
import com.aerospace.procurement_service.service.OrganizationProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationProfileServiceImpl implements OrganizationProfileService {

    // Singleton row — this ERP represents exactly one company, so there is always
    // exactly one profile, fixed at id=1 rather than auto-generated.
    private static final Long SINGLETON_ID = 1L;

    private final OrganizationProfileRepository repository;

    @Override
    public OrganizationProfile getProfile() {
        return repository.findById(SINGLETON_ID)
                .orElseGet(() -> repository.save(OrganizationProfile.builder().id(SINGLETON_ID).build()));
    }

    @Override
    public OrganizationProfile updateProfile(OrganizationProfileRequest request) {
        OrganizationProfile profile = getProfile();
        profile.setCompanyName(request.getCompanyName());
        profile.setAddressLine(request.getAddressLine());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setStateCode(request.getStateCode());
        profile.setPincode(request.getPincode());
        profile.setCountry(request.getCountry());
        profile.setPhone(request.getPhone());
        profile.setTelefax(request.getTelefax());
        profile.setEmail(request.getEmail());
        profile.setGstin(request.getGstin());
        profile.setPan(request.getPan());
        profile.setRange(request.getRange());
        profile.setDivision(request.getDivision());
        profile.setBankName(request.getBankName());
        profile.setBankBranch(request.getBankBranch());
        profile.setBankAccountNumber(request.getBankAccountNumber());
        profile.setBankIfsc(request.getBankIfsc());
        profile.setDefaultPaymentTerms(request.getDefaultPaymentTerms());
        profile.setDefaultPayment(request.getDefaultPayment());
        profile.setDefaultDeliveryTerms(request.getDefaultDeliveryTerms());
        profile.setDefaultExciseDuty(request.getDefaultExciseDuty());
        profile.setTermsAndConditions(request.getTermsAndConditions());
        return repository.save(profile);
    }
}
