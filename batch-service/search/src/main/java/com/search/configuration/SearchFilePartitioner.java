package com.search.configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그 파일을 파티션으로 분할하는 Partitioner
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchFilePartitioner implements Partitioner {

	private final SearchLogResourceProvider searchLogResourceProvider;

	@Value("${batch.search.log-directory:logs}")
	private String logDirectory;

	@Value("${batch.search.log-pattern:item-view.log*,search-view.log*}")
	private String logPattern;

	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {

		Resource[] resources = searchLogResourceProvider.createResources(logDirectory, logPattern);
		Map<String, ExecutionContext> partitions = new HashMap<>();

		int resourceLength = resources.length;
		if (resourceLength == 0) {
			log.warn("처리할 로그 파일이 없습니다.");
			return partitions;
		}

		// gridSize만큼 파티션 생성, 각 파티션에 여러 파일 할당
		assignPartition(resourceLength, resources, partitions, gridSize);

		log.info("파티셔닝 완료 - 생성된 파티션 수: {} (gridSize: {})", partitions.size(), gridSize);
		return partitions;
	}

	private void assignPartition(
		int resourceLength,
		Resource[] resources,
		Map<String, ExecutionContext> partitions,
		int gridSize
	) {

		PartitionCountInfo partitionCountInfo = getPartitionSizePerFiles(resourceLength, gridSize);
		int actualPartitionCount = partitionCountInfo.actualPartitionCount();
		int filesPerPartition = partitionCountInfo.filesPerPartition();

		for (int partitionIndex = 0; partitionIndex < actualPartitionCount; partitionIndex++) {
			ExecutionContext context = new ExecutionContext();

			StringBuilder filePathsBuilder = extractFilePath(
				filesPerPartition,
				resourceLength,
				resources,
				partitionIndex);

			context.putString("filePaths", filePathsBuilder.toString());
			partitions.put("partition" + partitionIndex, context);
		}
	}

	private StringBuilder extractFilePath(
		int filesPerPartition,
		int resourceLength,
		Resource[] resources,
		int partitionIndex) {

		// 현재 파티션이 처리할 파일 범위 계산
		int startFileIndex = partitionIndex * filesPerPartition;
		int endFileIndex = Math.min(startFileIndex + filesPerPartition, resourceLength);

		// 파티션에 파일 경로들을 쉼표로 구분하여 저장
		StringBuilder filePathsBuilder = new StringBuilder();
		for (int fileIndex = startFileIndex; fileIndex < endFileIndex; fileIndex++) {
			try {
				String filePath = resources[fileIndex].getFile().getAbsolutePath();
				if (!filePathsBuilder.isEmpty()) {
					filePathsBuilder.append(",");
				}
				filePathsBuilder.append(filePath);

				log.info("  파티션 {} <- 파일[{}]: {}",
					partitionIndex, fileIndex, resources[fileIndex].getFilename());
			} catch (IOException e) {
				log.error("파일 경로 추출 실패: {}", resources[fileIndex].getDescription(), e);
			}
		}
		return filePathsBuilder;
	}

	private PartitionCountInfo getPartitionSizePerFiles(int resourceLength, int gridSize) {

		// gridSize 고려한 실제 파티션 수 계산
		int actualPartitionCount = Math.min(resourceLength, gridSize);

		// 각 파티션당 처리할 파일 수 계산
		int filesPerPartition = (int)Math.ceil((double)resourceLength / actualPartitionCount);

		log.info("파티셔닝 시작 - 총 파일 수: {}, gridSize: {}, 실제 파티션 수: {}, 파티션당 파일 수: {}",
			resourceLength, gridSize, actualPartitionCount, filesPerPartition);
		return new PartitionCountInfo(filesPerPartition, actualPartitionCount);
	}

	private record PartitionCountInfo(int filesPerPartition, int actualPartitionCount) {
	}
}
