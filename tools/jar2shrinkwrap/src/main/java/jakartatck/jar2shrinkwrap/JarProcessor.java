package jakartatck.jar2shrinkwrap;

import java.util.zip.ZipEntry;

/**
 * JarProcessor
 *
 * @author Scott Marlow
 */
public interface JarProcessor {

    void process(ZipEntry entry);

    void saveOutput();
}
