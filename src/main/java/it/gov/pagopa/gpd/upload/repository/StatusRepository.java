package it.gov.pagopa.gpd.upload.repository;

import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlQuerySpec;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.model.enumeration.ServiceType;
import it.gov.pagopa.gpd.upload.repository.impl.StatusRepositoryImpl;

import java.time.LocalDateTime;
import java.util.List;

public interface StatusRepository {
    Status saveStatus(Status status);

    Status upsert(Status status);

    Status findStatusById(String id, String fiscalCode);

    List<Status> find(String query);

    List<Status> find(SqlQuerySpec query, CosmosQueryRequestOptions queryRequestOptions);

    StatusRepositoryImpl.FileIdsPage findFileIdsPage(
            String brokerCode,
            String organizationFiscalCode,
            LocalDateTime from,
            LocalDateTime to,
            int size,
            String continuationToken,
            ServiceType serviceType
    );
}
