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
    private List<TSFileSet> ejbElements = new ArrayList<>();
    // Set of filesets and/or zipfilesets to be added to the client component archive, the component archive in the EJB vehicle as well as the appclient vehicle
    private TSFileSet clientElements;
    // Set of filesets and/or zipfilesets to be added to the servlet vehicle archive
    private List<TSFileSet> servletElements = new ArrayList<>();
    // Set of filesets and/or zipfilesets to be added to the jsp vehicle archive
    private TSFileSet jspElements;
    // Set of filesets and/or zipfilesets to be added to the wsservlet vehicle archive
    private TSFileSet wservletElements;
    // Set of filesets and/or zipfilesets to be added to the wsejb vehicle archive
    private TSFileSet wsejbElements;
    // Set of filesets and/or zipfilesets to be added to all ear vehicle archive
    private List<TSFileSet> earElements = new ArrayList<>();
    // Set of filesets and/or zipfilesets to be added to all vehicle archives
    private TSFileSet jarElements;

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

    public String getVehicleoverride() {
        return vehicleoverride;
    }

    public int getBuildleveloverride() {
        return buildleveloverride;
    }

    public boolean isSingleear() {
        return singleear;
    }

    public List<TSFileSet> getEjbElements() {
        return ejbElements;
    }

    public TSFileSet getClientElements() {
        return clientElements;
    }

    public List<TSFileSet> getServletElements() {
        return servletElements;
    }

    public TSFileSet getJspElements() {
        return jspElements;
    }

    public TSFileSet getWservletElements() {
        return wservletElements;
    }

    public TSFileSet getWsejbElements() {
        return wsejbElements;
    }

    public List<TSFileSet> getEarElements() {
        return earElements;
    }

    public TSFileSet getJarElements() {
        return jarElements;
    }

    /**
     * Override the parsed definition of the filesets with the exact resources that went into
     * the jar based on the jar task information
     * @param taskInfo - the resources are those found in the last jar task build event
     */
    public void addJarResources(TsTaskInfo taskInfo) {
        String archiveName = taskInfo.getArchiveName();
        if(archiveName.contains("ear")) {
            earElements.clear();
            earElements.addAll(taskInfo.getResources());
        } else if(archiveName.contains("ejb")) {
            ejbElements.clear();
            ejbElements.addAll(taskInfo.getResources());
        } else if(archiveName.contains("war")) {
            servletElements.clear();
            servletElements.addAll(taskInfo.getResources());
        } else {
            throw new RuntimeException("Unhandled archive type" + archiveName);
        }
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
            TSFileSet fileSet = extractFileSets(rcc);
            String childTag = rcc.getElementTag();
            switch (childTag) {
                case "ejb-elements":
                    this.ejbElements.add(fileSet);
                    break;
                case "client-elements":
                    this.clientElements = fileSet;
                    break;
                case "servlet-elements":
                    this.servletElements.add(fileSet);
                    break;
                case "jsp-elements":
                    this.jspElements = fileSet;
                    break;
                case "wsservlet-elements":
                    this.wservletElements = fileSet;
                    break;
                case "wsejb-elements":
                    this.wsejbElements = fileSet;
                    break;
                case "ear-elements":
                    this.earElements.add(fileSet);
                    break;
                case "jar-elements":
                    this.jarElements = fileSet;
                    break;
            }
            for (RuntimeConfigurable rccc : Utils.asList(rcc.getChildren())) {
                System.out.printf("+++ +++ nested RC: %s, attrs=%s\n", rccc.getElementTag(), rccc.getAttributeMap());
            }
        }

    }
    private TSFileSet extractFileSets(RuntimeConfigurable rcc) {
        TSFileSet theSet = null;
        for (RuntimeConfigurable fsRC : Utils.asList(rcc.getChildren())) {
            AttributeMap fsMap = new AttributeMap(attributes.getProject(), fsRC.getAttributeMap());
            theSet = new TSFileSet(fsMap);
        }
        return theSet;
    }

}
