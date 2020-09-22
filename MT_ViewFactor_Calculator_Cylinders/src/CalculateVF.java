// CalculateVF sets up the GPU-acceleration portion of the code
// Generates the STL file objects and controls the output

import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import org.j3d.loaders.stl.STLFileReader;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.concurrent.atomic.DoubleAdder;

public class CalculateVF {

    // Define an output file for the view factor results
    public static String outputFile = "output.txt";

    public static void run(STLFileReader emitterReader, STLFileReader receiverReader) {

        // Total view factor
        double viewFactor = 0;

        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            PrintWriter testBatchWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
            // Write output to file:
            WriteFileAndConsole(testBatchWriter, "-------------------- STARTING NEW TEST BATCH --------------------");
            Calendar cal = Calendar.getInstance();
            WriteFileAndConsole(testBatchWriter, "Time: " + dateFormat.format(cal.getTime()));
            WriteFileAndConsole(testBatchWriter, "-----------------------------------------------------------------");
            testBatchWriter.close();

            // Set times to track how long the calculations took
            Timer globalTimer = new Timer();
            Timer gpuTimer = null;
            globalTimer.start();
            double GPUTime = 0;

            //Create the Geometry files based upon STL
            Geometry emitter = new Geometry(emitterReader);
            Geometry receiver = new Geometry(receiverReader);
            System.out.println("STL Parsed Time: " + globalTimer.stop());

            int emitterTessellationLength = emitter.getSize();
            int receiverTessellationLength = receiver.getSize();

            gpuTimer = new Timer();
            gpuTimer.start();
            ThreadedAdder adder = new ThreadedAdder(new DoubleAdder());

            // Initialize your objects for geometry partitioning
            SelfIntersectionUpdate receiverUpdate = new SelfIntersectionUpdate(receiver);
            SelfIntersectionUpdate blockUpdate = new SelfIntersectionUpdate(receiver);

            double individualViewFactors = 0;

            // Set the first index of geometry partitioning
            // Note that with concentric cylinders, the outer cylinder represents the emitting surface and is not partitioned
            int index = 0;
            receiverUpdate.update(index);
            blockUpdate.updateBlock(index);


            MTCalculator partialMT = new MTCalculator(emitter, receiverUpdate, blockUpdate, receiver);
            individualViewFactors = partialMT.calculate(adder::add, adder::finishAndGet);
            GPUTime += gpuTimer.stop();

            //Final viewFactor that gets printed out to the screen
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
            // Write output to file:
            WriteFileAndConsole(writer, "-------------------- Results --------------------");
            WriteFileAndConsole(writer, " Cylinders: ");
            WriteFileAndConsole(writer, "\nEmitter Tessellations: " + emitterTessellationLength + " Receiver Tessellations: " + receiverTessellationLength);
            WriteFileAndConsole(writer, "\nCalculated view factor: " + individualViewFactors + "\n");
            WriteFileAndConsole(writer, "GPU time: " + GPUTime);
            WriteFileAndConsole(writer, "Total time: " + globalTimer.stop() + "\n");
            writer.close();

            PrintWriter complete = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
            WriteFileAndConsole(complete, "Task completed.");
            complete.close();

        }
        catch (Exception e) {
        System.out.println("Error writing to file.");
        }
    }

    public static void WriteFileAndConsole(PrintWriter writer, String string) {
        writer.println(string);
        System.out.println(string);
    }

}