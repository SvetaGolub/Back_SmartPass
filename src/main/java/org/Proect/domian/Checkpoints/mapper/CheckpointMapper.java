package  org.Proect.domian.Checkpoints.mapper;

import org.Proect.domian.Checkpoints.dto.CheckpointResponse;
import org.Proect.domian.Checkpoints.model.Checkpoints;
import org.springframework.stereotype.Component;

@Component
public class CheckpointMapper {

    public CheckpointResponse toResponse(Checkpoints checkpoint) {
        if (checkpoint == null) return null;

        CheckpointResponse response = new CheckpointResponse();
        response.setId(checkpoint.getId());
        response.setName(checkpoint.getName());
        response.setStatus(checkpoint.getStatus());

        if (checkpoint.getZone() != null) {
            response.setZoneId(checkpoint.getZone().getId());
            response.setZoneName(checkpoint.getZone().getName());
        }

        return response;
    }
}
