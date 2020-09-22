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

        // Create the arrays of tessellations per emitter and receiver
        // Used when wanting to run successive meshes
        int emitterTessellations[] = new int[2];
        int receiverTessellations[] = new int[2];

        // User defined number of tessellations per surface.
        // I utilized this convention purely for keeping track of which STL files I was using
        emitterTessellations[0] = 544;
        emitterTessellations[1] = 2268;

        receiverTessellations[0] = 336;
        receiverTessellations[1] = 2604;

        for (int i = 0; i < receiverTessellations.length; i++) {

            // Link file location to the respective STL file
            StringBuilder emitterFileName = new StringBuilder("C:\\Users\\asher\\Desktop\\MT_ViewFactor_Calculator_Cylinders\\STLFiles\\OuterCylinder\\");
            StringBuilder receiverFileName = new StringBuilder("C:\\Users\\asher\\Desktop\\MT_ViewFactor_Calculator_Cylinders\\STLFiles\\InnerCylinder\\");

            emitterFileName.append("outer_");
            receiverFileName.append("inner_");

            emitterFileName.append(emitterTessellations[i] + "T.stl");
            receiverFileName.append(receiverTessellations[i] + "T.stl");

            //Create File objects for the STL File Reader
            File emitterFile = new File(emitterFileName.toString());
            File receiverFile = new File(receiverFileName.toString());

            try {

                // Instantiate the GPUs. This code assumes the computer has, at a maximum, two GPUs
                KernelPreferences preferences = KernelManager.instance().getDefaultPreferences();
                Device device1 = null;
                Device device2 = null;
                for (int w=0; w<preferences.getPreferredDevices(null).size(); w++){
                    if (w==0){ //1st GPU
                        System.out.println("Device 1: ");
                        device1 = preferences.getPreferredDevices(null).get(w);
                        System.out.println(device1);
                        System.out.println();
                    }
                    else if (w==1){//2nd GPU
                        System.out.println("Device 2: ");
                        device2 =preferences.getPreferredDevices(null).get(w);
                        System.out.println(device2);
                        System.out.println();
                    }
                }

                LinkedHashSet<Device> DeviceSet = new LinkedHashSet<Device>();
                DeviceSet.add(device1); //Add GPU that you wish to run
                KernelManager.instance().setDefaultPreferredDevices(DeviceSet); //Set the GPU to run


                //Create STL Files
                STLFileReader emitterReader = new STLFileReader(emitterFile);
                STLFileReader receiverReader = new STLFileReader(receiverFile);
                CalculateVF.run(emitterReader, receiverReader);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
