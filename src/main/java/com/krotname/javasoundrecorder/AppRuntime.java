package com.krotname.javasoundrecorder;

import com.krotname.javasoundrecorder.audio.AudioInputProbe;
import com.krotname.javasoundrecorder.audio.JavaSoundInputProbe;
import com.krotname.javasoundrecorder.config.AppConfig;
import com.krotname.javasoundrecorder.config.UserPreferences;
import com.krotname.javasoundrecorder.config.UserPreferencesStore;
import com.krotname.javasoundrecorder.export.ExportFormat;
import com.krotname.javasoundrecorder.export.ExportResult;
import com.krotname.javasoundrecorder.export.RecordingExportService;
import com.krotname.javasoundrecorder.library.RecordingEntry;
import com.krotname.javasoundrecorder.library.RecordingLibraryService;
import com.krotname.javasoundrecorder.metadata.RecordingMetadata;
import com.krotname.javasoundrecorder.metadata.RecordingMetadataStore;
import com.krotname.javasoundrecorder.model.FileUploadResult;
import com.krotname.javasoundrecorder.orchestration.RecordingCoordinator;
import com.krotname.javasoundrecorder.storage.UploadService;
import com.krotname.javasoundrecorder.ui.RecorderSettings;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

class AppRuntime {
    private final Map<String, String> env;
    private final UserPreferencesStore preferencesStore;
    private final RuntimeFactory runtimeFactory;
    private final RecordingLibraryService libraryService;
    private final RecordingExportService exportService;
    private final RecordingMetadataStore metadataStore;
    private final AtomicReference<AppConfig> config;
    private final AtomicReference<RecordingCoordinator> coordinator;

    AppRuntime(Map<String, String> env, UserPreferencesStore preferencesStore, AppConfig config,
               RecordingCoordinator coordinator) {
        this(env, preferencesStore, new RuntimeFactory(), new RecordingLibraryService(), new RecordingExportService(),
                new RecordingMetadataStore(), config, coordinator);
    }

    AppRuntime(Map<String, String> env, UserPreferencesStore preferencesStore, RuntimeFactory runtimeFactory,
               AppConfig config, RecordingCoordinator coordinator) {
        this(env, preferencesStore, runtimeFactory, new RecordingLibraryService(), new RecordingExportService(),
                new RecordingMetadataStore(), config, coordinator);
    }

    AppRuntime(Map<String, String> env, UserPreferencesStore preferencesStore, RuntimeFactory runtimeFactory,
               RecordingLibraryService libraryService, RecordingExportService exportService, AppConfig config,
               RecordingCoordinator coordinator) {
        this(env, preferencesStore, runtimeFactory, libraryService, exportService, new RecordingMetadataStore(),
                config, coordinator);
    }

    AppRuntime(Map<String, String> env, UserPreferencesStore preferencesStore, RuntimeFactory runtimeFactory,
               RecordingLibraryService libraryService, RecordingExportService exportService,
               RecordingMetadataStore metadataStore, AppConfig config, RecordingCoordinator coordinator) {
        this.env = env;
        this.preferencesStore = preferencesStore;
        this.runtimeFactory = runtimeFactory;
        this.libraryService = libraryService;
        this.exportService = exportService;
        this.metadataStore = metadataStore;
        this.config = new AtomicReference<>(config);
        this.coordinator = new AtomicReference<>(coordinator);
    }

    RecordingCoordinator currentCoordinator() {
        return coordinator.get();
    }

    RecorderSettings currentSettings() {
        AppConfig current = config.get();
        return new RecorderSettings(
                current.recordingDuration().toMillis(),
                current.recordingDirectory(),
                current.isUploadEnabled(),
                current.audioInputName()
        );
    }

    AudioInputProbe inputProbe() {
        return runtimeFactory.inputProbe(config.get().audioInputName());
    }

    List<String> inputNames() {
        return runtimeFactory.inputNames();
    }

    List<RecordingEntry> recordings() throws IOException {
        return libraryService.list(config.get().recordingDirectory());
    }

    RecordingEntry rename(RecordingEntry entry, String newName) throws IOException {
        return libraryService.rename(entry, newName);
    }

    void delete(RecordingEntry entry) throws IOException {
        libraryService.delete(entry);
    }

    ExportResult exportWav(RecordingEntry entry, java.nio.file.Path target) throws IOException {
        return exportService.exportWav(entry, target);
    }

    ExportResult exportRecording(RecordingEntry entry, java.nio.file.Path target, ExportFormat format)
            throws IOException {
        return exportService.export(entry, target, format);
    }

    RecordingMetadata metadata(RecordingEntry entry) throws IOException {
        return metadataStore.read(entry.path());
    }

    void saveMetadata(RecordingEntry entry, RecordingMetadata metadata) throws IOException {
        metadataStore.save(entry.path(), metadata);
    }

    FileUploadResult upload(RecordingEntry entry) throws IOException {
        AppConfig current = config.get();
        if (!current.isUploadEnabled()) {
            throw new IOException("Upload is disabled. Enable upload and configure a Dropbox token before retrying.");
        }
        return runtimeFactory.uploadService(current).upload(entry.path());
    }

    void applySettings(RecorderSettings settings) {
        RecordingCoordinator active = coordinator.get();
        if (active.isRunning()) {
            throw new IllegalStateException("Cannot change settings while recording is active.");
        }
        UserPreferences nextPreferences = new UserPreferences(
                Optional.of(Duration.ofMillis(settings.recordingDurationMillis())),
                Optional.of(settings.recordingDirectory()),
                Optional.of(settings.uploadEnabled()),
                Optional.ofNullable(settings.audioInputName())
        );
        preferencesStore.save(nextPreferences);
        AppConfig nextConfig = AppConfig.from(env, nextPreferences);
        RecordingCoordinator nextCoordinator = runtimeFactory.coordinator(nextConfig);
        RecordingCoordinator previous = coordinator.getAndSet(nextCoordinator);
        config.set(nextConfig);
        previous.close();
    }

    void close() {
        coordinator.get().close();
    }

    static class RuntimeFactory {
        RecordingCoordinator coordinator(AppConfig config) {
            return Main.createCoordinator(config);
        }

        UploadService uploadService(AppConfig config) {
            return Main.resolveUploader(config);
        }

        AudioInputProbe inputProbe(String inputName) {
            return new JavaSoundInputProbe(inputName);
        }

        List<String> inputNames() {
            return new JavaSoundInputProbe().inputNames();
        }
    }
}
