package tck.jakarta.platform.ant;

import com.sun.ts.lib.harness.VehicleVerifier;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.RuntimeConfigurable;
import tck.jakarta.platform.vehicles.VehicleType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ts.vehicles representation
 * In addition to the attributes used by the ts.vehicles macrodef, the following properties need to be passed
 * in from the ant project:
 *
 * vehicle.pkg.dir = the package dir for vehicles classes as a relative path, e.g., com/sun/ts/tests/common/vehicle
 */
public class Vehicles {
    private VehicleVerifier vehicleVerifier;

    // Name used to construct vehicle archive names
    private final String name;
    private final String vehiclePkgDir;
    private final AttributeMap attributes;
    // Custom manifest to be included in each vehicle archive
    private String manifest;
    // Directory containing the permissions descriptor
    private String earpermissionsdescriptor = "${src.dir}/${pkg.dir}";
    // ist of files to exclude from all vehicle archives relative to the classes directory
    private String excludedfiles = "";
    // Include default files in the component archive.  This attribute only affects the appclient vehicle.
    private boolean includedefaultfiles = true;
    // A comma separated list of class file regular expressions to include in each vehicle that gets built, relative to the TS_HOME/classes directory
    private String classes;
    // A single vehicle to process. if set, this overrides the default vehicles that are set in vehicle.properties
    private String vehicleoverride;
    /* Set to 1 (classes), 2 (wars, jars, etc.), or 3 (ears).
       This overrides the default build.level property for the deliverable.
       Default is what is set in bin/ts.jte
     */
    private int buildleveloverride = 3;
    //  If true, bundle all vehicle component archives into a single Java EE Ear file.  If false, a unique Ear will
    //    be used for each vehicle component archive.  The default is false
    private boolean singleear;
    // Set of filesets and/or zipfilesets to be added to the ejb component archive
    private FileSet ejbElements;
    // Set of filesets and/or zipfilesets to be added to the client component archive, the component archive in the EJB vehicle as well as the appclient vehicle
    private FileSet clientElements;
    // Set of filesets and/or zipfilesets to be added to the servlet vehicle archive
    private FileSet servletElements;
    // Set of filesets and/or zipfilesets to be added to the jsp vehicle archive
    private FileSet jspElements;
    // Set of filesets and/or zipfilesets to be added to the wsservlet vehicle archive
    private FileSet wservletElements;
    // Set of filesets and/or zipfilesets to be added to the wsejb vehicle archive
    private FileSet wsejbElements;
    // Set of filesets and/or zipfilesets to be added to all ear vehicle archive
    private FileSet earElements;
    // Set of filesets and/or zipfilesets to be added to all vehicle archives
    private FileSet jarElements;

    public Vehicles(final String name, final String vehiclePkgDir, AttributeMap attributes, RuntimeConfigurable rc, Location location) {
        this(name, vehiclePkgDir, attributes, location.getFileName());
        // Need to parse the child RuntimeConfigurables
        addFileSets(rc);
    }
    public Vehicles(final String name, final String vehiclePkgDir, AttributeMap attributes, String testDir) {
        this.name = name;
        this.vehiclePkgDir = vehiclePkgDir;
        this.attributes = attributes;
        this.vehicleVerifier = VehicleVerifier.getInstance(new File(testDir));

        if(attributes.containsKey("buildleveloverride")) {
            String level = attributes.getAttribute("buildleveloverride");
            this.buildleveloverride = Integer.parseInt(level);
        }
        this.includedefaultfiles = Boolean.parseBoolean(attributes.getAttribute("includedefaultfiles"));
        this.singleear = Boolean.parseBoolean(attributes.getAttribute("singleear"));
        this.classes = attributes.getAttribute("classes");
        this.excludedfiles = attributes.getAttribute("excludedfiles");
        this.manifest = attributes.getAttribute("manifest");
        this.earpermissionsdescriptor = attributes.getAttribute("earpermissionsdescriptor", earpermissionsdescriptor);
        this.vehicleoverride = attributes.getAttribute("vehicleoverride");

    }

    /**
     * Build the list of test vehicles to build.
     * @return Possible empty list of VehicleType to build
     */
    public List<VehicleType> determineVehicleToBuild() {
        List<VehicleType> result = new ArrayList<>();
        // The VehicleType based on the test package
        List<VehicleType> testVehicles = VehicleType.toEnumList(vehicleVerifier.getVehicleSet());
        if(vehicleoverride == null) {
            result = testVehicles;
        }
        else {
            VehicleType overrideVehicle = VehicleType.valueOf(vehicleoverride);
            // This is only used if it is in the testVehicle list
            if(testVehicles.contains(overrideVehicle)) {
                result.add(overrideVehicle);
            }
        }
        return result;
    }

    public void buildTestVehicles() {
        List<VehicleType> testVehicles = determineVehicleToBuild();
        if(testVehicles.isEmpty()) {
            return;
        }

        for (VehicleType testVehicle : testVehicles) {
            buildTestVehicle(testVehicle);
        }
    }
    public void buildTestVehicle(VehicleType type) {
        // These are the EE 10 ${vehicle.pkg.dir}/**/*Runner.class values
        Class<?>[] runnerClasses = {com.sun.ts.tests.common.vehicle.EmptyVehicleRunner.class, com.sun.ts.tests.common.vehicle.VehicleRunnerFactory.class};

        // appclient_vehicle
        String vehicleName = type.name() + "_vehicle";
        // appclient_vehicle_appclient
        String vehiclePrefix = vehicleName + '_' + type.name();
    }

    @Override
    public String toString() {
        return "Vehicles{" +
                "vehicles=" + Arrays.asList(vehicleVerifier.getVehicleSet()) +
                ", name='" + name + '\'' +
                ", vehiclePkgDir='" + vehiclePkgDir + '\'' +
                ", manifest='" + manifest + '\'' +
                ", earpermissionsdescriptor='" + earpermissionsdescriptor + '\'' +
                ", excludedfiles='" + excludedfiles + '\'' +
                ", includedefaultfiles=" + includedefaultfiles +
                ", classes='" + classes + '\'' +
                ", vehicleoverride='" + vehicleoverride + '\'' +
                ", buildleveloverride=" + buildleveloverride +
                ", singleear=" + singleear +
                ", ejbElements=" + ejbElements +
                ", clientElements=" + clientElements +
                ", servletElements=" + servletElements +
                ", jspElements=" + jspElements +
                ", wservletElements=" + wservletElements +
                ", wsejbElements=" + wsejbElements +
                ", earElements=" + earElements +
                ", jarElements=" + jarElements +
                '}';
    }

    /**
     * TODO, probably need to handle multiple fileset/zipfileset elements in a child
     * @param rc the ts.vehicles RuntimeConfigurable
     */
    private void addFileSets(RuntimeConfigurable rc) {
        for (RuntimeConfigurable rcc : Utils.asList(rc.getChildren())) {
            String childTag = rcc.getElementTag();
            switch (childTag) {
                case "ejb-elements":
                    this.ejbElements = extractFileSets(rcc);
                    break;
                case "client-elements":
                    this.clientElements = extractFileSets(rcc);
                    break;
                case "servlet-elements":
                    this.servletElements = extractFileSets(rcc);
                    break;
                case "jsp-elements":
                    this.jspElements = extractFileSets(rcc);
                    break;
                case "wsservlet-elements":
                    this.wservletElements = extractFileSets(rcc);
                    break;
                case "wsejb-elements":
                    this.wsejbElements = extractFileSets(rcc);
                    break;
                case "ear-elements":
                    this.earElements = extractFileSets(rcc);
                    break;
                case "jar-elements":
                    this.jarElements = extractFileSets(rcc);
                    break;
            }
            for (RuntimeConfigurable rccc : Utils.asList(rcc.getChildren())) {
                System.out.printf("+++ +++ nested RC: %s, attrs=%s\n", rccc.getElementTag(), rccc.getAttributeMap());
            }
        }

    }
    private FileSet extractFileSets(RuntimeConfigurable rcc) {
        FileSet theSet = null;
        for (RuntimeConfigurable fsRC : Utils.asList(rcc.getChildren())) {
            AttributeMap fsMap = new AttributeMap(attributes.getProject(), fsRC.getAttributeMap());
            theSet = new FileSet(fsMap);
        }
        return theSet;
    }
}
