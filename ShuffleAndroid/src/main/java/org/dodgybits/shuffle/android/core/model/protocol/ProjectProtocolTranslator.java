package org.dodgybits.shuffle.android.core.model.protocol;

import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.dto.ShuffleProtos.Project.Builder;

public class ProjectProtocolTranslator implements EntityProtocolTranslator<Project, org.dodgybits.shuffle.dto.ShuffleProtos.Project> {

    private EntityDirectory<Context> mContextDirectory;

    public ProjectProtocolTranslator(EntityDirectory<Context> contextDirectory) {
        mContextDirectory = contextDirectory;
    }
    
    public org.dodgybits.shuffle.dto.ShuffleProtos.Project toMessage(Project project) {
        Builder builder = org.dodgybits.shuffle.dto.ShuffleProtos.Project.newBuilder();
        builder
            .setId(project.getLocalId().getId())
            .setGaeEntityId(project.getGaeId().getId())
            .setName((project.getName()))
            .setModified(ProtocolUtil.toDate(project.getModifiedDate()))
            .setParallel(project.isParallel())
            .setActive(project.isActive())
            .setDeleted(project.isDeleted());

        final Id defaultContextId = project.getDefaultContextId();
        if (defaultContextId.isInitialised()) {
            builder.setDefaultContextId(defaultContextId.getId());
        }

        return builder.build();
    }

    public Project fromMessage(
            org.dodgybits.shuffle.dto.ShuffleProtos.Project dto) {
        Project.Builder builder = Project.newBuilder();
        builder
            .setLocalId(Id.create(dto.getId()))
            .setName(dto.getName())
            .setModifiedDate(ProtocolUtil.fromDate(dto.getModified()))
            .setParallel(dto.getParallel());

        if (dto.hasGaeEntityId()) {
            builder.setGaeId(Id.create(dto.getGaeEntityId()));
        }

        if (dto.hasActive()) {
            builder.setActive(dto.getActive());
        } else {
            builder.setActive(true);
        }

        if (dto.hasDeleted()) {
            builder.setDeleted(dto.getDeleted());
        } else {
            builder.setDeleted(false);
        }

        if (dto.hasDefaultContextId()) {
            Id defaultContextId = Id.create(dto.getDefaultContextId());
            Context context = mContextDirectory.findById(defaultContextId);

            // it's possible the default context no longer exists so check for it
            builder.setDefaultContextId(context == null ? Id.NONE : context.getLocalId());
        }

        return builder.build();
    }      
    
}
