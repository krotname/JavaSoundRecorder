package com.krotname.javasoundrecorder;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureUnitTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("com.krotname.javasoundrecorder");

    @Test
    void packageSlicesHaveNoCycles() {
        slices().matching("com.krotname.javasoundrecorder.(*)..")
                .should()
                .beFreeOfCycles()
                .check(classes);
    }

    @Test
    void uiShouldNotDriveNonUiLayers() {
        noClasses().that().resideInAnyPackage("..ui..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..audio..", "..storage..", "..config..")
                .check(classes);
    }
}
