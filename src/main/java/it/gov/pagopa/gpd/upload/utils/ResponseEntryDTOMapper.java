package it.gov.pagopa.gpd.upload.utils;

import io.micronaut.core.annotation.Nullable;
import it.gov.pagopa.gpd.upload.entity.ResponseEntry;
import it.gov.pagopa.gpd.upload.model.v2.ResponseEntryDTO;
import jakarta.inject.Singleton;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "jsr330")
public interface ResponseEntryDTOMapper {

    @Mapping(source = "requestIDs", target = "iupds")
    ResponseEntryDTO toDTO(ResponseEntry responseEntry);

    List<ResponseEntryDTO> toDTOs(@Nullable List<ResponseEntry> responseEntries);
}