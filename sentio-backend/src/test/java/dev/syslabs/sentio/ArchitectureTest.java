package dev.syslabs.sentio;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "dev.syslabs.sentio", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

        @ArchTest
        static final ArchRule layerDependencies = layeredArchitecture()
                        .consideringOnlyDependenciesInLayers()
                        .layer("Controller").definedBy("..controller..")
                        .layer("Service").definedBy("..service..")
                        .layer("Repository").definedBy("..repository..")
                        .layer("Model").definedBy("..model..")
                        .layer("DTO").definedBy("..dto..")
                        .layer("Config").definedBy("..config..")
                        .layer("Listener").definedBy("..listener..")

                        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service", "Listener", "Config")
                        .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Listener", "Config");

        @ArchTest
        static final ArchRule controllerNaming = classes()
                        .that().areAnnotatedWith(RestController.class)
                        .or().areAnnotatedWith(Controller.class)
                        .should().haveSimpleNameEndingWith("Controller")
                        .because("Controllers should follow naming convention *Controller");

        @ArchTest
        static final ArchRule serviceNaming = classes()
                        .that().areAnnotatedWith(Service.class)
                        .should().haveSimpleNameEndingWith("Service")
                        .because("Services should follow naming convention *Service");

        @ArchTest
        static final ArchRule repositoryNaming = classes()
                        .that().areAnnotatedWith(Repository.class)
                        .or().areAssignableTo(org.springframework.data.repository.Repository.class)
                        .should().haveSimpleNameEndingWith("Repository")
                        .because("Repositories should follow naming convention *Repository");

        @ArchTest
        static final ArchRule noFieldInjection = noFields()
                        .should().beAnnotatedWith(Autowired.class)
                        .because("Use constructor injection for better testability");

        @ArchTest
        static final ArchRule noCyclicDependencies = slices()
                        .matching("dev.syslabs.sentio.(*)..")
                        .should().beFreeOfCycles()
                        .because("Cyclic dependencies make code hard to maintain");

        @ArchTest
        static final ArchRule controllersShouldNotDependOnOtherControllers = noClasses()
                        .that().resideInAPackage("..controller..")
                        .should().dependOnClassesThat().resideInAPackage("..controller..")
                        .because("Controllers should not depend on other controllers");

        @ArchTest
        static final ArchRule servicesShouldNotAccessControllers = noClasses()
                        .that().resideInAPackage("..service..")
                        .should().dependOnClassesThat().resideInAPackage("..controller..")
                        .because("Services should not depend on controllers");
}
