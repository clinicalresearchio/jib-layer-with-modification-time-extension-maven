package com.infobip.jib.extension.lwmt;

import com.google.cloud.tools.jib.api.buildplan.*;
import com.google.cloud.tools.jib.maven.extension.MavenData;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.infobip.jib.extension.lwmt.LayerWithModificationTimeJibExtension.LAYER_WITH_MODIFICATION_TIME_NAME;
import static org.assertj.core.api.BDDAssertions.then;

class LayerWithModificationTimeJibExtensionTest {

    private static final Instant LAST_MODIFICATION_TIME = Instant.ofEpochSecond(9_999_999);

    @Test
    void shouldMoveToModifiedLayer() {
        // given
        Configuration givenConfiguration = givenConfiguration("**/a.*");
        MavenData givenMavenData = givenMavenData(LAST_MODIFICATION_TIME);
        LayerWithModificationTimeJibExtension givenExtension = new LayerWithModificationTimeJibExtension();
        ContainerBuildPlan givenBuildPlan = createContainerBuildPlan(
                new LinkedHashMap<String, List<String>>() {{
                    put("layer1", Arrays.asList("/layer1/a.file", "/layer1/b.file", "/layer1/c.file"));
                    put("layer2", Arrays.asList("/layer2/d.file", "/layer2/e.file"));
                    put("layer3", Arrays.asList("/layer3/f.file"));
                }}
        );

        // when
        ContainerBuildPlan actual = givenExtension.extendContainerBuildPlan(
                givenBuildPlan, null, Optional.of(givenConfiguration),
                givenMavenData, null);

        // then
        LinkedHashMap<String, List<String>> expectedLayerMap = new LinkedHashMap<String, List<String>>() {{
            put("layer1", Arrays.asList("/layer1/b.file", "/layer1/c.file"));
            put("layer2", Arrays.asList("/layer2/d.file", "/layer2/e.file"));
            put("layer3", Arrays.asList("/layer3/f.file"));
        }};
        List<String> expectedModifiedLayerFiles = Arrays.asList("/layer1/a.file");
        ContainerBuildPlan expected = createContainerBuildPlan(expectedLayerMap, expectedModifiedLayerFiles);
        then(actual).usingRecursiveComparison()
                    .isEqualTo(expected);
    }

    @Test
    void shouldMoveMultipleToModifiedLayer() {
        // given
        Configuration givenConfiguration = new Configuration(Arrays.asList("**/a.file", "**/d.file"));
        MavenData givenMavenData = givenMavenData(LAST_MODIFICATION_TIME);
        LayerWithModificationTimeJibExtension givenExtension = new LayerWithModificationTimeJibExtension();
        ContainerBuildPlan givenBuildPlan = createContainerBuildPlan(
                new LinkedHashMap<String, List<String>>() {{
                    put("layer1", Arrays.asList("/layer1/a.file", "/layer1/b.file", "/layer1/c.file"));
                    put("layer2", Arrays.asList("/layer2/d.file", "/layer2/e.file"));
                    put("layer3", Arrays.asList("/layer3/f.file"));
                }}
        );

        // when
        ContainerBuildPlan actual = givenExtension.extendContainerBuildPlan(givenBuildPlan, null, Optional.of(givenConfiguration),
                                                             givenMavenData, null);

        // then
        LinkedHashMap<String, List<String>> expectedLayerMap = new LinkedHashMap<String, List<String>>() {{
            put("layer1", Arrays.asList("/layer1/b.file", "/layer1/c.file"));
            put("layer2", Arrays.asList("/layer2/e.file"));
            put("layer3", Arrays.asList("/layer3/f.file"));
        }};
        List<String> expectedModifiedLayerFiles = Arrays.asList("/layer1/a.file", "/layer2/d.file");
        ContainerBuildPlan expected = createContainerBuildPlan(expectedLayerMap, expectedModifiedLayerFiles);
        then(actual).usingRecursiveComparison()
                    .isEqualTo(expected);
    }

    @Test
    void shouldRemoveEmptyLayer() {
        // given
        Configuration givenConfiguration = givenConfiguration("**/b.file");
        MavenData givenMavenData = givenMavenData(LAST_MODIFICATION_TIME);
        LayerWithModificationTimeJibExtension givenExtension = new LayerWithModificationTimeJibExtension();
        ContainerBuildPlan givenBuildPlan = createContainerBuildPlan(
                new LinkedHashMap<String, List<String>>() {{
                    put("layer1", Arrays.asList("/data/a.file"));
                    put("layer2", Arrays.asList("/data/b.file"));
                }}
        );

        // when
        ContainerBuildPlan actual = givenExtension.extendContainerBuildPlan(givenBuildPlan, null, Optional.of(givenConfiguration),
                                                             givenMavenData, null);

        // then
        LinkedHashMap<String, List<String>> expectedLayerMap = new LinkedHashMap<String, List<String>>() {{
            put("layer1", Arrays.asList("/data/a.file"));
        }};
        List<String> expectedModifiedLayerFiles = Arrays.asList("/data/b.file");
        ContainerBuildPlan expected = createContainerBuildPlan(expectedLayerMap, expectedModifiedLayerFiles);
        then(actual).usingRecursiveComparison()
                    .isEqualTo(expected);
    }

    @Test
    void shouldDoNoModificationsWhenNoConfiguration() {
        // given
        ContainerBuildPlan givenBuildPlan = givenDefaultBuildPlan();
        LayerWithModificationTimeJibExtension givenExtension = new LayerWithModificationTimeJibExtension();

        // when
        ContainerBuildPlan actual = givenExtension.extendContainerBuildPlan(givenBuildPlan, null, Optional.empty(), null, null);

        // then
        then(actual).isEqualTo(givenBuildPlan);
    }

    @Test
    void shouldDoNoModificationsWhenNoFilters() {
        // given
        ContainerBuildPlan givenBuildPlan = ContainerBuildPlan.builder().build();
        LayerWithModificationTimeJibExtension givenExtension = new LayerWithModificationTimeJibExtension();
        Configuration givenConfiguration = new Configuration();

        // when
        ContainerBuildPlan actual = givenExtension.extendContainerBuildPlan(givenBuildPlan, null, Optional.of(givenConfiguration),
                                                             null, null);

        // then
        then(actual).isEqualTo(givenBuildPlan);
    }

    private ContainerBuildPlan givenDefaultBuildPlan() {
        new LinkedHashMap<String, List<String>>() {{
            put("layer1", Arrays.asList("/layer1/a.file", "/layer1/b.file", "/layer1/c.file"));
            put("layer2", Arrays.asList("/layer2/d.file", "/layer2/e.file"));
            put("layer3", Arrays.asList("/layer3/f.file"));
        }};

        LinkedHashMap<String, List<String>> map = new LinkedHashMap<String, List<String>>();
        map.put("layer1", Arrays.asList("/layer1/a.file", "/layer1/b.file", "/layer1/c.file"));
        map.put("layer2", Arrays.asList("/layer2/d.file", "/layer2/e.file"));
        map.put("layer3", Arrays.asList("/layer3/f.file"));
        return createContainerBuildPlan(map, Arrays.asList());
    }

    private ContainerBuildPlan createContainerBuildPlan(LinkedHashMap<String, List<String>> layersToFilePaths) {
        return createContainerBuildPlan(layersToFilePaths, Arrays.asList());
    }

    private ContainerBuildPlan createContainerBuildPlan(LinkedHashMap<String, List<String>> layersToFilePaths,
                                                        List<String> modifiedPaths) {
        ContainerBuildPlan.Builder containerBuildPlanBuilder = ContainerBuildPlan.builder();

        for (Map.Entry<String, List<String>> layerData : layersToFilePaths.entrySet()) {
            FileEntriesLayer.Builder layerBuilder = FileEntriesLayer.builder().setName(layerData.getKey());
            layerData.getValue()
                     .stream()
                     .filter(path -> !modifiedPaths.contains(path))
                     .map(Paths::get)
                     .map(this::givenFileEntry)
                     .forEach(layerBuilder::addEntry);
            containerBuildPlanBuilder.addLayer(layerBuilder.build());
        }

        if (!modifiedPaths.isEmpty()) {
            List<FileEntry> modifiedEntries = modifiedPaths.stream()
                                               .map(path -> givenFileEntry(Paths.get(path), LAST_MODIFICATION_TIME))
                                               .collect(Collectors.toList());
            FileEntriesLayer modifiedLayer = FileEntriesLayer.builder()
                                                .setName(LAYER_WITH_MODIFICATION_TIME_NAME)
                                                .setEntries(modifiedEntries)
                                                .build();
            containerBuildPlanBuilder.addLayer(modifiedLayer);
        }

        return containerBuildPlanBuilder.build();
    }

    private FileEntry givenFileEntry(Path path) {
        return givenFileEntry(path, Instant.EPOCH);
    }

    private FileEntry givenFileEntry(Path path, Instant modificationTime) {
        return new FileEntry(path, AbsoluteUnixPath.fromPath(path), FilePermissions.DEFAULT_FILE_PERMISSIONS,
                             modificationTime, "owner");
    }

    private Configuration givenConfiguration(String filter) {
        return new Configuration(Arrays.asList(filter));
    }

    private MavenData givenMavenData(Instant instant) {
        MavenSession mavenSession = Mockito.mock(MavenSession.class);
        BDDMockito.given(mavenSession.getStartTime()).willReturn(new Date(instant.toEpochMilli()));
        MavenData mavenData = Mockito.mock(MavenData.class);
        BDDMockito.given(mavenData.getMavenSession()).willReturn(mavenSession);
        return mavenData;
    }
}
