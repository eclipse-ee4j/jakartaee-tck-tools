package tck.jakarta.platform.ant;

import com.sun.ts.lib.harness.VehicleVerifier;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.RuntimeConfigurable;
import tck.jakarta.platform.ant.api.DefaultEEMapping;
import tck.jakarta.platform.ant.api.EE11toEE10Mapping;
import tck.jakarta.platform.vehicles.VehicleType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * ts.vehicles representation
 * In addition to the attributes used by the ts.vehicles macrodef, the following properties need to be passed
 * in from the ant project:
 *
 * vehicle.pkg.dir = the package dir for vehicles classes as a relative path, e.g., com/sun/ts/tests/common/vehicle
 */
public class Vehicles {
    private static final Logger log = Logger.getLogger(Vehicles.class.getName());
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
    private List<TsFileSet> ejbElements = new ArrayList<>();
    // Set of filesets and/or zipfilesets to be added to the client component archive, the component archive in the EJB vehicle as well as the appclient vehicle
    private TsFileSet clientElements;
    // Set of filesets and/or zipfilesets to be added to the servlet vehicle archive
    private List<TsFileSet> servletElements = new ArrayList<>();
    // Set of filesets and/or zipfilesets to be added to the jsp vehicle archive
    private TsFileSet jspElements;
    // Set of filesets and/or zipfilesets to be added to all ear vehicle archive
    private List<TsFileSet> earElements = new ArrayList<>();
    // Set of filesets and/or zipfilesets to be added to all vehicle archives
    private TsFileSet jarElements;
    private List<Lib> earLibs = new ArrayList<>();
    private List<Lib> warLibs = new ArrayList<>();
    private EE11toEE10Mapping mapping = DefaultEEMapping.getInstance();


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

    public String getName() {
        return name;
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

    public List<TsFileSet> getEjbElements() {
        return ejbElements;
    }

    public TsFileSet getClientElements() {
        return clientElements;
    }

    public List<TsFileSet> getServletElements() {
        return servletElements;
    }

    public TsFileSet getJspElements() {
        return jspElements;
    }

    public List<TsFileSet> getEarElements() {
        return earElements;
    }

    public TsFileSet getJarElements() {
        return jarElements;
    }

    public EE11toEE10Mapping getMapping() {
        return mapping;
    }

    public void setMapping(EE11toEE10Mapping mapping) {
        this.mapping = mapping;
    }

    /**
     * Override the parsed definition of the filesets with the exact resources that went into
     * the jar based on the jar task information
     * @param taskInfo - the resources are those found in the last jar task build event
     */
    public void addJarResources(TsTaskInfo taskInfo) {
        List<TsArchiveInfo> allArchives = taskInfo.getArchives().values().stream().flatMap(Collection::stream).toList();
        for(TsArchiveInfo lib : allArchives) {
            String archiveName = lib.getFullArchiveName();
            if (archiveName.contains("ear")) {
                earElements.clear();
                earElements.addAll(lib.getResources());
            } else if (archiveName.contains("ejb")) {
                ejbElements.clear();
                ejbElements.addAll(lib.getResources());
            } else if (archiveName.contains("war")) {
                servletElements.clear();
                servletElements.addAll(lib.getResources());
            } else {
                throw new RuntimeException("Unhandled archive type" + archiveName);
            }
        }
    }

    /**
     *
     * @param packageInfo
     */
    public void addJarResources(TsPackageInfo packageInfo) {
        String name = packageInfo.getTargetName();
        List<TsArchiveInfo> allArchives = packageInfo.getArchives().values().stream().flatMap(Collection::stream).toList();
        if(name.contains("ear")) {
            for(TsArchiveInfo archiveInfo : allArchives) {
                String archiveFullName = archiveInfo.getFullArchiveName();
                String archiveName = archiveFullName;
                int lastDot = archiveFullName.lastIndexOf('.');
                if(lastDot != -1) {
                    archiveName = archiveFullName.substring(0, lastDot);
                }
                Lib lib = new Lib(this.mapping);
                lib.setArchiveName(archiveName);
                lib.addResources(archiveInfo.getResources());
                earLibs.add(lib);
            }
        } else if(name.contains("war")) {
            for(TsArchiveInfo archiveInfo : allArchives) {
                String archiveFullName = archiveInfo.getFullArchiveName();
                String archiveName = archiveFullName;
                int lastDot = archiveFullName.lastIndexOf('.');
                if (lastDot != -1) {
                    archiveName = archiveFullName.substring(0, lastDot);
                }
                Lib lib = new Lib();
                lib.setArchiveName(archiveName);
                lib.addResources(archiveInfo.getResources());
                warLibs.add(lib);
            }
        } else {
            throw new RuntimeException("Unhandled archive type" + name);
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
            TsFileSet fileSet = extractFileSets(rcc);
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
                case "ear-elements":
                    this.earElements.add(fileSet);
                    break;
                case "jar-elements":
                    this.jarElements = fileSet;
                    break;
            }
            for (RuntimeConfigurable rccc : Utils.asList(rcc.getChildren())) {
                debug("+++ +++ nested RC: %s, attrs=%s\n", rccc.getElementTag(), rccc.getAttributeMap());
            }
        }

    }
    private TsFileSet extractFileSets(RuntimeConfigurable rcc) {
        TsFileSet theSet = null;
        for (RuntimeConfigurable fsRC : Utils.asList(rcc.getChildren())) {
            AttributeMap fsMap = new AttributeMap(attributes.getProject(), fsRC.getAttributeMap());
            theSet = new TsFileSet(fsMap);
        }
        return theSet;
    }

    private void debug(String format, Object ... args) {
        String msg = String.format(format, args);
        log.fine(msg);
    }
}
