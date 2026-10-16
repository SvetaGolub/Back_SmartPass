package org.Proect.domian.Guest_passes.mapper;

import org.Proect.domian.Guest_passes.dto.GuestPassResponse;
import org.Proect.domian.Guest_passes.model.GuestPasses;
import org.Proect.domian.User.model.User;
import org.springframework.stereotype.Component;

@Component
public class GuestPassMapper {

    public GuestPassResponse toResponse(GuestPasses pass) {
        if (pass == null) return null;

        GuestPassResponse dto = new GuestPassResponse();
        dto.setId(pass.getId());

        User creator = pass.getCreatesByUserId();
        if (creator != null) {
            dto.setCreatorUserId(creator.getId());
            dto.setCreatorFullName(String.format("%s %s %s",
                    creator.getLastName(), creator.getFirstName(), creator.getMiddleName()).trim());
        }

        dto.setGuestName(pass.getGuestName());
        dto.setValidFrom(pass.getValidFrom());
        dto.setValidTo(pass.getValidTo());
        dto.setIsActivated(pass.getIsActivated());
        dto.setUidLink(pass.getUidLink());
        return dto;
    }
}
