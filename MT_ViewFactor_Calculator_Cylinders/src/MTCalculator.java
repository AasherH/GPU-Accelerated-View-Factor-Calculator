// Calculates the view factor via GPU-accelerated programming
// Ray casting and intersection detection occurs in this class
// Final view factor is returned to CalculateVF

import com.aparapi.Kernel;
import com.aparapi.Range;
import java.lang.Math;
import java.util.function.Consumer;

public class MTCalculator extends Kernel {

    // Instance variables
    private double[] result; // Contains the view factors for each specific job
    private double viewFactorResults[];
    private double individualViewFactors[];
    private double viewFactor;

    private Geometry emitter;
    private SelfIntersectionUpdate receiver;
    private SelfIntersectionUpdate block;

    private int emitterTessellations;
    private double[] emitterNormalX;
    private double[] emitterNormalY;
    private double[] emitterNormalZ;
    private double[] emitterVertexAX;
    private double[] emitterVertexAY;
    private double[] emitterVertexAZ;
    private double[] emitterCenterX;
    private double[] emitterCenterY;
    private double[] emitterCenterZ;
    private double[] emitterAreas;

    private int blockTessellations;
    private double[] blockNormalX;
    private double[] blockNormalY;
    private double[] blockNormalZ;
    private double[] blockVertexAX;
    private double[] blockVertexAY;
    private double[] blockVertexAZ;
    private double[] blockEdgeBAX;
    private double[] blockEdgeBAY;
    private double[] blockEdgeBAZ;
    private double[] blockEdgeCAX;
    private double[] blockEdgeCAY;
    private double[] blockEdgeCAZ;

    private int receiverTessellations;
    private double[] receiverNormalX;
    private double[] receiverNormalY;
    private double[] receiverNormalZ;
    private double[] receiverVertexAX;
    private double[] receiverVertexAY;
    private double[] receiverVertexAZ;
    private double[] receiverCenterX;
    private double[] receiverCenterY;
    private double[] receiverCenterZ;
    private double[] receiverAreas;

    private int totalNumberOfUpdated;

    private int emitterIndex;
    private static final double PI = 3.141592653589793238462643383279502884197169399375105820974944592307816406286d;

    //UPDATED EMITTER/RECEIVER
    private double[] normalXupdate;
    private double[] normalYupdate;
    private double[] normalZupdate;

    private double[] vertexAXupdate;
    private double[] vertexAYupdate;
    private double[] vertexAZupdate;

    private double[] edgeBAXupdate;
    private double[] edgeBAYupdate;
    private double[] edgeBAZupdate;

    private double[] edgeCAXupdate;
    private double[] edgeCAYupdate;
    private double[] edgeCAZupdate;

    private double[] centerXupdate;
    private double[] centerYupdate;
    private double[] centerZupdate;

    private double[] areaupdate;

    //////////////////////////////////////////////////////////////////////////
    //UPDATED BLOCK
    private double[] normalXblock;
    private double[] normalYblock;
    private double[] normalZblock;

    private double[] vertexAXblock;
    private double[] vertexAYblock;
    private double[] vertexAZblock;

    private double[] edgeBAXblock;
    private double[] edgeBAYblock;
    private double[] edgeBAZblock;

    private double[] edgeCAXblock;
    private double[] edgeCAYblock;
    private double[] edgeCAZblock;

    private double[] centerXblock;
    private double[] centerYblock;
    private double[] centerZblock;

    private double[] areablock;


    ////////////////////////////////////////////////////////////////////
    // Original EMITTER/RECEIVER values
    private double[] normalX;
    private double[] normalY;
    private double[] normalZ;

    private double[] vertexAX;
    private double[] vertexAY;
    private double[] vertexAZ;

    private double[] edgeBAX;
    private double[] edgeBAY;
    private double[] edgeBAZ;

    private double[] edgeCAX;
    private double[] edgeCAY;
    private double[] edgeCAZ;

    private double[] centerX;
    private double[] centerY;
    private double[] centerZ;

    private double[] area;
    private int size;


    public interface KernelComplete {
        double onComplete();
    }


    public MTCalculator(Geometry emitter, SelfIntersectionUpdate receiver, SelfIntersectionUpdate block, Geometry OGUpdater) {
        // Copy over constant final data
        this.emitter = emitter;
        this.receiver = receiver;
        this.block = block;

        totalNumberOfUpdated = OGUpdater.getSize();

        emitterTessellations = emitter.getSize();
        emitterNormalX = emitter.getNormalX();
        emitterNormalY = emitter.getNormalY();
        emitterNormalZ = emitter.getNormalZ();
        emitterVertexAX = emitter.getVertexAX();
        emitterVertexAY = emitter.getVertexAY();
        emitterVertexAZ = emitter.getVertexAZ();
        emitterCenterX = emitter.getCenterX();
        emitterCenterY = emitter.getCenterY();
        emitterCenterZ = emitter.getCenterZ();
        emitterAreas = emitter.getArea();

        blockTessellations = block.getSizeblock();
        blockNormalX = block.getNormalXblock();
        blockNormalY = block.getNormalYblock();
        blockNormalZ = block.getNormalZblock();
        blockVertexAX = block.getVertexAXblock();
        blockVertexAY = block.getVertexAYblock();
        blockVertexAZ = block.getVertexAZblock();
        blockEdgeBAX = block.getEdgeBAXblock();
        blockEdgeBAY = block.getEdgeBAYblock();
        blockEdgeBAZ = block.getEdgeBAZblock();
        blockEdgeCAX = block.getEdgeCAXblock();
        blockEdgeCAY = block.getEdgeCAYblock();
        blockEdgeCAZ = block.getEdgeCAZblock();

        receiverTessellations = receiver.getSize();
        receiverNormalX = receiver.getNormalX();
        receiverNormalY = receiver.getNormalY();
        receiverNormalZ = receiver.getNormalZ();
        receiverVertexAX = receiver.getVertexAX();
        receiverVertexAY = receiver.getVertexAY();
        receiverVertexAZ = receiver.getVertexAZ();
        receiverCenterX = receiver.getCenterX();
        receiverCenterY = receiver.getCenterY();
        receiverCenterZ = receiver.getCenterZ();
        receiverAreas = receiver.getArea();

        result = new double[receiverTessellations];

    }

    //Run on the GPU
    @Override
    public void run() { //gets called in CalculateVF when we instantiate the execute method for the Kernel

        int receiverIndex = getGlobalId();

        // Calculate the ray from the emitter to the destination tessellation.
        double rayX = receiverCenterX[receiverIndex] - emitterCenterX[emitterIndex];
        double rayY = receiverCenterY[receiverIndex] - emitterCenterY[emitterIndex];
        double rayZ = receiverCenterZ[receiverIndex] - emitterCenterZ[emitterIndex];
        double rayMagnitude = vectorMagnitude(rayX, rayY, rayZ);

        // Check if any intersecting geometry exists.
        for (int blockIndex = 0; blockIndex < blockTessellations; blockIndex++) {
            double intersectionDistance = intersectionDistance(blockIndex, rayX, rayY, rayZ);
            // If intersecting geometry exists, the contributed view factor is zero.
            if (intersectionDistance != 0 && intersectionDistance <= rayMagnitude) {
                result[receiverIndex] = 0;
                return;
            }
        }

        double emitterDenominator =
                vectorMagnitude(
                        emitterNormalX[emitterIndex],
                        emitterNormalY[emitterIndex],
                        emitterNormalZ[emitterIndex]) * rayMagnitude;
        double receiverDenominator =
                vectorMagnitude(
                        receiverNormalX[receiverIndex],
                        receiverNormalY[receiverIndex],
                        receiverNormalZ[receiverIndex]) * rayMagnitude;

        double emitterNormalDotRay =
                emitterNormalX[emitterIndex] * rayX
                        + emitterNormalY[emitterIndex] * rayY
                        + emitterNormalZ[emitterIndex] * rayZ;
        double receiverNormalDotRay =
                receiverNormalX[receiverIndex] * rayX
                        + receiverNormalY[receiverIndex] * rayY
                        + receiverNormalZ[receiverIndex] * rayZ;

        double cosThetaOne = emitterNormalDotRay / emitterDenominator;
        double cosThetaTwo = receiverNormalDotRay / receiverDenominator;

        if (cosThetaOne < 0) {
            cosThetaOne = -cosThetaOne;
        }
        if (cosThetaTwo < 0){
            cosThetaTwo = -cosThetaTwo;
        }


        result[receiverIndex] = cosThetaOne * cosThetaTwo * emitterAreas[emitterIndex] * receiverAreas[receiverIndex]
                / (PI * rayMagnitude * rayMagnitude);

        // Uncomment if interested in the view factor for a ray cast
        //System.out.println(result[receiverIndex]);


    }

    public double calculate(Consumer<double[]> resultConsumer, KernelComplete completionHandler) {

        viewFactorResults = new double[emitterTessellations]; //should store the final values
        individualViewFactors = new double[totalNumberOfUpdated];

        // Acquire all the emitter data
        emitterNormalX = emitter.getNormalX();
        emitterNormalY = emitter.getNormalY();
        emitterNormalZ = emitter.getNormalZ();
        emitterCenterX = emitter.getCenterX();
        emitterCenterY = emitter.getCenterY();
        emitterCenterZ = emitter.getCenterZ();
        emitterAreas = emitter.getArea();

        // Partition for as many times as many tessellations are located within the receiver
        for (int index = 0; index < totalNumberOfUpdated; index++) {

            //Update your values
            receiver.update(index);
            block.updateBlock(index);

            blockVertexAX = block.getVertexAXblock();
            blockVertexAY = block.getVertexAYblock();
            blockVertexAZ = block.getVertexAZblock();
            blockEdgeBAX = block.getEdgeBAXblock();
            blockEdgeBAY = block.getEdgeBAYblock();
            blockEdgeBAZ = block.getEdgeBAZblock();
            blockEdgeCAX = block.getEdgeCAXblock();
            blockEdgeCAY = block.getEdgeCAYblock();
            blockEdgeCAZ = block.getEdgeCAZblock();
            receiverTessellations = receiver.getSize();


            receiverNormalX = receiver.getNormalX();
            receiverNormalY = receiver.getNormalY();
            receiverNormalZ = receiver.getNormalZ();
            receiverCenterX = receiver.getCenterX();
            receiverCenterY = receiver.getCenterY();
            receiverCenterZ = receiver.getCenterZ();
            receiverAreas = receiver.getArea();

            // Explicitly pass only these values onto the GPU
            setExplicit(true);
            put(emitterNormalX).put(emitterNormalY).put(emitterNormalZ);
            put(emitterCenterX).put(emitterCenterY).put(emitterCenterZ);
            put(emitterAreas);

            put(blockVertexAX).put(blockVertexAY).put(blockVertexAZ);
            put(blockEdgeBAX).put(blockEdgeBAY).put(blockEdgeBAZ);
            put(blockEdgeCAX).put(blockEdgeCAY).put(blockEdgeCAZ);

            put(receiverNormalX).put(receiverNormalY).put(receiverNormalZ);
            put(receiverCenterX).put(receiverCenterY).put(receiverCenterZ);
            put(receiverAreas);

            for (emitterIndex = 0; emitterIndex < emitterTessellations; emitterIndex++) {
                super.execute(Range.create(receiverTessellations));
                get(result);
                resultConsumer.accept(result); //automatically should call add via method referencing
                viewFactorResults[emitterIndex] = sum(result);

            }

            individualViewFactors[index] = sum(viewFactorResults);
            // Uncomment if interested in the view factor for one iteration
            //System.out.println(individualViewFactors[index]);
        }
        
        System.out.println("View factor: " + (sum(individualViewFactors))/(sum(emitter.getArea())));
        viewFactor = completionHandler.onComplete()/sum(emitterAreas);

        return viewFactor;
    }

    private double intersectionDistance(int interconnectIndex, double rayX, double rayY, double rayZ) {
        // MT Algorithm for intersection detection

        double pvecX = rayY * blockEdgeCAZ[interconnectIndex] - rayZ * blockEdgeCAY[interconnectIndex];
        double pvecY = rayZ * blockEdgeCAX[interconnectIndex] - rayX * blockEdgeCAZ[interconnectIndex]; //negative is accounted for by switching order of ops
        double pvecZ = rayX * blockEdgeCAY[interconnectIndex] - rayY * blockEdgeCAX[interconnectIndex];

        double intersectionDistance = -1;

        double det = blockEdgeBAX[interconnectIndex] * pvecX
                + blockEdgeBAY[interconnectIndex] * pvecY
                + blockEdgeBAZ[interconnectIndex] * pvecZ;

        //Back-face culling enabled
        if (det < 0){
            return 0;
        }

        // Ray is parallel to plane. Thus, no intersection since dot product of orthogonal vectors is 0
        if (det < 1e-8 && det > -1e-8){
            return 0;
        }

        double invDet = 1 / det;

        double tvecX = emitterCenterX[emitterIndex] - blockVertexAX[interconnectIndex];
        double tvecY = emitterCenterY[emitterIndex] - blockVertexAY[interconnectIndex];
        double tvecZ = emitterCenterZ[emitterIndex] - blockVertexAZ[interconnectIndex];

        double u = (tvecX * pvecX + tvecY * pvecY + tvecZ * pvecZ) * invDet; //normalize u

        if (u < 0 || u > 1){
            return 0;
        }

        double qvecX = tvecY * blockEdgeBAZ[interconnectIndex] - tvecZ * blockEdgeBAY[interconnectIndex];
        double qvecY = tvecZ * blockEdgeBAX[interconnectIndex] - tvecX * blockEdgeBAZ[interconnectIndex];
        double qvecZ = tvecX * blockEdgeBAY[interconnectIndex] - tvecY * blockEdgeBAX[interconnectIndex];

        double v = (rayX * qvecX + rayY * qvecY + rayZ * qvecZ) * invDet; //normalize v

        if (v < 0 || (u + v) > 1 ) {
            return 0;
        }
        else {
            return (blockEdgeCAX[interconnectIndex] * qvecX
                    + blockEdgeCAY[interconnectIndex] * qvecY
                    + blockEdgeCAZ[interconnectIndex] * qvecZ) * invDet;

        }
    }


    public double vectorMagnitude(double x, double y, double z) {
        return Math.sqrt(x*x + y*y + z*z);
    }


    private static double sum(double...values) {
        double result = 0;
        for (int index=0;index<values.length; index++){
            //System.out.println(result);
            result = result + values[index];
        }
        return result;
    }

    //EMITTER FUNCTIONS

    private double [] getNormalX(){
        return normalXupdate;
    }

    private double[] getNormalY() {
        return normalYupdate;
    }

    private double[] getNormalZ() {
        return normalZupdate;
    }

    private double[] getVertexAX() {
        return vertexAXupdate;
    }

    private double[] getVertexAY() {
        return vertexAYupdate;
    }

    private double[] getVertexAZ() {
        return vertexAZupdate;
    }

    private double[] getEdgeBAX() {
        return edgeBAXupdate;
    }

    private double[] getEdgeBAY() {
        return edgeBAYupdate;
    }

    private double[] getEdgeBAZ() {
        return edgeBAZupdate;
    }

    private double[] getEdgeCAX() {
        return edgeCAXupdate;
    }

    private double[] getEdgeCAY() {
        return edgeCAYupdate;
    }

    private double[] getEdgeCAZ() {
        return edgeCAZupdate;
    }

    private double[] getCenterX() {
        return centerXupdate;
    }

    private double[] getCenterY() {
        return centerYupdate;
    }

    private double[] getCenterZ() {
        return centerZupdate;
    }

    private int getSize(){
        return normalXupdate.length;
    }

    private double[] getArea() {
        return areaupdate;
    }

    //BLOCKING FUNCTIONS

    private double [] getNormalXblock(){
        return normalXblock;
    }

    private double[] getNormalYblock() {
        return normalYblock;
    }

    private double[] getNormalZblock() {
        return normalZblock;
    }

    private double[] getVertexAXblock() {
        return vertexAXblock;
    }

    private double[] getVertexAYblock() {
        return vertexAYblock;
    }

    private double[] getVertexAZblock() {
        return vertexAZblock;
    }

    private double[] getEdgeBAXblock() {
        return edgeBAXblock;
    }

    private double[] getEdgeBAYblock() {
        return edgeBAYblock;
    }

    private double[] getEdgeBAZblock() {
        return edgeBAZblock;
    }

    private double[] getEdgeCAXblock() {
        return edgeCAXblock;
    }

    private double[] getEdgeCAYblock() {
        return edgeCAYblock;
    }

    private double[] getEdgeCAZblock() {
        return edgeCAZblock;
    }

    private double[] getCenterXblock() {
        return centerXblock;
    }

    private double[] getCenterYblock() {
        return centerYblock;
    }

    private double[] getCenterZblock() {
        return centerZblock;
    }

    private int getSizeblock(){
        return normalXblock.length;
    }

    private double[] getAreablock() {
        return areablock;
    }

    private void update(int index) {

        //Assign the correct updater value to the updated one
        normalXupdate[0] = normalX[index];
        normalYupdate[0] = normalY[index];
        normalZupdate[0] = normalZ[index];

        vertexAXupdate[0] = vertexAX[index];
        vertexAYupdate[0] = vertexAY[index];
        vertexAZupdate[0] = vertexAZ[index];

        edgeBAXupdate[0] = edgeBAX[index];
        edgeBAYupdate[0] = edgeBAY[index];
        edgeBAZupdate[0] = edgeBAZ[index];

        edgeCAXupdate[0] = edgeCAX[index];
        edgeCAYupdate[0] = edgeCAY[index];
        edgeCAZupdate[0] = edgeCAZ[index];

        centerXupdate[0] = centerX[index];
        centerYupdate[0] = centerY[index];
        centerZupdate[0] = centerZ[index];

        areaupdate[0] = area[index];
    }

    private void updateBlock(int index){

        for (int i=0; i<size-1; i++){

            //if i is less than index, then you can use a 1:1 corrspondence between original STL file and the updated version
            if (i < index){
                normalXblock[i] = normalX[i];
                normalYblock[i] = normalY[i];
                normalZblock[i] = normalZ[i];

                vertexAXblock[i] = vertexAX[i];
                vertexAYblock[i] = vertexAY[i];
                vertexAZblock[i] = vertexAZ[i];

                edgeBAXblock[i] = edgeBAX[i];
                edgeBAYblock[i] = edgeBAY[i];
                edgeBAZblock[i] = edgeBAZ[i];

                edgeCAXblock[i] = edgeCAX[i];
                edgeCAYblock[i] = edgeCAY[i];
                edgeCAZblock[i] = edgeCAZ[i];

                centerXblock[i] = centerX[i];
                centerYblock[i] = centerY[i];
                centerZblock[i] = centerZ[i];

                areablock[i] = area[i];
            }

            else if (i >= index){
                normalXblock[i] = normalX[i+1];
                normalYblock[i] = normalY[i+1];
                normalZblock[i] = normalZ[i+1];

                vertexAXblock[i] = vertexAX[i+1];
                vertexAYblock[i] = vertexAY[i+1];
                vertexAZblock[i] = vertexAZ[i+1];

                edgeBAXblock[i] = edgeBAX[i+1];
                edgeBAYblock[i] = edgeBAY[i+1];
                edgeBAZblock[i] = edgeBAZ[i+1];

                edgeCAXblock[i] = edgeCAX[i+1];
                edgeCAYblock[i] = edgeCAY[i+1];
                edgeCAZblock[i] = edgeCAZ[i+1];

                centerXblock[i] = centerX[i+1];
                centerYblock[i] = centerY[i+1];
                centerZblock[i] = centerZ[i+1];

                areablock[i] = area[i+1];
            }
        }
    }
}