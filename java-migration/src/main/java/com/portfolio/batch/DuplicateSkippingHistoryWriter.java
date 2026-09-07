package com.portfolio.batch;

import com.portfolio.domain.PositionHistory;
import com.portfolio.domain.PositionHistoryId;
import com.portfolio.repository.PositionHistoryRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;

public class DuplicateSkippingHistoryWriter implements ItemWriter<PositionHistory> {
  private final PositionHistoryRepository historyRepository;
  private final RepositoryItemWriter<PositionHistory> delegate;
  private int duplicateCount;

  public DuplicateSkippingHistoryWriter(PositionHistoryRepository historyRepository) {
    this.historyRepository = historyRepository;
    delegate = new RepositoryItemWriter<>();
    delegate.setRepository(historyRepository);
    delegate.setMethodName("save");
    try {
      delegate.afterPropertiesSet();
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to configure POSHIST writer", exception);
    }
  }

  @Override
  public void write(Chunk<? extends PositionHistory> chunk) throws Exception {
    List<PositionHistory> newRecords = new ArrayList<>();
    int duplicates = 0;
    for (PositionHistory history : chunk) {
      PositionHistoryId id = history.getId();
      if (historyRepository.existsById(id)) {
        duplicates++;
      } else {
        newRecords.add(history);
      }
    }
    if (!newRecords.isEmpty()) {
      delegate.write(new Chunk<>(newRecords));
    }
    duplicateCount += duplicates;
  }

  public int getDuplicateCount() {
    return duplicateCount;
  }

  public void resetDuplicateCount() {
    duplicateCount = 0;
  }
}
