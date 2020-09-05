// Driver class for the MT_ViewFactor_Calculator Program
// User specified STL files for the emitting, receiving, and blocking geometries
// Meant to run thermoelectric generator designs.
// No self-intersection algorithms are utilized in this version of the code

// Imports
// Aparapi is for GPU usage
import org.j3d.loaders.stl.STLFileReader;
import java.io.File;
import com.aparapi.device.*;
import com.aparapi.internal.kernel.*;
import java.util.LinkedHashSet;


public class TEDServer{

    public static void main(String args[]) {

        // Constants are user-defined for use in the output
        Constants globalConstants = new Constants();
        globalConstants.N=1; // Number of junctions
        globalConstants.theta=0.1; // TEG packing density
        globalConstants.H_W=1.50; // TEG height-to-width ratio
        globalConstants.t=0.125; // TEG interconnect thickness


        // Create the arrays of tessellations per emitter and receiver
        // Used when wanting to run successive meshes
        int emitterTessellations[] = new int[1];
        int receiverTessellations[] = new int[1];

        // User defined number of tessellations per surface.
        // I utilized this convention purely for keeping track of which STL files I was using
        emitterTessellations[0] = 2134;

        receiverTessellations[0] = 2141;

        // Tessellations in the blocking geometry. Note how few tessellations the blocking surface has.
        int blockT = 44;

        KernelPreferences preferences = KernelManager.instance().getDefaultPreferences();

        // Instantiate the GPUs. This code assumes the computer has, at a maximum, two GPUs
        Device device1 = null;
        Device device2 = null;
        for (int w=0; w<preferences.getPreferredDevices(null).size(); w++) {
            if (w == 0) { //1st GPU
                System.out.println("Device 1: ");
                device1 = preferences.getPreferredDevices(null).get(w);
                System.out.println(device1);
                System.out.println();
            } else if (w == 1) {//2nd GPU
                System.out.println("Device 2: ");
                device2 =preferences.getPreferredDevices(null).get(w);
                System.out.println(device2);
                System.out.println();
            }
        }

        // Set the GPU to run on.
        LinkedHashSet<Device> DeviceSet = new LinkedHashSet<Device>();
        DeviceSet.add(device1); //Add GPU that you wish to run
        KernelManager.instance().setDefaultPreferredDevices(DeviceSet);



        // Loop through the number of successive meshes that were specified above. This bit is unnecessary if running one configuration
        for (int i = 0; i < receiverTessellations.length; i++) {

            // Link file location to the respective STL file
            StringBuilder emitterFileName = new StringBuilder("C:\\Desktop\\MT_ViewFactor_Calculator\\STLFiles\\Emitter\\");
            StringBuilder receiverFileName = new StringBuilder("C:\\Desktop\\MT_ViewFactor_Calculator\\STLFiles\\Receiver\\");
            StringBuilder blockFileName = new StringBuilder("C:\\Desktop\\MT_ViewFactor_Calculator\\STLFiles\\Blocking\\");

            emitterFileName.append("top_");
            receiverFileName.append("bottom_");
            blockFileName.append("block_");

            emitterFileName.append(emitterTessellations[i] + "T.stl");
            receiverFileName.append(receiverTessellations[i] + "T.stl");
            blockFileName.append(blockT + "T.stl");

            //Create File objects for the STL File Reader
            File emitterFile = new File(emitterFileName.toString());
            File receiverFile = new File(receiverFileName.toString());
            File blockFile = new File(blockFileName.toString());
            //File blockFile = null;
            try {
                //Create STL Files
                STLFileReader emitterReader = new STLFileReader(emitterFile);
                STLFileReader receiverReader = new STLFileReader(receiverFile);
                STLFileReader blockReader = new STLFileReader(blockFile);
                CalculateVF.run(globalConstants, emitterReader, receiverReader, blockReader);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
